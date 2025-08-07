package com.app.digital.payments.digital_pyments.models.dtos;

import java.time.LocalDate;
import java.util.List;

import com.app.digital.payments.digital_pyments.utils.Payments;


public class SaleResponseDto {
    private Long id;
    private ClientDto client;
    private String descriptionProduct;
    private Double priceTotal;
    private LocalDate dateSale;
    private LocalDate finalPaymentDate; // Fecha final de pago
    private Payments typePayments;
    private Integer daysLate = 0; 
    private Integer quantityFees;
    private boolean completed; // Si está completamente pagada
    private Double amountFe;
    private List<FeeDto> fees;
    private Double cost;
    
    private Integer paidFeesCount;  // Cuotas pagadas
 
    // private Integer additionalFees = 0; // Cuotas extras agregadas
    
    private Double remainingAmount;
    private Integer totalFees; // Total de cuotas (originales + extras)





    public SaleResponseDto() {
    }

    public SaleResponseDto(Long id, ClientDto client, String descriptionProduct, Double priceTotal,
                           LocalDate dateSale, LocalDate finalPaymentDate, Payments typePayments,
                           Integer daysLate, Integer quantityFees, boolean completed, Double amountFe,
                           List<FeeDto> fees, Integer paidFeesCount, Double remainingAmount, Integer totalFees) {
        this.id = id;
        this.client = client;
        this.descriptionProduct = descriptionProduct;
        this.priceTotal = priceTotal;
        this.dateSale = dateSale;
        this.finalPaymentDate = finalPaymentDate;
        this.typePayments = typePayments;
        this.daysLate = daysLate;
        this.quantityFees = quantityFees;
        this.completed = completed;
        this.amountFe = amountFe;
        this.fees = fees;
        this.paidFeesCount = paidFeesCount;
        this.remainingAmount = remainingAmount;
        this.totalFees = totalFees;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClientDto getClient() {
        return client;
    }

    public void setClient(ClientDto client) {
        this.client = client;
    }

    public String getDescriptionProduct() {
        return descriptionProduct;
    }

    public void setDescriptionProduct(String descriptionProduct) {
        this.descriptionProduct = descriptionProduct;
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

    public LocalDate getFinalPaymentDate() {
        return finalPaymentDate;
    }

    public void setFinalPaymentDate(LocalDate finalPaymentDate) {
        this.finalPaymentDate = finalPaymentDate;
    }

    public Payments getTypePayments() {
        return typePayments;
    }

    public void setTypePayments(Payments typePayments) {
        this.typePayments = typePayments;
    }

    public Integer getDaysLate() {
        return daysLate;
    }

    public void setDaysLate(Integer daysLate) {
        this.daysLate = daysLate;
    }

    public Integer getQuantityFees() {
        return quantityFees;
    }

    public void setQuantityFees(Integer quantityFees) {
        this.quantityFees = quantityFees;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Double getAmountFe() {
        return amountFe;
    }

    public void setAmountFe(Double amountFe) {
        this.amountFe = amountFe;
    }

    public List<FeeDto> getFees() {
        return fees;
    }

    public void setFees(List<FeeDto> fees) {
        this.fees = fees;
    }


    public Integer getPaidFeesCount() {
        return paidFeesCount;
    }


    public void setPaidFeesCount(Integer paidFeesCount) {
        this.paidFeesCount = paidFeesCount;
    }


    public Double getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(Double remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public Integer getTotalFees() {
        return totalFees;
    }

    public void setTotalFees(Integer totalFees) {
        this.totalFees = totalFees;
    }
    
    public Double getCost() {
        return cost;
    }
    public void setCost(Double cost) {
        this.cost = cost;
    }
}
