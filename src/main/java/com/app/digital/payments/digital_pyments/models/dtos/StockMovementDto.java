package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.app.digital.payments.digital_pyments.utils.StockMovementType;

/**
 * Movimiento de inventario tal como lo lee el cliente: incluye el SKU del
 * momento y, cuando la operación lo conoce, el stock que quedó después del
 * movimiento ({@code stockAfter} es null en el historial).
 */
public class StockMovementDto {

    private Long id;
    private Long variantId;
    private String sku;
    private StockMovementType type;
    private Integer delta;
    private BigDecimal unitCost;
    private String reason;
    private Long saleId;
    private LocalDateTime createdAt;
    private String createdBy;
    private Integer stockAfter;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public StockMovementType getType() {
        return type;
    }

    public void setType(StockMovementType type) {
        this.type = type;
    }

    public Integer getDelta() {
        return delta;
    }

    public void setDelta(Integer delta) {
        this.delta = delta;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getSaleId() {
        return saleId;
    }

    public void setSaleId(Long saleId) {
        this.saleId = saleId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getStockAfter() {
        return stockAfter;
    }

    public void setStockAfter(Integer stockAfter) {
        this.stockAfter = stockAfter;
    }
}
