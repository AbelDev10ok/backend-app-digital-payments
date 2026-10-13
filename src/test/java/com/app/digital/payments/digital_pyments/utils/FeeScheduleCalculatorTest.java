package com.app.digital.payments.digital_pyments.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class FeeScheduleCalculatorTest {

    private final LocalDate base = LocalDate.of(2026, 8, 6);

    @Test
    void nextExpirationDateSemanal() {
        assertEquals(base.plusWeeks(1), FeeScheduleCalculator.nextExpirationDate(base, Payments.SEMANAL));
    }

    @Test
    void nextExpirationDateQuincenal() {
        assertEquals(base.plusDays(15), FeeScheduleCalculator.nextExpirationDate(base, Payments.QUINCENAL));
    }

    @Test
    void nextExpirationDateMensual() {
        assertEquals(base.plusMonths(1), FeeScheduleCalculator.nextExpirationDate(base, Payments.MENSUAL));
    }

    @Test
    void nextExpirationDateContado() {
        assertEquals(base.plusDays(1), FeeScheduleCalculator.nextExpirationDate(base, Payments.CONTADO));
    }

    @Test
    void nextExpirationDateRechazaNull() {
        assertThrows(IllegalArgumentException.class,
                () -> FeeScheduleCalculator.nextExpirationDate(null, Payments.MENSUAL));
    }

    @Test
    void finalPaymentDateMensual() {
        assertEquals(base.plusMonths(5).minusDays(1),
                FeeScheduleCalculator.finalPaymentDate(base, Payments.MENSUAL, 6));
    }

    @Test
    void finalPaymentDateContado() {
        assertEquals(base, FeeScheduleCalculator.finalPaymentDate(base, Payments.CONTADO, 1));
    }

    @Test
    void finalPaymentDateUnaSolaCuotaMensual() {
        assertEquals(base.minusDays(1), FeeScheduleCalculator.finalPaymentDate(base, Payments.MENSUAL, 1));
    }

    @Test
    void finalPaymentDateSemanal() {
        assertEquals(base.plusWeeks(5).minusDays(1),
                FeeScheduleCalculator.finalPaymentDate(base, Payments.SEMANAL, 6));
    }

    @Test
    void finalPaymentDateSemanalUnaCuota() {
        assertEquals(base.minusDays(1), FeeScheduleCalculator.finalPaymentDate(base, Payments.SEMANAL, 1));
    }

    @Test
    void finalPaymentDateQuincenal() {
        assertEquals(base.plusDays(75).minusDays(1),
                FeeScheduleCalculator.finalPaymentDate(base, Payments.QUINCENAL, 6));
    }

    @Test
    void finalPaymentDateQuincenalUnaCuota() {
        assertEquals(base.minusDays(1), FeeScheduleCalculator.finalPaymentDate(base, Payments.QUINCENAL, 1));
    }

    @Test
    void finalPaymentDateRechazaNull() {
        assertThrows(IllegalArgumentException.class,
                () -> FeeScheduleCalculator.finalPaymentDate(null, Payments.MENSUAL, 1));
    }

    @Test
    void finalPaymentDateMantieneFinDeMes() {
        LocalDate finMes = LocalDate.of(2026, 1, 31);
        LocalDate esperado = finMes.plusMonths(5).minusDays(1);
        assertEquals(esperado, FeeScheduleCalculator.finalPaymentDate(finMes, Payments.MENSUAL, 6));
    }
}
