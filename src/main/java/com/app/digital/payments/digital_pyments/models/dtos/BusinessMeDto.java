package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Vista del negocio propio (tenant autenticado) para la pantalla
 * "Mi Negocio". Incluye los defaults de prestamos y el estado del plan.
 */
public class BusinessMeDto {

    private Long id;
    private String name;
    private String cuit;
    private String address;
    private String phone;
    private String currency;
    private BigDecimal defaultInterestRate;
    private String defaultPaymentFrequency;
    private String planStatus;
    private LocalDate trialEndsAt;
    private LocalDate subscriptionUntil;
    private LocalDate createdAt;

    public BusinessMeDto() {
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

    public String getCuit() {
        return cuit;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
}
