package com.app.digital.payments.digital_pyments.utils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Payment;
import com.app.digital.payments.digital_pyments.models.Sale;

/**
 * Aplica un cobro sobre el plan de cuotas de una operación.
 *
 * El plan es fijo: las cuotas se materializan al crear la operación y un cobro
 * nunca crea ni reprograma cuotas. La plata entra por la cuota impaga más
 * antigua (por número) y el excedente sigue con la siguiente, en cascada; una
 * cuota se cierra recién cuando su acumulado cubre su monto programado.
 */
public final class FeePaymentAllocator {

    private FeePaymentAllocator() {
    }

    /**
     * Aplica {@code monto} sobre las cuotas impagas de la venta, de la más
     * antigua a la más nueva. No persiste nada: los servicios guardan.
     *
     * Primero planifica la asignación completa sin mutar nada y sólo después
     * registra los cobros, así un plan que no cubre el monto no deja pagos a
     * medias.
     *
     * @return las cuotas que recibieron cobro, en el orden en que se aplicó.
     */
    public static List<Fee> aplicar(Sale sale, BigDecimal monto, LocalDateTime cuando, PaymentMethod metodo) {
        List<Fee> impagas = sale.getFees().stream()
                .filter(fee -> !Boolean.TRUE.equals(fee.getPaid()) && fee.getPendingAmount().signum() > 0)
                .sorted(Comparator.comparing(Fee::getNumberFee, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // Fase 1: planificar la asignación sin tocar ninguna cuota
        List<Fee> cuotas = new ArrayList<>();
        List<BigDecimal> montos = new ArrayList<>();
        BigDecimal restante = monto;
        for (Fee cuota : impagas) {
            if (restante.signum() <= 0) {
                break;
            }
            BigDecimal aplicado = cuota.getPendingAmount().min(restante);
            cuotas.add(cuota);
            montos.add(aplicado);
            restante = restante.subtract(aplicado);
        }
        if (restante.signum() > 0) {
            throw new IllegalStateException(
                    "El plan de cuotas no cubre la deuda restante; revisá las cuotas de la operación");
        }

        // Fase 2: registrar los cobros planificados
        for (int i = 0; i < cuotas.size(); i++) {
            Fee cuota = cuotas.get(i);
            Payment pago = new Payment(cuota, montos.get(i), cuando, metodo);
            pago.setBusiness(sale.getBusiness());
            cuota.applyPayment(pago);
        }
        return cuotas;
    }
}
