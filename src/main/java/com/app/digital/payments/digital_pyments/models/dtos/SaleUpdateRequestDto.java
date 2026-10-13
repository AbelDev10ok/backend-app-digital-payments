package com.app.digital.payments.digital_pyments.models.dtos;

import com.app.digital.payments.digital_pyments.utils.IValueOfEnum;
import com.app.digital.payments.digital_pyments.utils.SaleKind;

import io.micrometer.common.lang.NonNull;

public class SaleUpdateRequestDto {    @NonNull

    private String descriptionProduct;

    // Categoría de producto (solo obligatoria para VENTA)
    private Long productType;

    // Producto del catálogo (opcional, solo VENTA)
    private Long product;

    @IValueOfEnum(enumClass = SaleKind.class, message = "El tipo de transacción debe ser VENTA o PRESTAMO")
    private String kind;

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

    public Long getProduct() {
        return product;
    }

    public void setProduct(Long product) {
        this.product = product;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    
    
    
}
