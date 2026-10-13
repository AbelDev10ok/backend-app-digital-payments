package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.services.iservices.IFeeServices;
import com.app.digital.payments.digital_pyments.utils.FeeScheduleCalculator;
import com.app.digital.payments.digital_pyments.utils.PaymentMethod;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantGuard;

import jakarta.persistence.EntityNotFoundException;

import static com.app.digital.payments.digital_pyments.mappers.FeeMapper.convertToFeeDto;

@Service
public class FeeServices implements IFeeServices {

    private static final Logger log = LoggerFactory.getLogger(FeeServices.class);

    private final IFeeRepository feeRepository;

    private final ISaleRepository saleRepository;

    FeeServices(IFeeRepository feeRepository, ISaleRepository saleRepository) {
        this.feeRepository = feeRepository;
        this.saleRepository = saleRepository;
    }

    @Override
    @Transactional
    public void deleteFee(Long feeId) {
        Fee fee = getFeeById(feeId);
        Sale sale = fee.getSale();

        if (sale.getFees().size() == 1 && sale.getFees().contains(fee)) {
            throw new IllegalArgumentException("No se puede eliminar la única cuota de una venta. Elimine la venta completa si es necesario.");
        }

        BigDecimal feeAmount = fee.getAmount() != null ? fee.getAmount() : BigDecimal.ZERO;
        if (Boolean.TRUE.equals(fee.getPaid())) {
            sale.setRemainingAmount(sale.getRemainingAmount().add(feeAmount));
        }

        if (sale.getStatus() == SaleStatus.COMPLETED
                && sale.getFees().stream().filter(f -> !f.getPaid() && !f.getId().equals(feeId)).count() == 0) {
            sale.setStatus(SaleStatus.ACTIVE);
            sale.setRealFinalDate(null);
        }

        int nextFeeNumber = sale.getFees().stream().mapToInt(Fee::getNumberFee).max().orElse(0) + 1;

        sale.getFees().remove(fee);
        feeRepository.delete(fee);

        boolean hasUnpaidFees = sale.getFees().stream().anyMatch(f -> !f.getPaid());
        if (Boolean.TRUE.equals(fee.getPaid()) && sale.getRemainingAmount().signum() > 0 && !hasUnpaidFees) {
            Fee nextFee = new Fee();
            nextFee.setSale(sale);
            nextFee.setNumberFee(nextFeeNumber);
            nextFee.setBusiness(sale.getBusiness());

            Fee lastPaidFee = sale.getFees().stream()
                    .filter(Fee::getPaid)
                    .filter(f -> f.getExpirationDate() != null)
                    .max((f1, f2) -> f1.getExpirationDate().compareTo(f2.getExpirationDate()))
                    .orElse(null);

            if (lastPaidFee == null) {
                throw new IllegalStateException("No hay una cuota pagada para calcular la siguiente cuota");
            }
            nextFee.setExpirationDate(FeeScheduleCalculator.nextExpirationDate(lastPaidFee.getExpirationDate(), sale.getTypePayments()));
            nextFee.setAmount(sale.getAmountFee());

            sale.getFees().add(nextFee);
            feeRepository.save(nextFee);
        } else if (sale.getRemainingAmount().signum() <= 0 && sale.getStatus() != SaleStatus.COMPLETED) {
            sale.setStatus(SaleStatus.COMPLETED);
            sale.setRealFinalDate(LocalDate.now());
        }
    }

    // metodos de registro de pago de cuota
    @Override
    @Transactional
    public FeeDto registerPayment(Long feeId, BigDecimal amount, LocalDate date, String paymentMethod) {
        Fee fee = getFeeById(feeId);
        Sale sale = fee.getSale();

        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("El método de pago es obligatorio");
        }
        PaymentMethod pm;
        try {
            pm = PaymentMethod.valueOf(paymentMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Método de pago inválido: " + paymentMethod + ". Use EFECTIVO o TRANSFERENCIA");
        }

        if (date != null && date.isBefore(sale.getDateCreation())) {
            throw new IllegalArgumentException("La fecha de pago no puede ser anterior a la fecha de creación de la venta");
        }

        List<Fee> paidFees = feeRepository.findBySaleId(sale.getId());
        for (Fee paidFee : paidFees) {
            if (Boolean.TRUE.equals(paidFee.getPaid()) && paidFee.getDatePayment() != null) {
                if (date != null && paidFee.getDatePayment().isEqual(date)) {
                    throw new IllegalArgumentException("Ya existe un pago registrado para la fecha: " + date);
                }
            }
        }

        if (Boolean.TRUE.equals(fee.getPaid())) {
            throw new IllegalStateException("La cuota ya está pagada");
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }

        // Evitar sobrepago: el monto no puede superar el saldo restante de la venta
        if (amount.compareTo(sale.getRemainingAmount()) > 0) {
            throw new IllegalArgumentException("El monto del pago no puede ser mayor al saldo restante de la venta");
        }

        sale.deductFromRemainingAmount(amount);

        fee.setPaid(true);
        fee.setAmount(amount);
        fee.setDatePayment(date);
        fee.setPaymentMethod(pm);
        feeRepository.save(fee);

        if (sale.getRemainingAmount().signum() <= 0) {
            sale.setStatus(SaleStatus.COMPLETED);
            sale.setRemainingAmount(BigDecimal.ZERO);
            sale.setRealFinalDate(date != null ? date : LocalDate.now());
        }

        if (sale.getRemainingAmount().signum() > 0) {
            Fee nextFee = new Fee();
            nextFee.setSale(sale);
            nextFee.setNumberFee(fee.getNumberFee() + 1);
            nextFee.setExpirationDate(FeeScheduleCalculator.nextExpirationDate(fee.getExpirationDate(), sale.getTypePayments()));
            nextFee.setAmount(sale.getAmountFee());
            nextFee.setPaid(false);
            nextFee.setBusiness(sale.getBusiness());
            sale.getFees().add(nextFee);
            feeRepository.save(nextFee);
        }

        saleRepository.save(sale);

        return convertToFeeDto(fee);
    }

    // metodo para actualizar pago
    @Override
    @Transactional
    public FeeDto postponeFee(Long feeId, LocalDate newDataExpiration, LocalDate newDatePayment, BigDecimal amount) {

        Fee fee = getFeeById(feeId);
        Sale sale = fee.getSale();

        if (newDataExpiration != null && !Objects.equals(newDataExpiration, fee.getExpirationDate())) {
            if (newDataExpiration.isBefore(sale.getDateCreation())) {
                throw new IllegalArgumentException("La fecha no puede ser anterior a la creación");
            }
            if (sale.getFees().stream().anyMatch(f -> f.getExpirationDate().equals(newDataExpiration))) {
                throw new IllegalArgumentException("Ya existe una cuota con esa fecha de expiración en esta venta");
            }
            fee.setExpirationDate(newDataExpiration);
            feeRepository.save(fee);
            return convertToFeeDto(fee);
        }

        if (newDatePayment != null && !Objects.equals(newDatePayment, fee.getDatePayment())) {
            if (newDatePayment.isBefore(sale.getDateCreation())) {
                throw new IllegalArgumentException("La fecha no puede ser anterior a la creación");
            }
            fee.setDatePayment(newDatePayment);
        }

        BigDecimal oldAmount = fee.getAmount() != null ? fee.getAmount() : BigDecimal.ZERO;
        BigDecimal currentRemaining = sale.getRemainingAmount() != null ? sale.getRemainingAmount() : BigDecimal.ZERO;

        if (amount != null) {
            BigDecimal difference = oldAmount.subtract(amount);
            sale.setRemainingAmount(currentRemaining.add(difference));
            fee.setAmount(amount);

            if (amount.signum() == 0) {
                fee.setPaid(false);
                fee.setDatePayment(null);
            }
        }

        if (sale.getRemainingAmount().signum() < 0) {
            throw new IllegalArgumentException("El monto del pago no puede ser mayor al saldo restante de la venta");
        }

        if (sale.getRemainingAmount().signum() > 0) {
            boolean hasUnpaidFees = sale.getFees().stream().anyMatch(f -> !f.getPaid());
            if (!hasUnpaidFees) {
                Fee lastPaidFee = sale.getFees().stream()
                        .filter(Fee::getPaid)
                        .filter(f -> f.getExpirationDate() != null)
                        .max((f1, f2) -> f1.getExpirationDate().compareTo(f2.getExpirationDate()))
                        .orElse(null);

                if (lastPaidFee != null) {
                    Fee nextFee = new Fee();
                    nextFee.setSale(sale);
                    nextFee.setNumberFee(lastPaidFee.getNumberFee() + 1);
                    nextFee.setExpirationDate(FeeScheduleCalculator.nextExpirationDate(lastPaidFee.getExpirationDate(), sale.getTypePayments()));
                    nextFee.setPaid(false);
                    nextFee.setAmount(sale.getAmountFee());
                    nextFee.setBusiness(sale.getBusiness());
                    sale.getFees().add(nextFee);
                    feeRepository.save(nextFee);
                }
            }
            sale.setStatus(SaleStatus.ACTIVE);
        } else {
            feeRepository.save(fee);
            sale.setStatus(SaleStatus.COMPLETED);
        }

        saleRepository.save(sale);

        return convertToFeeDto(fee);
    }

    @Override
    @Transactional
    public Fee getFeeById(Long feeId) {
        Fee fee = feeRepository.findById(feeId)
                .orElseThrow(() -> new EntityNotFoundException("Cuota no encontrada con ID: " + feeId));
        // La cuota (via su venta) tiene que pertenecer al negocio autenticado
        TenantGuard.verificar(fee.getBusiness() != null ? fee.getBusiness().getId() : null);
        return fee;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeeDto> getFeeBySaleId(Long feeId) {
        List<Fee> fees = feeRepository.findBySaleId(feeId);
        return fees.stream()
                .map(fee -> convertToFeeDto(fee))
                .toList();
    }

}
