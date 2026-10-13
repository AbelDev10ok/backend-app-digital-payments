package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;

public class ProductDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private Long productTypeId;
    private String productTypeName;

    public ProductDto() {}

    public ProductDto(Long id, String name, BigDecimal price, Integer stock, Long productTypeId, String productTypeName) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.productTypeId = productTypeId;
        this.productTypeName = productTypeName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Long getProductTypeId() {
        return productTypeId;
    }

    public void setProductTypeId(Long productTypeId) {
        this.productTypeId = productTypeId;
    }

    public String getProductTypeName() {
        return productTypeName;
    }

    public void setProductTypeName(String productTypeName) {
        this.productTypeName = productTypeName;
    }
}