package com.app.digital.payments.digital_pyments.models;

import java.math.BigDecimal;
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
import jakarta.persistence.Version;

import org.hibernate.annotations.Filter;

/**
 * Variante vendible (SKU) de un producto base.
 *
 * Cada variante tiene su propio costo, precio de venta y stock. El stock es
 * obligatorio y nunca negativo: no existe el "ilimitado" para esta app.
 * La moneda es la del producto base: el importe guardado no se reinterpreta.
 */
@Entity
@Table(name = "product_variant", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_variant_business_sku", columnNames = { "business_id", "sku" })
})
@Filter(name = "tenantFilter", condition = "business_id = :businessId")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false, length = 120)
    private String sku;

    // Costo unitario del negocio y precio de venta unitario, separados.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    // Stock obligatorio: siempre un número, nunca null, nunca negativo.
    @Column(nullable = false)
    private Integer stock;

    @Column(length = 120)
    private String barcode;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VariantAttribute> attributes = new ArrayList<>();

    // Bloqueo optimista: evita vender dos veces la última unidad (409 al segundo).
    @Version
    @Column(columnDefinition = "bigint default 0")
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
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

    public List<VariantAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<VariantAttribute> attributes) {
        this.attributes = attributes;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
