package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SubscriptionStatusDto {

    private Long id;
    private String status;
    private PlanDto plan;
    private LocalDate currentPeriodStart;
    private LocalDate currentPeriodEnd;
    private String mpPreapprovalId;
    private String mpInitPoint;
    private LocalDateTime lastPaymentFailedAt;
    private LocalDateTime createdAt;
    private LocalDate trialEndsAt;
    private String businessPlanStatus;

    public SubscriptionStatusDto() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public PlanDto getPlan() { return plan; }
    public void setPlan(PlanDto plan) { this.plan = plan; }
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
    public LocalDate getTrialEndsAt() { return trialEndsAt; }
    public void setTrialEndsAt(LocalDate trialEndsAt) { this.trialEndsAt = trialEndsAt; }
    public String getBusinessPlanStatus() { return businessPlanStatus; }
    public void setBusinessPlanStatus(String businessPlanStatus) { this.businessPlanStatus = businessPlanStatus; }
}