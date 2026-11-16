package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;

/**
 * Una línea del ingreso de mercadería: qué variante entra, cuántas unidades y,
 * opcionalmente, a qué costo unitario se compró.
 */
public class StockEntryLineDto {

    private Long variantId;
    private Integer quantity;
    private BigDecimal unitCost;

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }
}
