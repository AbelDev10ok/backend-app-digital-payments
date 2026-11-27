package com.app.digital.payments.digital_pyments.models.dtos;

/**
 * Ajuste por conteo físico: se manda lo contado y el backend calcula la
 * diferencia contra el stock actual. El motivo es obligatorio.
 */
public class StockAdjustmentRequestDto {

    private Long variantId;
    private Integer countedQuantity;
    private String reason;

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Integer getCountedQuantity() {
        return countedQuantity;
    }

    public void setCountedQuantity(Integer countedQuantity) {
        this.countedQuantity = countedQuantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
