package com.app.digital.payments.digital_pyments.utils;

public enum SaleKind {
    VENTA,
    PRESTAMO;

    public static SaleKind fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El tipo de transacción no puede ser nulo o vacío");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de transacción no válido: " + value, e);
        }
    }
}
