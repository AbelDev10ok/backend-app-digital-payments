package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado del cobro por cliente: un monto distribuido entre las operaciones
 * del cliente con deuda en la misma moneda. Los montos son siempre de una sola
 * moneda: nunca se suman monedas distintas.
 */
public class ClientPaymentResultDto {

    private Long clientId;
    private String clientName;
    private String currency;
    private BigDecimal appliedAmount;
    private BigDecimal remainingDebt;
    private List<Application> applications = new ArrayList<>();

    /**
     * Detalle del cobro sobre una operacion del cliente.
     */
    public static class Application {

        private Long saleId;
        private String saleDescription;
        private String saleKind; // VENTA | PRESTAMO
        private BigDecimal appliedAmount;
        private BigDecimal saleRemainingAmount;
        private Integer feesPaid;
        private Boolean saleCompleted;

        public Long getSaleId() {
            return saleId;
        }

        public void setSaleId(Long saleId) {
            this.saleId = saleId;
        }

        public String getSaleDescription() {
            return saleDescription;
        }

        public void setSaleDescription(String saleDescription) {
            this.saleDescription = saleDescription;
        }

        public String getSaleKind() {
            return saleKind;
        }

        public void setSaleKind(String saleKind) {
            this.saleKind = saleKind;
        }

        public BigDecimal getAppliedAmount() {
            return appliedAmount;
        }

        public void setAppliedAmount(BigDecimal appliedAmount) {
            this.appliedAmount = appliedAmount;
        }

        public BigDecimal getSaleRemainingAmount() {
            return saleRemainingAmount;
        }

        public void setSaleRemainingAmount(BigDecimal saleRemainingAmount) {
            this.saleRemainingAmount = saleRemainingAmount;
        }

        public Integer getFeesPaid() {
            return feesPaid;
        }

        public void setFeesPaid(Integer feesPaid) {
            this.feesPaid = feesPaid;
        }

        public Boolean getSaleCompleted() {
            return saleCompleted;
        }

        public void setSaleCompleted(Boolean saleCompleted) {
            this.saleCompleted = saleCompleted;
        }
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

    public BigDecimal getAppliedAmount() {
        return appliedAmount;
    }

    public void setAppliedAmount(BigDecimal appliedAmount) {
        this.appliedAmount = appliedAmount;
    }

    public BigDecimal getRemainingDebt() {
        return remainingDebt;
    }

    public void setRemainingDebt(BigDecimal remainingDebt) {
        this.remainingDebt = remainingDebt;
    }

    public List<Application> getApplications() {
        return applications;
    }

    public void setApplications(List<Application> applications) {
        this.applications = applications;
    }
}
