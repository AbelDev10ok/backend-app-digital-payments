package com.app.digital.payments.digital_pyments.models.dtos;

import java.time.LocalDate;
import java.util.List;

import com.app.digital.payments.digital_pyments.utils.IValueOfEnum;
import com.app.digital.payments.digital_pyments.utils.Payments;

import io.micrometer.common.lang.NonNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SaleRequestDto {

    @NonNull
    private Long clientId;
    @NotBlank(message = "La descripción del producto no puede estar vacía")
    private String descriptionProduct;

    // El precio debe ser mayor a 0
    // @NotNull(message = "El precio total no puede estar vacío")
    // @Min(value = 1, message = "El precio total debe ser mayor a 0")
    // private Double priceTotal;
    
    // El tipo de pago debe ser uno de los valores definidos en Payments
    @IValueOfEnum(enumClass = Payments.class, message = "El tipo de pago debe ser SEMANAL, QUINCENAL o MENSUAL")
    private String payments;

    @Min(value = 1, message = "La cantidad de cuotas debe ser mayor a 0")
    private Integer quantityFees;

    @NotNull(message = "El valor de la cuota no puede estar vacío")
    private Double amountFee;

    @NotNull(message = "El costo no puede estar vacío")
    private Double cost; // Costo del producto/servicio

    // en caso de que sea una venta vieja
    private LocalDate dateSale;

    private LocalDate firstFeeDate;

    private Boolean payFirstFee;

    private Double firstFeeAmount; // Monto de la primera cuota, puede ser null si no se paga
    

    private List<PaidFeeDto> paidFees; // Lista de cuotas pagadas

    public SaleRequestDto() {
    
    }
    
    public Long getClienteId() {
        return clientId;
    }

    public void setClienteId(Long clientId) {
        this.clientId = clientId;
    }

    public String getDescripcionProducto() {
        return descriptionProduct;
    }

    public void setDescripcionProducto(String descriptionProduct) {
        this.descriptionProduct = descriptionProduct;
    }


    public Integer getCantidadCuotas() {
        return quantityFees;
    }

    public void setCantidadCuotas(Integer quantityFees) {
        this.quantityFees = quantityFees;
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

    // public Double getPriceTotal() {
    //     return priceTotal;
    // }

    // public void setPriceTotal(Double priceTotal) {
    //     this.priceTotal = priceTotal;
    // }

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

    public LocalDate getFirstFeeDate() {
        return firstFeeDate;
    }

    public void setFirstFeeDate(LocalDate firstFeeDate) {
        this.firstFeeDate = firstFeeDate;
    }


    public Boolean getPayFirstFee() {
        return payFirstFee;
    }


    public void setPayFirstFee(Boolean payFirstFee) {
        this.payFirstFee = payFirstFee;
    }



    public Double getFirstFeeAmount() {
        return firstFeeAmount;
    }



    public void setFirstFeeAmount(Double firstFeeAmount) {
        this.firstFeeAmount = firstFeeAmount;
    }

    public String getPayments() {
        return payments;
    }

    public void setPayments(String payments) {
        this.payments = payments;
    }

    public List<PaidFeeDto> getPaidFees() {
        return paidFees;
    }

    public void setPaidFees(List<PaidFeeDto> paidFees) {
        this.paidFees = paidFees;
    }

    public Double getAmountFee() {
        return amountFee;
    }

    public void setAmountFee(Double amountFee) {
        this.amountFee = amountFee;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }
        

    
}
