package com.app.digital.payments.digital_pyments.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Parada de la ronda: un cliente con deuda en una moneda, con el monto
 * planificado y el compromiso que tenia al armarse el plan.
 *
 * <p>
 * El cliente se guarda por ID y con datos snapshot (nombre, telefono,
 * direccion): la ronda es un documento del dia, no una vista en vivo. La
 * promesa tambien se copia (id, fecha y monto) para no depender de que siga
 * pendiente.
 * </p>
 */
@Entity
@Table(name = "collection_round_item")
public class CollectionRoundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private CollectionRound round;

    // Orden de visita dentro de la ronda (1 = primero)
    @Column(nullable = false)
    private Integer position;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "client_name", length = 200)
    private String clientName;

    @Column(name = "client_phone", length = 40)
    private String clientPhone;

    @Column(name = "client_address", length = 255)
    private String clientAddress;

    @Column(nullable = false, length = 3)
    private String currency;

    // Monto cobrable planificado: suma de las cuotas impagas del grupo, con el
    // tope de la deuda de cada operacion. Es el plan, no el saldo vivo.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // Mayor atraso entre las cuotas del grupo (en dias) al armar la ronda
    @Column(name = "days_overdue", nullable = false)
    private Long daysOverdue;

    // Snapshot de la promesa pendiente del cliente en esta moneda, si habia
    @Column(name = "promise_id")
    private Long promiseId;

    @Column(name = "promise_date")
    private LocalDate promiseDate;

    @Column(name = "promise_amount", precision = 12, scale = 2)
    private BigDecimal promiseAmount;

    // Unica marca que aporta el cobrador: visito y no estaba
    @Column(name = "no_estaba", nullable = false)
    private Boolean noEstaba = Boolean.FALSE;

    @Column(length = 500)
    private String note;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CollectionRoundItem() {
    }

    public CollectionRoundItem(CollectionRound round, Integer position, Long clientId, String clientName,
            String clientPhone, String clientAddress, String currency, BigDecimal amount, Long daysOverdue,
            PaymentPromise promise) {
        this.round = round;
        this.position = position;
        this.clientId = clientId;
        this.clientName = clientName;
        this.clientPhone = clientPhone;
        this.clientAddress = clientAddress;
        this.currency = currency;
        this.amount = amount;
        this.daysOverdue = daysOverdue;
        if (promise != null) {
            this.promiseId = promise.getId();
            this.promiseDate = promise.getPromisedDate();
            this.promiseAmount = promise.getAmount();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CollectionRound getRound() {
        return round;
    }

    public void setRound(CollectionRound round) {
        this.round = round;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientPhone() {
        return clientPhone;
    }

    public void setClientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
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

    public Long getDaysOverdue() {
        return daysOverdue;
    }

    public void setDaysOverdue(Long daysOverdue) {
        this.daysOverdue = daysOverdue;
    }

    public Long getPromiseId() {
        return promiseId;
    }

    public void setPromiseId(Long promiseId) {
        this.promiseId = promiseId;
    }

    public LocalDate getPromiseDate() {
        return promiseDate;
    }

    public void setPromiseDate(LocalDate promiseDate) {
        this.promiseDate = promiseDate;
    }

    public BigDecimal getPromiseAmount() {
        return promiseAmount;
    }

    public void setPromiseAmount(BigDecimal promiseAmount) {
        this.promiseAmount = promiseAmount;
    }

    public Boolean getNoEstaba() {
        return noEstaba;
    }

    public void setNoEstaba(Boolean noEstaba) {
        this.noEstaba = noEstaba;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
