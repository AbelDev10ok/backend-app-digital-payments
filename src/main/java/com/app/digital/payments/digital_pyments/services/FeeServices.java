package com.app.digital.payments.digital_pyments.services;

import java.time.LocalDate;
// import java.time.temporal.ChronoUnit;
// import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.FeeStatus;
import com.app.digital.payments.digital_pyments.utils.Payments;

import jakarta.persistence.EntityNotFoundException;

import static com.app.digital.payments.digital_pyments.mappers.FeeMapper.convertToFeeDto;

@Service
public class FeeServices implements IFeeServices {

    @Autowired
    private IFeeRepository feeRepository;

    @Autowired
    private ISalesServices saleService;

    @Autowired
    private ISaleRepository saleRepository;



    // metodos de registro de pago de cuota
    @Override
    @Transactional
    public FeeDto registerPayment(Long feeId, Double amount) {
        Fee fee = getFeeById(feeId);
        
        if (fee.getPaid()) {
            throw new IllegalStateException("La cuota ya está pagada");
        }

        Sale sale = fee.getSale();
        // if(amount > sale.getRemainingAmount()) {
        //     throw new IllegalArgumentException("El monto del pago no puede ser mayor al saldo restante de la venta");
        // }

        if (amount <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }

        sale.deductFromRemainingAmount(amount);

        fee.setPaid(true);
        fee.setAmount(amount);
        fee.setDatePayment(LocalDate.now());

        // marcar venta como completada si el monto restante es cero o negativo
        if (sale.getRemainingAmount() <= 0) {
            sale.setCompleted(true);
            sale.setRemainingAmount(0.0); // Asegurarse de que el monto restante sea cero
        }

        // Generar la proxima cuota si quedan cuotas por pendientes
        if (sale.getRemainingAmount() > 0) {
            Fee nextFee = new Fee();
            nextFee.setSale(sale);
            nextFee.setNumberFee(fee.getNumberFee() + 1);
            if(sale.getTypePayments() == Payments.MENSUAL) {
                nextFee.setExpirationDate(fee.getExpirationDate().plusMonths(1));
            }
            else if(sale.getTypePayments() == Payments.QUINCENAL) {
                nextFee.setExpirationDate(fee.getExpirationDate().plusDays(15));
            }
            else if(sale.getTypePayments() == Payments.SEMANAL) {
                nextFee.setExpirationDate(fee.getExpirationDate().plusWeeks(1));
            } else {
                throw new IllegalArgumentException("Tipo de pago no soportado: " + sale.getTypePayments()); 
            }
            nextFee.setAdditional(false);
            sale.getFees().add(nextFee);
            feeRepository.save(nextFee);
        }
        
        saleRepository.save(sale);
        Fee savedFee = feeRepository.save(fee);
        updateRelatedSaleStatus(fee.getSale().getId());
        
        return  convertToFeeDto(savedFee);
    }

    // metodo para cambiar fecha de pago de una cuota
    @Override
    @Transactional
    public FeeDto postponeFee(Long feeId, LocalDate newDate) {
        Fee fee = getFeeById(feeId);
        
        if (fee.getPaid()) {
            throw new IllegalStateException("No se puede posponer una cuota pagada");
        }

        if (newDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La nueva fecha de vencimiento no puede ser anterior a la fecha actual");
        }
        
        fee.setExpirationDate(newDate);
        updateRelatedSaleStatus(fee.getSale().getId());
        
        return convertToFeeDto(feeRepository.save(fee));
    }
    // método para actualizar el estado de la venta relacionada
    private void updateRelatedSaleStatus(Long saleId) {
        saleService.updateSaleGlobalDelay(saleId);
    }


    @Override
    @Transactional
    public Fee getFeeById(Long feeId) {
        return feeRepository.findById(feeId)
            .orElseThrow(() -> new EntityNotFoundException("Cuota no encontrada con ID: " + feeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeeDto> getFeeBySaleId(Long feeId) {
        List<Fee> fees = feeRepository.findBySaleId(feeId);        
        return fees.stream()
        .map(fee-> convertToFeeDto(fee))
        .toList();
    }
    

    // método para obtener cuotas pendientes por cliente
    @Override
    @Transactional(readOnly = true)
    public List<FeeDto> getOutstandingFeeByClientId(Long clientId) {
        return feeRepository.findBySaleClientIdAndPaid(clientId, false).stream()
                .map(fee-> convertToFeeDto(fee))
                .collect(Collectors.toList());
    }

    // método para obtener cuotas prioritarias
    // (con mayor cantidad de días de atraso)
    // @Override
    // @Transactional(readOnly = true)
    // public List<FeeDto> getPriorityFees() {
    //     LocalDate today = LocalDate.now();
        
    //     List<Fee> fees = feeRepository.findPriorityFees(today);
    //     return fees.stream()
    //         // .sorted(Comparator.comparing(Fee::getDaysLate).reversed())
    //         .map(fee-> convertToFeeDto(fee))
    //         .toList();
    // }

    @Override
    @Transactional(readOnly = true)
    public List<FeeDto> getFeesByStatus(FeeStatus status, LocalDate date) {
        return switch (status) {
            case TODAY -> getTodaysFees(date);
            case PENDING -> getPendingFees();
            // case POSTPONED -> getPostponedFees();
            case DELAYED -> getDelayedFees(date);
            default -> throw new IllegalArgumentException("Unexpected value: " + status);
        };
    }
    
    @Transactional(readOnly = true)
    private List<FeeDto> getTodaysFees(LocalDate date) {
        return feeRepository.findByExpirationDateAndPaid(date, false).stream()
            .map(fee -> {
                FeeDto dto = convertToFeeDto(fee);
                return dto;
            })
            .toList();
    }
    
    @Transactional(readOnly = true)
    private List<FeeDto> getPendingFees() {
        return feeRepository.findByPaidFalse().stream()
            .map(fee-> convertToFeeDto(fee))
            .toList();
    }
    
    // @Transactional(readOnly = true)
    // private List<FeeDto> getPostponedFees() {
    //     return feeRepository.findByPostponedTrueAndPaidFalse().stream()
    //         .map(fee-> convertToFeeDto(fee))
    //         .toList();
    // }

    // método para obtener cuotas atrasadas
    // (no pagadas y con fecha de vencimiento anterior a la fecha actual)
    @Transactional(readOnly = true)
    private List<FeeDto> getDelayedFees(LocalDate date) {
        return feeRepository.findByPaidFalseAndExpirationDateBefore(date).stream()
            .map(fee -> {
                FeeDto dto = convertToFeeDto(fee);
                return dto;
            })
            .toList();
    }

}
