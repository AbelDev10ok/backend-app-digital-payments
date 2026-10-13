package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PaidFeeDto {
    
    private BigDecimal amount;
    private LocalDate datePayment;
    
    public PaidFeeDto() {
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    public LocalDate getDatePayment() {
        return datePayment;
    }
    public void setDatePayment(LocalDate datePayment) {
        this.datePayment = datePayment;
    }
    
}
