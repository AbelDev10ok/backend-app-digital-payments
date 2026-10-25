package com.app.digital.payments.digital_pyments.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

import org.hibernate.annotations.Filter;

import com.app.digital.payments.digital_pyments.utils.PaymentMethod;

/**
 * Registro de un cobro real (el "log" de pagos).
 *
 * Una cuota puede tener varios pagos: cobros parciales, o varios cobros el
 * mismo dia (cada uno con su horario). Este es el registro autoritativo del
 * dinero que entro: la cuota solo guarda el acumulado ({@code paidAmount}) para
 * poder filtrar y listar sin traer la coleccion completa.
 *
 * Nunca se modifica el monto programado de la cuota al cobrar.
 */
@Entity
@Table(name = "payment")
@Filter(name = "tenantFilter", condition = "business_id = :businessId")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Negocio (tenant) al que pertenece el pago (el mismo de su cuota)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_id", nullable = false)
    private Fee fee;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Fecha y hora del cobro. El backend sella el horario al registrar; si el
     * cobrador backdatea la fecha, se guarda la hora real del registro.
     */
    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    public Payment() {
    }

    public Payment(Fee fee, BigDecimal amount, LocalDateTime paidAt, PaymentMethod paymentMethod) {
        this.fee = fee;
        this.amount = amount;
        this.paidAt = paidAt;
        this.paymentMethod = paymentMethod;
        this.business = fee != null ? fee.getBusiness() : null;
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

    public Fee getFee() {
        return fee;
    }

    public void setFee(Fee fee) {
        this.fee = fee;
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
