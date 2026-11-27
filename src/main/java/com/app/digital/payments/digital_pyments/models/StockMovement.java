package com.app.digital.payments.digital_pyments.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.app.digital.payments.digital_pyments.utils.StockMovementType;

/**
 * Movimiento de inventario: una línea del libro de stock.
 *
 * <p>
 * Es append-only y la fuente de verdad del inventario: no se edita ni se borra.
 * {@code ProductVariant.stock} es una caché que se actualiza en la misma
 * transacción, y el invariante es {@code SUM(delta) == stock}.
 * </p>
 *
 * <p>
 * A propósito no hay relaciones JPA a {@code Business}, {@code ProductVariant}
 * ni {@code Sale}: los identificadores son columnas simples para que el
 * historial sobreviva al borrado de una venta o de una variante sin ventas.
 * El {@code sku} queda como snapshot para poder leer el movimiento sin joins.
 * </p>
 */
@Entity
@Table(name = "stock_movement", indexes = {
        @Index(name = "idx_stock_movement_business_variant", columnList = "business_id, variant_id, created_at"),
        @Index(name = "idx_stock_movement_business_type", columnList = "business_id, type, created_at")
})
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    // Snapshot del SKU: el movimiento se lee sin depender del catálogo vivo.
    @Column(nullable = false, length = 120)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockMovementType type;

    // Delta con signo. Nunca 0: un movimiento sin efecto no se registra.
    @Column(nullable = false)
    private Integer delta;

    // Costo unitario observado en un ingreso; no reescribe el costo del catálogo.
    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(length = 200)
    private String reason;

    // Venta que originó el movimiento (sin FK: la auditoría sobrevive al borrado).
    @Column(name = "sale_id")
    private Long saleId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
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
}
