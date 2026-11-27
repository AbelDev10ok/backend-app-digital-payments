package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;

/** Fila del preview/aplicación de un ajuste masivo: un SKU con su antes y después. */
public class BulkPriceRowDto {

    private Long variantId;
    private Long productId;
    private String productName;
    private String sku;
    private String variantLabel;
    private String currency;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private BigDecimal oldCost;
    private BigDecimal newCost;
    /** false cuando el ajuste deja el SKU igual (redondeo o valor que no lo mueve). */
    private Boolean changed;

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

    public String getVariantLabel() {
        return variantLabel;
    }

    public void setVariantLabel(String variantLabel) {
        this.variantLabel = variantLabel;
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

    public Boolean getChanged() {
        return changed;
    }

    public void setChanged(Boolean changed) {
        this.changed = changed;
    }
}