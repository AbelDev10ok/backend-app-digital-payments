package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ajuste masivo de precios del catálogo.
 *
 * {@code scope} decide a qué SKUs se aplica (ALL, CATEGORY o VARIANTS) y
 * {@code mode} cómo se calcula el valor nuevo (PERCENT o AMOUNT). El mismo
 * criterio se puede aplicar al costo ({@code adjustCost}) para conservar el
 * margen, y el precio nuevo se puede redondear a múltiplos de {@code roundTo}.
 */
public class BulkPriceRequestDto {

    private String scope;
    private Long categoryId;
    private List<Long> variantIds;
    private String mode;
    private BigDecimal value;
    private Boolean adjustCost;
    private BigDecimal roundTo;

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public List<Long> getVariantIds() {
        return variantIds;
    }

    public void setVariantIds(List<Long> variantIds) {
        this.variantIds = variantIds;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
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
}