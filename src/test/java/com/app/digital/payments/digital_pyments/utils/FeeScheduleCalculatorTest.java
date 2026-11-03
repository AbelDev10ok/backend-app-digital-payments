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
}
