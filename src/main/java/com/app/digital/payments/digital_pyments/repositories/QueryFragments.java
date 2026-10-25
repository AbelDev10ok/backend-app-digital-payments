package com.app.digital.payments.digital_pyments.repositories;

// Fragmentos de JPQL compartidos entre repositorios.
public final class QueryFragments {

    private QueryFragments() {
    }

    // "Monto efectivo" de una cuota pendiente: lo que falta cobrar de esa cuota
    // (su monto, o el valor de cuota acordado si no tiene), menos lo ya cobrado en
    // ella, y acotado a la deuda restante de la venta.
    //
    // El tope importa: en una cuota impaga el monto guardado puede quedar por encima
    // de lo que la venta debe (datos viejos, o una cuota cobrada que se edito), y
    // nadie puede cobrar mas de lo que se debe. Las consultas usan los alias "f"
    // (Fee) y "s" (Sale).
    public static final String MONTO_EFECTIVO_CUOTA =
            "CASE WHEN ((CASE WHEN f.amount > 0 THEN f.amount ELSE s.amountFee END) - COALESCE(f.paidAmount, 0))"
                    + " > s.remainingAmount"
                    + " THEN s.remainingAmount"
                    + " ELSE ((CASE WHEN f.amount > 0 THEN f.amount ELSE s.amountFee END) - COALESCE(f.paidAmount, 0))"
                    + " END";
}
