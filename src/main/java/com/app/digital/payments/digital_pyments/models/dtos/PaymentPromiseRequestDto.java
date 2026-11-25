package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alta de una promesa de pago: el cliente se compromete a pagar un monto de su
 * deuda en una moneda y una fecha. El negocio y el cliente salen del contexto.
 */
public class PaymentPromiseRequestDto {

    // Moneda ISO 4217 del compromiso (obligatoria)
    private String currency;

    // Monto prometido (obligatorio, mayor a cero)
    private BigDecimal amount;

    // Fecha prometida (obligatoria, hoy o posterior)
    private LocalDate promisedDate;

    // Comentario de la gestión (opcional)
    private String note;

    public PaymentPromiseRequestDto() {
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
