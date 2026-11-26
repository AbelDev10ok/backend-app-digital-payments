package com.app.digital.payments.digital_pyments.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Filter;

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

/**
 * Ronda de cobro de un negocio para un dia: la hoja de ruta que se arma una
 * sola vez (por el job diario o por la primera consulta) y se conserva como
 * plan del dia. Reconstruirla pisaria el trabajo ya hecho, asi que es
 * idempotente por (negocio, fecha).
 *
 * <p>
 * Los items son un snapshot: guardan nombre, telefono y direccion del cliente
 * al momento de armar la ronda. Editar el cliente no cambia la ronda ya
 * armada.
 * </p>
 */
@Entity
@Table(name = "collection_round", uniqueConstraints = {
        @UniqueConstraint(name = "uk_collection_round_business_date", columnNames = { "business_id", "round_date" })
})
@Filter(name = "tenantFilter", condition = "business_id = :businessId")
public class CollectionRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "round_date", nullable = false)
    private LocalDate roundDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<CollectionRoundItem> items = new ArrayList<>();

    public CollectionRound() {
    }

    public CollectionRound(Business business, LocalDate roundDate, LocalDateTime createdAt) {
        this.business = business;
        this.roundDate = roundDate;
        this.createdAt = createdAt;
    }

    public void agregarItem(CollectionRoundItem item) {
        item.setRound(this);
        items.add(item);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public LocalDate getRoundDate() {
        return roundDate;
    }

    public void setRoundDate(LocalDate roundDate) {
        this.roundDate = roundDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<CollectionRoundItem> getItems() {
        return items;
    }

    public void setItems(List<CollectionRoundItem> items) {
        this.items = items;
    }
}
