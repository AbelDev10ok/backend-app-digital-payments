package com.app.digital.payments.digital_pyments.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Negocio (tenant) del SaaS. Cada usuario pertenece a un negocio y todos los
 * datos del dominio (clientes, ventas, cuotas, categorias) le corresponden.
 */
@Entity
@Table(name = "business")
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Moneda de referencia del negocio (ISO 4217). Configuracion futura.
    @Column(nullable = false)
    private String currency = "ARS";

    private LocalDateTime createdAt;

    // Tasa de interes por defecto para prestamos (nullable = sin default)
    @Column(precision = 12, scale = 2)
    private BigDecimal defaultInterestRate;

    // Frecuencia de cuota por defecto: SEMANAL, QUINCENAL, MENSUAL, CONTADO
    @Column(length = 20)
    private String defaultPaymentFrequency;

    // Estado de suscripcion: TRIAL o ACTIVE (el cobro real llega con Fase 12)
    @Column(nullable = false, length = 20)
    private String planStatus = "TRIAL";

    // Fin del periodo de prueba
    private LocalDate trialEndsAt;

    // Suscripcion pagada hasta (Fase 12 con MercadoPago)
    private LocalDate subscriptionUntil;

    public Business() {
    }

    public Business(String name, String currency) {
        this.name = name;
        this.currency = currency;
        this.createdAt = LocalDateTime.now();
    }

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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getDefaultInterestRate() {
        return defaultInterestRate;
    }

    public void setDefaultInterestRate(BigDecimal defaultInterestRate) {
        this.defaultInterestRate = defaultInterestRate;
    }

    public String getDefaultPaymentFrequency() {
        return defaultPaymentFrequency;
    }

    public void setDefaultPaymentFrequency(String defaultPaymentFrequency) {
        this.defaultPaymentFrequency = defaultPaymentFrequency;
    }

    public String getPlanStatus() {
        return planStatus;
    }

    public void setPlanStatus(String planStatus) {
        this.planStatus = planStatus;
    }

    public LocalDate getTrialEndsAt() {
        return trialEndsAt;
    }

    public void setTrialEndsAt(LocalDate trialEndsAt) {
        this.trialEndsAt = trialEndsAt;
    }

    public LocalDate getSubscriptionUntil() {
        return subscriptionUntil;
    }

    public void setSubscriptionUntil(LocalDate subscriptionUntil) {
        this.subscriptionUntil = subscriptionUntil;
    }
}
