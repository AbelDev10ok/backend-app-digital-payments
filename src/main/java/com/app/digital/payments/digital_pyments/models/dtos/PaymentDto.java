package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un cobro registrado sobre una cuota (con fecha y hora). */
public class PaymentDto {

    private Long id;
    private BigDecimal amount;
    private LocalDateTime paidAt;
    private String paymentMethod; // EFECTIVO | TRANSFERENCIA

    public PaymentDto() {
    }

    public PaymentDto(Long id, BigDecimal amount, LocalDateTime paidAt, String paymentMethod) {
        this.id = id;
        this.amount = amount;
        this.paidAt = paidAt;
        this.paymentMethod = paymentMethod;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
