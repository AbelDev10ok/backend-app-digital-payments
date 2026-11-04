package com.app.digital.payments.digital_pyments.utils;

public enum FeeStatus {
    TODAY,       // Cuotas que vencen hoy
    PENDING,     // Todas las pendientes
    UPCOMING,    // Cuotas pendientes que aun no vencen
    PAID,        // Cuotas pagadas
    POSTPONED,   // Cuotas pospuestas
    DELAYED,     // Cuotas atrasadas
    A_COBRAR     // Impagas con vencimiento dentro de los proximos 30 dias (incluye vencidas y de hoy)
}
