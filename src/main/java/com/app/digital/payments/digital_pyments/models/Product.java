package com.app.digital.payments.digital_pyments.models;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Filter;

/**
 * Producto base del catálogo.
 *
 * No tiene precio ni stock propios: lo que se vende es una {@link ProductVariant}
 * (SKU), que guarda costo, precio y stock. Un producto simple tiene una única
 * variante sin atributos; uno con variantes declara en {@link ProductAttribute}
 * qué atributos las generan.
 */
@Entity
@Table(name = "product", uniqueConstraints = {
		@UniqueConstraint(name = "uk_product_business_name", columnNames = { "business_id", "name" })
})
@Filter(name = "tenantFilter", condition = "business_id = :businessId")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    private String brand;

    // Un producto vendido no se borra: se desactiva para conservar el historial.
    // El default en DDL permite agregar la columna a filas ya existentes.
    @Column(nullable = false, columnDefinition = "boolean not null default true")
    private Boolean active = true;

    // Categoria opcional a la que pertenece el producto
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_type_id", nullable = true)
    private ProductType productType;

    // El nombre del producto es unico dentro del negocio (V8)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    // Moneda (ISO 4217) de los precios, congelada al crear el producto (snapshot).
    // Igual que en Sale: los montos no se convierten, solo se etiquetan.
    @Column(name = "currency", length = 3)
    private String currency;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductAttribute> attributes = new ArrayList<>();

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

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariant> variants) {
        this.variants = variants;
    }

    public List<ProductAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<ProductAttribute> attributes) {
        this.attributes = attributes;
    }
}
