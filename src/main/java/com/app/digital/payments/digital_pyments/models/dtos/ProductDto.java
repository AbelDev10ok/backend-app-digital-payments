package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Fila vendible del catálogo: un producto base con una de sus variantes.
 * {@code id} es el producto base y {@code variantId} el SKU que se elige al
 * vender. Un producto simple devuelve una sola fila.
 */
public class ProductDto {
    private Long id;
    private Long variantId;
    private String name;
    private String description;
    private String brand;
    private String sku;
    private String variantLabel;
    private BigDecimal price;
    private BigDecimal cost;
    private Integer stock;
    private String barcode;
    // Fila vendible: true solo si el producto base Y la variante estan activos.
    private Boolean active;
    // Estado del producto base, para la gestion del catalogo.
    private Boolean productActive;
    // Estado propio de la variante (sin combinar con el producto): el formulario
    // de edicion lo usa para no pisarlo cuando se activa o desactiva el producto.
    private Boolean variantActive;
    private Long productTypeId;
    private String productTypeName;

    // Moneda (ISO 4217) del precio, congelada al crear el producto (snapshot)
    private String currency;

    private List<VariantAttributeDto> attributes = new ArrayList<>();

    public ProductDto() {}

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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
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

    public Boolean getProductActive() {
        return productActive;
    }

    public void setProductActive(Boolean productActive) {
        this.productActive = productActive;
    }

    public Boolean getVariantActive() {
        return variantActive;
    }

    public void setVariantActive(Boolean variantActive) {
        this.variantActive = variantActive;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<VariantAttributeDto> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<VariantAttributeDto> attributes) {
        this.attributes = attributes != null ? attributes : new ArrayList<>();
    }
}
