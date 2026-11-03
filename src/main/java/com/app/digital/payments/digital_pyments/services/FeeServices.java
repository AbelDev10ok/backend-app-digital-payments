package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Payment;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;
import com.app.digital.payments.digital_pyments.models.dtos.PaymentDto;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.IPaymentRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.services.iservices.IFeeServices;
import com.app.digital.payments.digital_pyments.utils.FeePaymentAllocator;
import com.app.digital.payments.digital_pyments.utils.FeeScheduleCalculator;
import com.app.digital.payments.digital_pyments.utils.PaymentMethod;
import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantGuard;

import jakarta.persistence.EntityNotFoundException;

import static com.app.digital.payments.digital_pyments.mappers.FeeMapper.convertToFeeDto;

@Service
public class FeeServices implements IFeeServices {

    private static final Logger log = LoggerFactory.getLogger(FeeServices.class);

    private final IFeeRepository feeRepository;

    private final ISaleRepository saleRepository;

    private final IPaymentRepository paymentRepository;

    FeeServices(IFeeRepository feeRepository, ISaleRepository saleRepository, IPaymentRepository paymentRepository) {
        this.feeRepository = feeRepository;
        this.saleRepository = saleRepository;
        this.paymentRepository = paymentRepository;
    }

    // Registro de un cobro: la plata entra por la cuota impaga más antigua y el
    // excedente cascada a las siguientes, sin crear ni reprogramar cuotas y sin
    // reescribir el monto programado de ninguna. Reglas:
    //  - El monto no puede superar el saldo restante de la venta.
    //  - No hay límite de pagos por día: se permite más de un cobro en la misma
    //    fecha (cada uno queda con su horario).
    //  - El horario lo sella el backend.
    @Override
    @Transactional
    public FeeDto registerPayment(Long feeId, BigDecimal amount, LocalDate date, String paymentMethod) {
        Fee fee = getFeeById(feeId);
        Sale sale = fee.getSale();

        // Una venta anulada es terminal: no se le registran cobros nuevos.
        if (sale.getStatus() == SaleStatus.CANCELED) {
            throw new IllegalStateException("No se puede cobrar una cuota de una venta anulada");
        }

        PaymentMethod pm = parsearMetodoPago(paymentMethod);

        if (date != null && date.isBefore(sale.getDateCreation())) {
            throw new IllegalArgumentException("La fecha de pago no puede ser anterior a la fecha de creación de la venta");
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }

        if (Boolean.TRUE.equals(fee.getPaid())) {
            throw new IllegalStateException("La cuota ya está pagada");
        }

        // El saldo se recalcula antes de validar: el tope del cobro es la deuda real
        // (total menos cobrado), no un número guardado que pueda estar viejo
        sale.recalcularSaldo();

        BigDecimal saldo = sale.getRemainingAmount() != null ? sale.getRemainingAmount() : BigDecimal.ZERO;
        if (amount.compareTo(saldo) > 0) {
            throw new IllegalArgumentException("El monto del pago no puede ser mayor al saldo restante de la venta");
        }

        // El cobro se reparte desde la cuota impaga más antigua: la cuota nombrada
        // sólo importa para las validaciones (cubrir deuda vieja no es un error).
        LocalDate fechaCobro = date != null ? date : LocalDate.now();
        // Si se backdatea la fecha, la hora guardada es la real del registro
        List<Fee> cuotasCobradas = FeePaymentAllocator.aplicar(
                sale, amount, fechaCobro.atTime(LocalTime.now()), pm);

        // El saldo sale de los cobros registrados, no de una resta a mano
        sale.recalcularSaldo();
        if (sale.getRemainingAmount().signum() <= 0) {
            sale.setStatus(SaleStatus.COMPLETED);
            sale.setRealFinalDate(fechaCobro);
            // La deuda quedo en cero: las cuotas impagas sobrantes ya no se cobran
            eliminarCuotasImpagasSobrantes(sale, fee);
        } else {
            sale.setStatus(SaleStatus.ACTIVE);
        }

        cuotasCobradas.forEach(feeRepository::save);
        saleRepository.save(sale);

        log.debug("Cobro registrado: cuota={} monto={} fecha={}", feeId, amount, fechaCobro);
        return convertToFeeDto(cuotasCobradas.get(0));
    }

    // Edición de una cuota: fecha de vencimiento, fecha de pago y monto.
    //  - Cuota impaga: su monto es sólo el valor de esa cuota; la deuda no se mueve
    //    porque todavía no entró plata.
    //  - Cuota ya cobrada: editar el monto es re-declarar cuánto se cobró en ella y
    //    el cobro registrado se ajusta al nuevo monto, con lo que la deuda cambia
    //    por la diferencia.
    // En todos los casos el saldo se recalcula desde los cobros (total - cobrado).
    @Override
    @Transactional
    public FeeDto postponeFee(Long feeId, LocalDate newDataExpiration, LocalDate newDatePayment, BigDecimal amount) {

        Fee fee = getFeeById(feeId);
        Sale sale = fee.getSale();

        // Una venta anulada es terminal: sus cuotas ya no se modifican.
        if (sale.getStatus() == SaleStatus.CANCELED) {
            throw new IllegalStateException("No se puede modificar una cuota de una venta anulada");
        }

        // Saldo real antes de tocar nada (de paso, cura un saldo viejo guardado)
        sale.recalcularSaldo();

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

        if (newDatePayment != null && !Objects.equals(newDatePayment, fechaDePago(fee))) {
            if (newDatePayment.isBefore(sale.getDateCreation())) {
                throw new IllegalArgumentException("La fecha no puede ser anterior a la creación");
            }
            ajustarFechaDePago(fee, newDatePayment);
        }

        if (amount != null) {
            if (Boolean.TRUE.equals(fee.getPaid())) {
                // La cuota ya estaba cobrada: se re-declara cuánto se cobró en ella
                // y el cobro registrado se ajusta al nuevo monto
                ajustarCobroRegistrado(sale, fee, amount);
            }
            // En una cuota impaga el monto es sólo el valor de esa cuota: la deuda
            // no se mueve porque todavía no entró plata
            fee.setAmount(amount);
            fee.refreshPaid();
        }

        sale.recalcularSaldo();

        if (sale.getRemainingAmount().signum() > 0) {
            // Sin cuotas impagas la venta quedaría sin nada que cobrar: se agenda
            // una cuota impaga por la deuda restante
            if (sale.getFees().stream().allMatch(f -> Boolean.TRUE.equals(f.getPaid()))) {
                crearCuotaPorDeuda(sale);
            }
            sale.setStatus(SaleStatus.ACTIVE);
            sale.setRealFinalDate(null);
        } else {
            feeRepository.save(fee);
            // La deuda quedo en cero: las cuotas impagas sobrantes ya no se cobran
            eliminarCuotasImpagasSobrantes(sale, fee);
            sale.setStatus(SaleStatus.COMPLETED);
            if (sale.getRealFinalDate() == null) {
                sale.setRealFinalDate(LocalDate.now());
            }
        }

        // El plan es fijo: editar una cuota no re-deriva el monto programado de
        // las impagas. Sólo cambia lo que se editó explícitamente.
        saleRepository.save(sale);

        return convertToFeeDto(fee);
    }

    @Override
    @Transactional
    public List<FeeDto> reprogramFee(Long feeId, Integer periods, Integer days) {
        validarDeltaDeReprogramacion(periods, days);

        Fee fee = getFeeById(feeId);
        Sale sale = fee.getSale();

        // Una venta anulada es terminal y una completada ya no tiene plan que correr.
        if (sale.getStatus() == SaleStatus.CANCELED) {
            throw new IllegalStateException("No se puede reprogramar una cuota de una venta anulada");
        }
        if (sale.getStatus() == SaleStatus.COMPLETED) {
            throw new IllegalStateException("No se puede reprogramar una cuota de una venta completada");
        }
        if (Boolean.TRUE.equals(fee.getPaid())) {
            throw new IllegalStateException("La cuota ya está pagada");
        }

        // El tramo arranca en la cuota nombrada y sigue por número de cuota: la
        // lista de la venta no garantiza el orden del cronograma.
        List<Fee> ordenadas = new ArrayList<>(sale.getFees());
        ordenadas.sort(Comparator.comparing(Fee::getNumberFee, Comparator.nullsLast(Comparator.naturalOrder())));

        List<Fee> movidas = new ArrayList<>();
        boolean enElTramo = false;
        for (Fee cuota : ordenadas) {
            enElTramo = enElTramo || esLaMismaCuota(cuota, fee);
            // Las cobradas quedan selladas donde están, aunque caigan en el tramo
            if (!enElTramo || Boolean.TRUE.equals(cuota.getPaid())) {
                continue;
            }
            cuota.setExpirationDate(correr(cuota.getExpirationDate(), periods, days, sale.getTypePayments()));
            movidas.add(cuota);
        }

        // La fecha final esperada es la del plan que quedó agendado
        sale.setFinalPaymentDate(vencimientoDeLaUltimaCuota(sale));

        // Auditoría: una reprogramación es UN evento, mueva una o muchas cuotas
        sale.setReprogrammedAt(LocalDateTime.now());
        sale.setReprogramCount(sale.getReprogramCount() == null ? 1 : sale.getReprogramCount() + 1);

        movidas.forEach(feeRepository::save);
        saleRepository.save(sale);

        log.debug("Reprogramación en bloque: cuota={} periods={} days={} cuotasMovidas={}",
                feeId, periods, days, movidas.size());
        return movidas.stream().map(cuota -> convertToFeeDto(cuota)).toList();
    }

    // Exactamente uno de los dos deltas, y siempre mayor a 0: la reprogramación
    // nunca corre una cuota hacia atrás.
    private void validarDeltaDeReprogramacion(Integer periods, Integer days) {
        boolean indicoPeriodos = periods != null;
        boolean indicoDias = days != null;
        boolean unicoYPositivo = indicoPeriodos != indicoDias
                && (!indicoPeriodos || periods >= 1)
                && (!indicoDias || days >= 1);
        if (!unicoYPositivo) {
            throw new IllegalArgumentException("Indicá periods o days (exactamente uno, mayor a 0)");
        }
    }

    // Delta en períodos de la frecuencia (encadenando nextExpirationDate, la misma
    // semántica del alta) o en días naturales.
    private LocalDate correr(LocalDate vencimiento, Integer periods, Integer days, Payments frecuencia) {
        if (days != null) {
            return vencimiento.plusDays(days);
        }
        LocalDate corrido = vencimiento;
        for (int periodo = 0; periodo < periods; periodo++) {
            corrido = FeeScheduleCalculator.nextExpirationDate(corrido, frecuencia);
        }
        return corrido;
    }

    // La última cuota del plan es la de mayor número y su vencimiento es la fecha
    // final esperada de la operación (S11).
    private LocalDate vencimientoDeLaUltimaCuota(Sale sale) {
        return sale.getFees().stream()
                .filter(cuota -> cuota.getExpirationDate() != null)
                .max(Comparator.comparing(Fee::getNumberFee, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Fee::getExpirationDate)
                .orElse(null);
    }

    private static boolean esLaMismaCuota(Fee una, Fee otra) {
        if (una == otra) {
            return true;
        }
        return una.getId() != null && otra.getId() != null && una.getId().equals(otra.getId());
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

    // Historial de cobros de una cuota, con fecha y hora (para el detalle de cobro)
    @Override
    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByFeeId(Long feeId) {
        Fee fee = getFeeById(feeId);
        return paymentRepository.findByFeeIdOrderByPaidAtAscIdAsc(fee.getId()).stream()
                .map(p -> new PaymentDto(p.getId(), p.getAmount(), p.getPaidAt(),
                        p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null))
                .toList();
    }

    private PaymentMethod parsearMetodoPago(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("El método de pago es obligatorio");
        }
        try {
            return PaymentMethod.valueOf(paymentMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Método de pago inválido: " + paymentMethod + ". Use EFECTIVO o TRANSFERENCIA");
        }
    }

    private LocalDate fechaDePago(Fee fee) {
        return fee.getLastPaymentAt() != null ? fee.getLastPaymentAt().toLocalDate() : null;
    }

    // La "fecha de pago" que se muestra y edita es la del ultimo cobro de la cuota.
    private void ajustarFechaDePago(Fee fee, LocalDate nuevaFecha) {
        Payment ultimo = fee.getPayments().stream()
                .max(Comparator.comparing(Payment::getPaidAt))
                .orElse(null);
        LocalTime hora = ultimo != null ? ultimo.getPaidAt().toLocalTime()
                : (fee.getLastPaymentAt() != null ? fee.getLastPaymentAt().toLocalTime() : LocalTime.now());
        fee.setLastPaymentAt(nuevaFecha.atTime(hora));
        if (ultimo != null) {
            ultimo.setPaidAt(nuevaFecha.atTime(hora));
            paymentRepository.save(ultimo);
        }
    }

    // Una venta sin deuda no necesita cuotas por cobrar: las cuotas impagas que
    // quedaron planificadas y ya no representan deuda se eliminan para que el
    // cronograma no muestre algo que ya no hay que cobrar. No se tocan las cuotas
    // que tengan algun cobro registrado (su plata no se puede borrar sola) ni la
    // cuota que se esta editando en ese momento.
    private void eliminarCuotasImpagasSobrantes(Sale sale, Fee excepto) {
        if (sale.getFees().stream().noneMatch(f -> Boolean.TRUE.equals(f.getPaid()))) {
            // Sin ninguna cuota cobrada no hay nada que limpiar (y la venta no puede
            // quedarse sin cuotas)
            return;
        }
        List<Fee> sobrantes = sale.getFees().stream()
                .filter(f -> f != excepto && !Boolean.TRUE.equals(f.getPaid()) && f.getPaidAmount().signum() == 0)
                .toList();
        if (sobrantes.isEmpty()) {
            return;
        }
        sale.getFees().removeAll(sobrantes);
        sobrantes.forEach(feeRepository::delete);
    }

    // Agenda una cuota impaga por la deuda restante: se usa cuando la venta queda
    // con saldo pero sin ninguna cuota por cobrar (para no dejarla sin cobrar).
    private void crearCuotaPorDeuda(Sale sale) {
        Fee referencia = sale.getFees().stream()
                .filter(f -> f.getExpirationDate() != null)
                .max(Comparator.comparing(Fee::getExpirationDate))
                .orElseThrow(() -> new IllegalStateException("No hay cuotas para calcular la siguiente cuota"));
        crearCuota(sale, referencia, sale.montoProximaCuota());
    }

    private void crearCuota(Sale sale, Fee feeReferencia, BigDecimal monto) {
        int nextNumber = sale.getFees().stream().mapToInt(Fee::getNumberFee).max().orElse(0) + 1;

        Fee nextFee = new Fee();
        nextFee.setSale(sale);
        nextFee.setNumberFee(nextNumber);
        nextFee.setExpirationDate(FeeScheduleCalculator.nextExpirationDate(feeReferencia.getExpirationDate(), sale.getTypePayments()));
        nextFee.setAmount(monto);
        nextFee.setPaid(false);
        nextFee.setBusiness(sale.getBusiness());
        sale.getFees().add(nextFee);
        feeRepository.save(nextFee);
    }

    // Deja el cobro registrado de la cuota acorde a su nuevo monto, para que el
    // acumulado de la cuota siga coincidiendo con lo que la venta descontó de su
    // saldo: si el monto baja se recortan los cobros sobrantes (del mas nuevo al
    // mas viejo, conservando el horario de los que quedan) y si sube queda
    // registrado un cobro de ajuste por la diferencia.
    private void ajustarCobroRegistrado(Sale sale, Fee fee, BigDecimal nuevoMonto) {
        BigDecimal nuevo = nuevoMonto.max(BigDecimal.ZERO);
        BigDecimal cobrado = fee.getPaidAmount();

        if (nuevo.compareTo(cobrado) > 0) {
            LocalDateTime horario = fee.getLastPaymentAt() != null
                    ? fee.getLastPaymentAt()
                    : fee.getExpirationDate().atTime(LocalTime.now());
            PaymentMethod pm = fee.getPaymentMethod() != null ? fee.getPaymentMethod() : PaymentMethod.EFECTIVO;
            Payment ajuste = new Payment(fee, nuevo.subtract(cobrado), horario, pm);
            ajuste.setBusiness(sale.getBusiness());
            fee.applyPayment(ajuste);
            return;
        }

        List<Payment> pagos = new ArrayList<>(fee.getPayments());
        pagos.sort(Comparator.comparing(Payment::getPaidAt));
        BigDecimal acumulado = cobrado;
        for (int i = pagos.size() - 1; i >= 0 && acumulado.compareTo(nuevo) > 0; i--) {
            Payment pago = pagos.get(i);
            BigDecimal exceso = acumulado.subtract(nuevo);
            if (pago.getAmount().compareTo(exceso) <= 0) {
                acumulado = acumulado.subtract(pago.getAmount());
                fee.getPayments().remove(pago);
            } else {
                pago.setAmount(pago.getAmount().subtract(exceso));
                acumulado = nuevo;
            }
        }

        fee.setPaidAmount(nuevo);
        List<Payment> restantes = fee.getPayments();
        if (!restantes.isEmpty()) {
            fee.setPaidAmount(restantes.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            restantes.stream()
                    .max(Comparator.comparing(Payment::getPaidAt))
                    .ifPresent(ultimo -> {
                        fee.setLastPaymentAt(ultimo.getPaidAt());
                        fee.setPaymentMethod(ultimo.getPaymentMethod());
                    });
        } else if (nuevo.signum() == 0) {
            // Monto 0 deja la cuota sin cobro asociado
            fee.setLastPaymentAt(null);
            fee.setPaymentMethod(null);
        }
    }

}
