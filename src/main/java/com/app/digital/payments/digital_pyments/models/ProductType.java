package com.app.digital.payments.digital_pyments.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Filter;

/**
 * Categoría del catálogo. Puede colgar de otra categoría (jerarquía por
 * negocio). El slug es estable para URLs o filtros y no cambia al renombrar.
 */
@Entity
@Table(name = "product_type", uniqueConstraints = {
		@UniqueConstraint(name = "uk_product_type_business_name", columnNames = { "business_id", "name" }),
		@UniqueConstraint(name = "uk_product_type_business_slug", columnNames = { "business_id", "slug" })
})
@Filter(name = "tenantFilter", condition = "business_id = :businessId")
public class ProductType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String icon;

    // Categoría padre (opcional). La jerarquía nunca puede tener ciclos.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = true)
    private ProductType parent;

    // Identificador estable dentro del negocio (se deriva del nombre al crear).
    @Column(length = 120)
    private String slug;

    // El nombre de la categoria es unico dentro del negocio (V5)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public ProductType getParent() {
        return parent;
    }

    public void setParent(ProductType parent) {
        this.parent = parent;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }
}
