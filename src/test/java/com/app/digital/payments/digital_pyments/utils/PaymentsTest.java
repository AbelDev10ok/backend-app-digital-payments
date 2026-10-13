package com.app.digital.payments.digital_pyments.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PaymentsTest {

    @Test
    void fromStringEsCaseInsensitive() {
        assertEquals(Payments.MENSUAL, Payments.fromString("mensual"));
        assertEquals(Payments.MENSUAL, Payments.fromString("Mensual"));
        assertEquals(Payments.SEMANAL, Payments.fromString("  SEMANAL "));
    }

    @Test
    void fromStringRechazaValorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> Payments.fromString("ANUAL"));
    }

    @Test
    void fromStringRechazaNullOVacio() {
        assertThrows(IllegalArgumentException.class, () -> Payments.fromString(null));
        assertThrows(IllegalArgumentException.class, () -> Payments.fromString(" "));
    }
}
