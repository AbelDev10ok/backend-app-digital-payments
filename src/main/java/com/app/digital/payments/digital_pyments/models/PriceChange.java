package com.app.digital.payments.digital_pyments.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Historial de un cambio de precio del catálogo.
 *
 * Guarda un snapshot de lo que había y lo que quedó: si mañana se borra la
 * variante, el registro sigue explicando qué se cobraba. El batch agrupa todos
 * los SKUs que se ajustaron juntos en la misma operación.
 */
@Entity
@Table(name = "price_change", indexes = {
        @Index(name = "idx_price_change_business_product", columnList = "business_id, product_id")
})
public class PriceChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    // Snapshot de la variante: no hay FK para no bloquear el borrado del catálogo.
    @Column(nullable = false)
    private Long variantId;

    private Long productId;

    @Column(length = 200)
    private String productName;

    @Column(length = 120)
    private String sku;

    // Moneda del producto al momento del cambio (los importes no se reetiquetan).
    @Column(length = 8)
    private String currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal oldPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal newPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal oldCost;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal newCost;

    // PERCENT o AMOUNT: qué ajuste se aplicó y con qué valor.
    @Column(length = 10)
    private String adjustmentMode;

    @Column(precision = 12, scale = 4)
    private BigDecimal adjustmentValue;

    @Column(nullable = false)
    private Boolean adjustCost = false;

    // Paso de redondeo aplicado al precio nuevo, si hubo.
    @Column(precision = 12, scale = 2)
    private BigDecimal roundTo;

    @Column(length = 36)
    private String batchId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 200)
    private String createdBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
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