package com.app.digital.payments.digital_pyments.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.app.digital.payments.digital_pyments.utils.Payments;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "sale")  
public class Sale {

    @Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @Column(nullable = false)
    private String descriptiononProduct;
    
    @Column(nullable = false)
    private Double priceTotal;
    
    @Column(nullable = false)
    private LocalDate dateSale;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Payments typePayments;
    
    @Column(nullable = false)
    private Integer quiantityFees; // Cantidad de cuotas originales
    
    @Column(nullable = false)
    private Integer additionalFees = 0; // Cuotas extras agregadas
    
    @Column(nullable = false)
    private Double remainingAmount= priceTotal;

    @Column(nullable = false)
    private Double amountFee;
    
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Fee> fees = new ArrayList<>();

    private LocalDate finalPaymentDate; 
    
    private LocalDate realFinalDate; 
    
    private Integer daysLate = 0; 

    private boolean completed; // Si está completamente pagada


    public Sale() {
    }

    public Sale(Client client, String descriptiononProduct, Double priceTotal, LocalDate dateSale,
                Payments typePayments, Integer quiantityFees, Double amountFee) {
        this.client = client;
        this.descriptiononProduct = descriptiononProduct;
        this.priceTotal = priceTotal;
        this.dateSale = dateSale;
        this.typePayments = typePayments;
        this.quiantityFees = quiantityFees;
        this.amountFee = amountFee;
        this.remainingAmount = priceTotal; // Inicialmente el monto restante es el total
    }


    // Método helper para actualizar el saldo
    public void deductFromRemainingAmount(Double amount) {
        this.remainingAmount = Math.max(0, this.remainingAmount - amount);
        if(this.remainingAmount <= 0) {
            this.completed = true;
        }
    }

    // Mantenemos quantityFees como campo calculado
    public Integer getTotalFees() {
        return quiantityFees + additionalFees;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getDescriptiononProduct() {
        return descriptiononProduct;
    }

    public void setDescriptiononProduct(String descriptiononProduct) {
        this.descriptiononProduct = descriptiononProduct;
    }

    public Double getPriceTotal() {
        return priceTotal;
    }

    public void setPriceTotal(Double priceTotal) {
        this.priceTotal = priceTotal;
    }

    public LocalDate getDateSale() {
        return dateSale;
    }

    public void setDateSale(LocalDate dateSale) {
        this.dateSale = dateSale;
    }

    public Payments getTypePayments() {
        return typePayments;
    }

    public void setTypePayments(Payments typePayments) {
        this.typePayments = typePayments;
    }

    public Integer getQuiantityFees() {
        return quiantityFees;
    }

    public void setQuiantityFees(Integer quiantityFees) {
        this.quiantityFees = quiantityFees;
    }

    public Double getAmountFe() {
        return amountFee;
    }

    public void setAmountFe(Double amountFee) {
        this.amountFee = amountFee;
    }

    public List<Fee> getFees() {
        return fees;
    }

    public void setFees(List<Fee> fees) {
        this.fees = fees;
    }

    public Double getAmountFee() {
        return amountFee;
    }

    public void setAmountFee(Double amountFee) {
        this.amountFee = amountFee;
    }

    public LocalDate getFinalPaymentDate() {
        return finalPaymentDate;
    }

    public void setFinalPaymentDate(LocalDate finalPaymentDate) {
        this.finalPaymentDate = finalPaymentDate;
    }

    public LocalDate getRealFinalDate() {
        return realFinalDate;
    }

    public void setRealFinalDate(LocalDate realFinalDate) {
        this.realFinalDate = realFinalDate;
    }

    public Integer getDaysLate() {
        return daysLate;
    }


    public void setDaysLate(Integer daysLate) {
        this.daysLate = daysLate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Integer getAdditionalFees() {
        return additionalFees;
    }

    public void setAdditionalFees(Integer additionalFees) {
        this.additionalFees = additionalFees;
    }

    public Double getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(Double remainingAmount) {
        this.remainingAmount = remainingAmount;
    }
 

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((client == null) ? 0 : client.hashCode());
        result = prime * result + ((descriptiononProduct == null) ? 0 : descriptiononProduct.hashCode());
        result = prime * result + ((priceTotal == null) ? 0 : priceTotal.hashCode());
        result = prime * result + ((dateSale == null) ? 0 : dateSale.hashCode());
        result = prime * result + ((typePayments == null) ? 0 : typePayments.hashCode());
        result = prime * result + ((quiantityFees == null) ? 0 : quiantityFees.hashCode());
        result = prime * result + ((additionalFees == null) ? 0 : additionalFees.hashCode());
        result = prime * result + ((remainingAmount == null) ? 0 : remainingAmount.hashCode());
        result = prime * result + ((amountFee == null) ? 0 : amountFee.hashCode());
        result = prime * result + ((fees == null) ? 0 : fees.hashCode());
        result = prime * result + ((finalPaymentDate == null) ? 0 : finalPaymentDate.hashCode());
        result = prime * result + ((realFinalDate == null) ? 0 : realFinalDate.hashCode());
        result = prime * result + ((daysLate == null) ? 0 : daysLate.hashCode());
        result = prime * result + (completed ? 1231 : 1237);
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
        Sale other = (Sale) obj;
        if (client == null) {
            if (other.client != null)
                return false;
        } else if (!client.equals(other.client))
            return false;
        if (descriptiononProduct == null) {
            if (other.descriptiononProduct != null)
                return false;
        } else if (!descriptiononProduct.equals(other.descriptiononProduct))
            return false;
        if (priceTotal == null) {
            if (other.priceTotal != null)
                return false;
        } else if (!priceTotal.equals(other.priceTotal))
            return false;
        if (dateSale == null) {
            if (other.dateSale != null)
                return false;
        } else if (!dateSale.equals(other.dateSale))
            return false;
        if (typePayments != other.typePayments)
            return false;
        if (quiantityFees == null) {
            if (other.quiantityFees != null)
                return false;
        } else if (!quiantityFees.equals(other.quiantityFees))
            return false;
        if (additionalFees == null) {
            if (other.additionalFees != null)
                return false;
        } else if (!additionalFees.equals(other.additionalFees))
            return false;
        if (remainingAmount == null) {
            if (other.remainingAmount != null)
                return false;
        } else if (!remainingAmount.equals(other.remainingAmount))
            return false;
        if (amountFee == null) {
            if (other.amountFee != null)
                return false;
        } else if (!amountFee.equals(other.amountFee))
            return false;
        if (fees == null) {
            if (other.fees != null)
                return false;
        } else if (!fees.equals(other.fees))
            return false;
        if (finalPaymentDate == null) {
            if (other.finalPaymentDate != null)
                return false;
        } else if (!finalPaymentDate.equals(other.finalPaymentDate))
            return false;
        if (realFinalDate == null) {
            if (other.realFinalDate != null)
                return false;
        } else if (!realFinalDate.equals(other.realFinalDate))
            return false;
        if (daysLate == null) {
            if (other.daysLate != null)
                return false;
        } else if (!daysLate.equals(other.daysLate))
            return false;
        if (completed != other.completed)
            return false;
        return true;
    }
    
    
    
}
