package com.app.digital.payments.digital_pyments.utils;

public enum Payments {
    SEMANAL,
    QUINCENAL,
    MENSUAL,
    CONTADO;

    public static Payments fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El tipo de pago no puede ser nulo o vacío");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de pago no válido: " + value, e);
        }
    }
}
