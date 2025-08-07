package com.app.digital.payments.digital_pyments.services;

import java.time.LocalDate;
// import java.time.temporal.ChronoUnit;
// import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.Payments;

import jakarta.persistence.EntityNotFoundException;

import static com.app.digital.payments.digital_pyments.mappers.FeeMapper.convertToFeeDto;

@Service
public class FeeServices implements IFeeServices {

    @Autowired
    private IFeeRepository feeRepository;

    @Autowired
    private IVentaServices saleService;

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

        
        Sale payableItem = fee.getSale();


        // if(amount > sale.getRemainingAmount()) {
        //     throw new IllegalArgumentException("El monto del pago no puede ser mayor al saldo restante de la venta");
        // }

        if (amount <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }

        payableItem.deductFromRemainingAmount(amount);

        fee.setPaid(true);
        fee.setAmount(amount);
        fee.setDatePayment(LocalDate.now());

        // marcar venta como completada si el monto restante es cero o negativo
        if (payableItem.getRemainingAmount() <= 0) {
            payableItem.setCompleted(true);
            payableItem.setRemainingAmount(0.0); // Asegurarse de que el monto restante sea cero
        }


        // Generar la proxima cuota si quedan cuotas por pendientes
        if (payableItem.getRemainingAmount() > 0) {


            Fee nextFee = new Fee();
            nextFee.setSale(payableItem);
            nextFee.setNumberFee(fee.getNumberFee() + 1);
            if(payableItem.getTypePayments() == Payments.MENSUAL) {
                nextFee.setExpirationDate(fee.getExpirationDate().plusMonths(1));
            }
            else if(payableItem.getTypePayments() == Payments.QUINCENAL) {
                nextFee.setExpirationDate(fee.getExpirationDate().plusDays(15));
            }
            else if(payableItem.getTypePayments() == Payments.SEMANAL) {
                nextFee.setExpirationDate(fee.getExpirationDate().plusWeeks(1));
            } else {
                throw new IllegalArgumentException("Tipo de pago no soportado: " + payableItem.getTypePayments()); 
            }

            // agregar logica para manejar cuotas adicionales en caso de que el numero de cuotas originales se haya completado
            //  y tengamos que generar una nueva cuota aumentar el numero de cuotas adicionales que tenemos en la venta
            // if (payableItem.getQuiantityFees() <= payableItem.getFees().size()) {
            //     // Si ya se han pagado todas las cuotas originales, incrementamos las cuotas adicionales
            //     payableItem.setAdditionalFees(payableItem.getAdditionalFees() + 1);
            //     nextFee.setAdditional(true); 
            // }else{

            //     nextFee.setAdditional(false);
            // }
            payableItem.getFees().add(nextFee);
            feeRepository.save(nextFee);
        }
            Sale sale = (Sale) payableItem;
            sale.setFinalPaymentDate(fee.getExpirationDate());
            sale.setRealFinalDate(LocalDate.now());
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
    
}
