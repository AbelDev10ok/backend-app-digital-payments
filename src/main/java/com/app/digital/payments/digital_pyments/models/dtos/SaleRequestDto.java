package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.app.digital.payments.digital_pyments.utils.IValueOfEnum;
import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleKind;

import io.micrometer.common.lang.NonNull;
import jakarta.validation.constraints.Min;

public class SaleRequestDto {

    @NonNull
    private Long clientId;
    private String descriptionProduct;

    // Tipo de transacción: VENTA o PRESTAMO (obligatorio)
    @IValueOfEnum(enumClass = SaleKind.class, message = "El tipo de transacción debe ser VENTA o PRESTAMO")
    private String kind;

    // El precio debe ser mayor a 0
    // @NotNull(message = "El precio total no puede estar vacío")
    // @Min(value = 1, message = "El precio total debe ser mayor a 0")
    // private Double priceTotal;
    
    // El tipo de pago debe ser uno de los valores definidos en Payments
    @IValueOfEnum(enumClass = Payments.class, message = "El tipo de pago debe ser SEMANAL, QUINCENAL o MENSUAL o CONTADO")
    private String payments;

    @Min(value = 0, message = "La cantidad de cuotas no puede ser negativa")
    private Integer quantityFees;

    // En VENTA el backend calcula el monto; en PRESTAMO es obligatorio.
    private BigDecimal amountFee;

    // En VENTA el backend calcula el costo desde la variante; en PRESTAMO es obligatorio.
    private BigDecimal cost;

    // Tasa de interés del préstamo (solo PRESTAMO, opcional)
    private BigDecimal interestRate;

    // en caso de que sea una venta vieja
    private LocalDate dateSale;

    private LocalDate firstFeeDate;

    private Boolean payFirstFee;

    private BigDecimal firstFeeAmount; // Monto de la primera cuota, puede ser null si no se paga
    

    private List<PaidFeeDto> paidFees; // Lista de cuotas pagadas

    // Categoría de producto (solo obligatoria para VENTA)
    private Long productType;

    // Variante/SKU vendido (obligatorio para VENTA)
    private Long variant;

    // Cantidad de unidades vendidas (solo VENTA, opcional; por defecto 1)
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer quantity;

    // Moneda ISO 4217 elegida para un PRESTAMO (no tiene producto del que
    // heredarla). En una VENTA la moneda la manda el producto y este campo solo
    // puede repetirla: si la contradice, el alta se rechaza.
    private String currency;

    public SaleRequestDto() {
    
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

    public BigDecimal getAmountFee() {
        return amountFee;
    }

    public void setAmountFee(BigDecimal amountFee) {
        this.amountFee = amountFee;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
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

    public BigDecimal getFirstFeeAmount() {
        return firstFeeAmount;
    }

    public void setFirstFeeAmount(BigDecimal firstFeeAmount) {
        this.firstFeeAmount = firstFeeAmount;
    }

    public List<PaidFeeDto> getPaidFees() {
        return paidFees;
    }

    public void setPaidFees(List<PaidFeeDto> paidFees) {
        this.paidFees = paidFees;
    }

    public Long getProductType() {
        return productType;
    }

    public void setProductType(Long productType) {
        this.productType = productType;
    }

    public Long getVariant() {
        return variant;
    }

    public void setVariant(Long variant) {
        this.variant = variant;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    
}
