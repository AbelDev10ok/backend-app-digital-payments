package com.app.digital.payments.digital_pyments.utils;

/**
 * Tipo de movimiento de inventario.
 *
 * <p>
 * INGRESO: mercadería que entra (compra, reposición o carga inicial).
 * VENTA: unidades que salen por una venta (delta negativo).
 * ANULACION: unidades que vuelven por anular o borrar una venta (delta positivo).
 * AJUSTE: corrección por conteo físico (delta con signo, nunca 0).
 * </p>
 */
public enum StockMovementType {
    INGRESO,
    VENTA,
    ANULACION,
    AJUSTE
}
