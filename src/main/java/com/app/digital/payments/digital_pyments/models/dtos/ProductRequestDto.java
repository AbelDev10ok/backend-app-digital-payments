package com.app.digital.payments.digital_pyments.models.dtos;

import java.util.ArrayList;
import java.util.List;

/**
 * Alta/edición de un producto base con sus variantes y los atributos que las
 * generan. Un producto simple manda una única variante sin valores de atributo.
 */
public class ProductRequestDto {

    private String name;
    private String description;
    private String brand;
    private Boolean active;
    private Long productTypeId;
    private String currency;
    private List<ProductVariantRequestDto> variants = new ArrayList<>();
    private List<Long> attributeIds = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getProductTypeId() {
        return productTypeId;
    }

    public void setProductTypeId(Long productTypeId) {
        this.productTypeId = productTypeId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<ProductVariantRequestDto> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariantRequestDto> variants) {
        this.variants = variants;
    }

    public List<Long> getAttributeIds() {
        return attributeIds;
    }

    public void setAttributeIds(List<Long> attributeIds) {
        this.attributeIds = attributeIds;
    }
}
