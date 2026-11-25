package com.app.digital.payments.digital_pyments.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.Filter;

import com.app.digital.payments.digital_pyments.utils.PromiseStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Promesa de pago: el cliente se compromete a pagar un monto de su deuda en una
 * fecha. Es una gestión comercial, no un pago: no toca saldos ni cuotas.
 *
 * Se registra por cliente + moneda (una promesa nunca mezcla monedas) y se
 * cierra de una sola forma: se cumple (un cobro cubre el monto), se cancela a
 * mano o queda vencida si la fecha pasó sin cobro. El estado guardado es
 * PENDING, FULFILLED o CANCELLED; "vencida" se deriva por fecha al leer.
 */
@Entity
@Table(name = "payment_promise")
@Filter(name = "tenantFilter", condition = "business_id = :businessId")
public class PaymentPromise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Negocio (tenant) al que pertenece la promesa (el mismo del cliente)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    // Moneda ISO 4217 del compromiso: nunca se suman ni se mezclan monedas
    @Column(nullable = false, length = 3)
    private String currency;

    // Monto que el cliente prometió pagar (puede ser parcial sobre su deuda)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "promised_date", nullable = false)
    private LocalDate promisedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromiseStatus status = PromiseStatus.PENDING;

    // Motivo o comentario de la gestión (opcional, lo escribe el cobrador)
    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Cuándo se cerró la promesa (cumplida o cancelada). Null mientras está pendiente.
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public PaymentPromise() {
    }

    public PaymentPromise(Business business, Client client, String currency, BigDecimal amount,
            LocalDate promisedDate, String note, LocalDateTime createdAt) {
        this.business = business;
        this.client = client;
        this.currency = currency;
        this.amount = amount;
        this.promisedDate = promisedDate;
        this.note = note;
        this.createdAt = createdAt;
        this.status = PromiseStatus.PENDING;
    }

    /**
     * ¿Sigue pendiente y su fecha ya pasó? Se deriva al leer: no se guarda,
     * para no necesitar un proceso que marque promesas vencidas.
     */
    public boolean estaVencida(LocalDate hoy) {
        return status == PromiseStatus.PENDING && promisedDate != null && promisedDate.isBefore(hoy);
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

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getPromisedDate() {
        return promisedDate;
    }

    public void setPromisedDate(LocalDate promisedDate) {
        this.promisedDate = promisedDate;
    }

    public PromiseStatus getStatus() {
        return status;
    }

    public void setStatus(PromiseStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
