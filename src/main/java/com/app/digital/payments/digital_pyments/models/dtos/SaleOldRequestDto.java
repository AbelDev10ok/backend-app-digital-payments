package com.app.digital.payments.digital_pyments.models.dtos;

import java.time.LocalDate;
import java.util.List;

import com.app.digital.payments.digital_pyments.utils.IValueOfEnum;
import com.app.digital.payments.digital_pyments.utils.Payments;

import io.micrometer.common.lang.NonNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SaleOldRequestDto {
    
    @NonNull
    private Long clientId;
    @NotBlank(message = "La descripción del producto no puede estar vacía")
    private String descriptionProduct;

    // El precio debe ser mayor a 0
    @NotNull(message = "El precio total no puede estar vacío")
    @Min(value = 1, message = "El precio total debe ser mayor a 0")
    private Double priceTotal;
    
    // El tipo de pago debe ser uno de los valores definidos en Payments
    @IValueOfEnum(enumClass = Payments.class, message = "El tipo de pago debe ser SEMANAL, QUINCENAL o MENSUAL")
    private String payments;
    @Min(value = 1, message = "La cantidad de cuotas debe ser mayor a 0")
    private Integer quantityFees;
    private LocalDate dateSale;

    private List<PaidFeeDto> paidFees; // Lista de cuotas pagadas

    public SaleOldRequestDto() {
        
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
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

    public String getPayments() {
        return payments;
    }

    public void setPayments(String payments) {
        this.payments = payments;
    }

    public Integer getQuantityFees() {
        return quantityFees;
    }

    public void setQuantityFees(Integer quantityFees) {
        this.quantityFees = quantityFees;
    }

    public LocalDate getDateSale() {
        return dateSale;
    }

    public void setDateSale(LocalDate dateSale) {
        this.dateSale = dateSale;
    }

    public List<PaidFeeDto> getPaidFees() {
        return paidFees;
    }

    public void setPaidFees(List<PaidFeeDto> paidFees) {
        this.paidFees = paidFees;
    }

    
}
