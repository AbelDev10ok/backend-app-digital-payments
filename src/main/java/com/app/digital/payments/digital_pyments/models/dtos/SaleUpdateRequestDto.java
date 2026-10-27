package com.app.digital.payments.digital_pyments.models.dtos;

import com.app.digital.payments.digital_pyments.utils.IValueOfEnum;
import com.app.digital.payments.digital_pyments.utils.SaleKind;

import io.micrometer.common.lang.NonNull;

public class SaleUpdateRequestDto {    @NonNull

    private String descriptionProduct;

    // Categoría de producto (solo obligatoria para VENTA)
    private Long productType;

    // Variante/SKU vendido (opcional, solo VENTA)
    private Long variant;

    @IValueOfEnum(enumClass = SaleKind.class, message = "El tipo de transacción debe ser VENTA o PRESTAMO")
    private String kind;

    // Corrección deliberada del vendedor sellado en la operación. Null = sin
    // atribuir (las reasignaciones del cliente no pasan por acá).
    private Long sellerId;

    public String getDescriptionProduct() {
        return descriptionProduct;
    }

    public void setDescriptionProduct(String descriptionProduct) {
        this.descriptionProduct = descriptionProduct;
    }

    public Long getProductType() {
        return productType;
    }

    public void setProductType(Long productType) {
        this.productType = productType;
    }

    public Long getVariant() {
        return variant;
    }

    public void setVariant(Long variant) {
        this.variant = variant;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    
    
    
}
