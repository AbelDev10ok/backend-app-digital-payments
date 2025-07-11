package com.app.digital.payments.digital_pyments.models.dtos;

import java.time.LocalDate;

public class PaidFeeDto {
    
    private Double amount;
    private LocalDate datePayment;
    
    public PaidFeeDto() {
    }
    
    public Double getAmount() {
        return amount;
    }
    public void setAmount(Double amount) {
        this.amount = amount;
    }
    public LocalDate getDatePayment() {
        return datePayment;
    }
    public void setDatePayment(LocalDate datePayment) {
        this.datePayment = datePayment;
    }
    
}
