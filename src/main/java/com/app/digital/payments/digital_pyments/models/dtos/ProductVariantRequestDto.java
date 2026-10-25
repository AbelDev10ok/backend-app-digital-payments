package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Alta/edición de una variante. En la creación el {@code id} va vacío; en la
 * edición identifica la variante existente que se actualiza.
 */
public class ProductVariantRequestDto {

    private Long id;
    private String sku;
    private BigDecimal cost;
    private BigDecimal price;
    private Integer stock;
    private String barcode;
    private Boolean active;
    private List<Long> attributeValueIds = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<Long> getAttributeValueIds() {
        return attributeValueIds;
    }

    public void setAttributeValueIds(List<Long> attributeValueIds) {
        this.attributeValueIds = attributeValueIds;
    }
}
