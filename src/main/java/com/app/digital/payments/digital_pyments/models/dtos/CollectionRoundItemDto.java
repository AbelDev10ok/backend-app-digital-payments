package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Parada de la ronda del dia. {@code amount} es el monto planificado al armar
 * la ronda; {@code pendingAmount} es lo que el cliente sigue debiendo hoy
 * (null si ya se cobro). El estado se deriva: COBRADO si salio de la agenda,
 * NO_ESTABA si el cobrador lo marco, PENDIENTE en el resto.
 */
public class CollectionRoundItemDto {

    private Long id;
    private Integer position;
    private Long clientId;
    private String clientName;
    private String clientPhone;
    private String clientAddress;
    private String currency;
    private BigDecimal amount;
    private BigDecimal pendingAmount;
    private Long daysOverdue;
    private String estado;
    private Long promiseId;
    private LocalDate promiseDate;
    private BigDecimal promiseAmount;
    private boolean promiseOverdue;
    private String note;

    public CollectionRoundItemDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(BigDecimal pendingAmount) {
        this.pendingAmount = pendingAmount;
    }

    public Long getDaysOverdue() {
        return daysOverdue;
    }

    public void setDaysOverdue(Long daysOverdue) {
        this.daysOverdue = daysOverdue;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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

    public boolean isPromiseOverdue() {
        return promiseOverdue;
    }

    public void setPromiseOverdue(boolean promiseOverdue) {
        this.promiseOverdue = promiseOverdue;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
