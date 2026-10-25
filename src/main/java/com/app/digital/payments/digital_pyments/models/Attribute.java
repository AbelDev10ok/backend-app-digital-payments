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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Filter;

/**
 * Atributo del catálogo de un negocio (por ejemplo "Color" o "Talle").
 *
 * Los atributos son propios de cada negocio y sus valores posibles viven en
 * {@link AttributeValue}. Un producto declara qué atributos generan variantes.
 */
@Entity
@Table(name = "attribute", uniqueConstraints = {
        @UniqueConstraint(name = "uk_attribute_business_name", columnNames = { "business_id", "name" })
})
@Filter(name = "tenantFilter", condition = "business_id = :businessId")
public class Attribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @OneToMany(mappedBy = "attribute", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<AttributeValue> values = new ArrayList<>();

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

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public List<AttributeValue> getValues() {
        return values;
    }

    public void setValues(List<AttributeValue> values) {
        this.values = values;
    }
}
