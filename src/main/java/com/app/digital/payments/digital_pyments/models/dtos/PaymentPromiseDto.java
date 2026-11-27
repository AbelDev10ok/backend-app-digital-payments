package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.app.digital.payments.digital_pyments.utils.PromiseStatus;

/**
 * Promesa de pago tal como la consume la UI: el estado guardado más la
 * condición derivada de vencida (fecha prometida pasada y sin cobro).
 */
public class PaymentPromiseDto {

    private Long id;
    private Long clientId;
    private String clientName;
    private String currency;
    private BigDecimal amount;
    private LocalDate promisedDate;
    private PromiseStatus status;
    // Derivado: pendiente con fecha prometida ya pasada
    private boolean vencida;
    private Long diasAtraso;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public PaymentPromiseDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isVencida() {
        return vencida;
    }

    public void setVencida(boolean vencida) {
        this.vencida = vencida;
    }

    public Long getDiasAtraso() {
        return diasAtraso;
    }

    public void setDiasAtraso(Long diasAtraso) {
        this.diasAtraso = diasAtraso;
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
