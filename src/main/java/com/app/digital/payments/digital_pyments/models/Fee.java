package com.app.digital.payments.digital_pyments.models;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table  (name = "fee")
public class Fee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;
    
    @Column(nullable = false)
    private Integer numberFee;
    
    private Double amount;
    
    @Column(nullable = false)
    private LocalDate expirationDate;

    @Column(nullable = false)
    private Boolean paid = false;

    @Column(nullable = false)
    private boolean additional = false; 
    
    private LocalDate datePayment;
    

    public Fee() {
    }

    public Fee(Sale venta, Integer numberFee, Double amount, LocalDate expirationDate) {
        this.sale = venta;
        this.numberFee = numberFee;
        this.amount = amount;
        this.expirationDate = expirationDate;
    }

    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public Integer getNumberFee() {
        return numberFee;
    }

    public void setNumberFee(Integer numberFee) {
        this.numberFee = numberFee;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }

    public LocalDate getDatePayment() {
        return datePayment;
    }

    public void setDatePayment(LocalDate datePayment) {
        this.datePayment = datePayment;
    }

    public boolean isAdditional() {
        return additional;
    }

    public void setAdditional(boolean additional) {
        this.additional = additional;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((sale == null) ? 0 : sale.hashCode());
        result = prime * result + ((numberFee == null) ? 0 : numberFee.hashCode());
        result = prime * result + ((amount == null) ? 0 : amount.hashCode());
        result = prime * result + ((expirationDate == null) ? 0 : expirationDate.hashCode());
        result = prime * result + ((paid == null) ? 0 : paid.hashCode());
        result = prime * result + ((datePayment == null) ? 0 : datePayment.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Fee other = (Fee) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (sale == null) {
            if (other.sale != null)
                return false;
        } else if (!sale.equals(other.sale))
            return false;
        if (numberFee == null) {
            if (other.numberFee != null)
                return false;
        } else if (!numberFee.equals(other.numberFee))
            return false;
        if (amount == null) {
            if (other.amount != null)
                return false;
        } else if (!amount.equals(other.amount))
            return false;
        if (expirationDate == null) {
            if (other.expirationDate != null)
                return false;
        } else if (!expirationDate.equals(other.expirationDate))
            return false;
        if (paid == null) {
            if (other.paid != null)
                return false;
        } else if (!paid.equals(other.paid))
            return false;
        if (datePayment == null) {
            if (other.datePayment != null)
                return false;
        } else if (!datePayment.equals(other.datePayment))
            return false;
        return true;
    }


    
}
