package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un cambio de precio registrado en el historial del catálogo. */
public class PriceChangeDto {

    private Long id;
    private Long variantId;
    private Long productId;
    private String productName;
    private String sku;
    private String currency;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private BigDecimal oldCost;
    private BigDecimal newCost;
    private String adjustmentMode;
    private BigDecimal adjustmentValue;
    private Boolean adjustCost;
    private BigDecimal roundTo;
    private String batchId;
    private LocalDateTime createdAt;
    private String createdBy;

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

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getOldPrice() {
        return oldPrice;
    }

    public void setOldPrice(BigDecimal oldPrice) {
        this.oldPrice = oldPrice;
    }

    public BigDecimal getNewPrice() {
        return newPrice;
    }

    public void setNewPrice(BigDecimal newPrice) {
        this.newPrice = newPrice;
    }

    public BigDecimal getOldCost() {
        return oldCost;
    }

    public void setOldCost(BigDecimal oldCost) {
        this.oldCost = oldCost;
    }

    public BigDecimal getNewCost() {
        return newCost;
    }

    public void setNewCost(BigDecimal newCost) {
        this.newCost = newCost;
    }

    public String getAdjustmentMode() {
        return adjustmentMode;
    }

    public void setAdjustmentMode(String adjustmentMode) {
        this.adjustmentMode = adjustmentMode;
    }

    public BigDecimal getAdjustmentValue() {
        return adjustmentValue;
    }

    public void setAdjustmentValue(BigDecimal adjustmentValue) {
        this.adjustmentValue = adjustmentValue;
    }

    public Boolean getAdjustCost() {
        return adjustCost;
    }

    public void setAdjustCost(Boolean adjustCost) {
        this.adjustCost = adjustCost;
    }

    public BigDecimal getRoundTo() {
        return roundTo;
    }

    public void setRoundTo(BigDecimal roundTo) {
        this.roundTo = roundTo;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
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
}