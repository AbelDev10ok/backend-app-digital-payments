package com.app.digital.payments.digital_pyments.models.dtos;

/**
 * Qué hacer con la cartera de un vendedor al deshabilitarlo.
 *
 * Sin datos sólo se puede deshabilitar a un vendedor que no tenga clientes
 * asignados. Si tiene cartera hay que elegir explícitamente entre traspasarla
 * completa a otro vendedor o dejarla sin vendedor (la cobra el negocio, que es
 * la fila "Sin vendedor" de las métricas).
 */
public class SellerDeactivationRequest {

    /** Vendedor que recibe toda la cartera. */
    private Long transferToSellerId;

    /** Deja los clientes sin vendedor: la deuda pasa a "Sin vendedor". */
    private Boolean leaveClientsUnassigned;

    public Long getTransferToSellerId() {
        return transferToSellerId;
    }

    public void setTransferToSellerId(Long transferToSellerId) {
        this.transferToSellerId = transferToSellerId;
    }

    public Boolean getLeaveClientsUnassigned() {
        return leaveClientsUnassigned;
    }

    public void setLeaveClientsUnassigned(Boolean leaveClientsUnassigned) {
        this.leaveClientsUnassigned = leaveClientsUnassigned;
    }
}
