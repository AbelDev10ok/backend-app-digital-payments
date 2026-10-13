package com.app.digital.payments.digital_pyments.models;

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

import org.hibernate.annotations.Filter;

@Entity
@Table(name = "subscription")
@Filter(name = "tenantFilter", condition = "business_id = :businessId")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false, length = 20)
    private String status = "TRIALING";

    @Column(nullable = false)
    private LocalDate currentPeriodStart;

    @Column(nullable = false)
    private LocalDate currentPeriodEnd;

    @Column(length = 100)
    private String mpPreapprovalId;

    @Column(length = 255)
    private String mpInitPoint;

    /**
     * Ultimo intento de cobro de renovacion rechazado. Null cuando el ultimo
     * cobro fue aprobado (o nunca hubo intentos). No afecta al acceso: el
     * periodo pagado sigue vigente hasta currentPeriodEnd; solo se usa para
     * avisar al usuario que arregle su medio de pago.
     */
    private LocalDateTime lastPaymentFailedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Subscription() {
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Subscription other)) return false;
        return id != null && id.equals(other.id);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Business getBusiness() { return business; }
    public void setBusiness(Business business) { this.business = business; }
    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getCurrentPeriodStart() { return currentPeriodStart; }
    public void setCurrentPeriodStart(LocalDate currentPeriodStart) { this.currentPeriodStart = currentPeriodStart; }
    public LocalDate getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(LocalDate currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }
    public String getMpPreapprovalId() { return mpPreapprovalId; }
    public void setMpPreapprovalId(String mpPreapprovalId) { this.mpPreapprovalId = mpPreapprovalId; }
    public String getMpInitPoint() { return mpInitPoint; }
    public void setMpInitPoint(String mpInitPoint) { this.mpInitPoint = mpInitPoint; }

    public LocalDateTime getLastPaymentFailedAt() { return lastPaymentFailedAt; }
    public void setLastPaymentFailedAt(LocalDateTime lastPaymentFailedAt) { this.lastPaymentFailedAt = lastPaymentFailedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}