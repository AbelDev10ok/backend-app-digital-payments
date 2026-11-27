package com.app.digital.payments.digital_pyments.utils;

/**
 * Estado de un item de la ronda del dia.
 *
 * <p>
 * Solo {@link #NO_ESTABA} se guarda: es la unica marca que aporta el
 * cobrador. {@link #COBRADO} se deriva de la agenda vigente (si el cliente ya
 * no aparece, se cobro) y {@link #PENDIENTE} es el resto. Guardar "cobrado"
 * seria duplicar el estado de las cuotas, que es la fuente de verdad.
 * </p>
 */
public enum RoundItemStatus {
    PENDIENTE,
    COBRADO,
    NO_ESTABA
}
