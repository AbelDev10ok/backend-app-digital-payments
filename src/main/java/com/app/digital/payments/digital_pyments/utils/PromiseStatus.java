package com.app.digital.payments.digital_pyments.utils;

/**
 * Estado de una promesa de pago.
 *
 * Sólo se guardan PENDING, FULFILLED y CANCELLED. La condición de "vencida"
 * (la fecha prometida ya pasó y nadie cobró) no se persiste: se deriva por
 * fecha al leer, así no hace falta un proceso que la marque.
 */
public enum PromiseStatus {
    PENDING,     // El cliente prometió pagar y la fecha todavía no llegó (o llegó sin cobro)
    FULFILLED,   // Un cobro cubrió el monto prometido
    CANCELLED    // Se canceló a mano (el cliente no la sostiene o se renegoció)
}
