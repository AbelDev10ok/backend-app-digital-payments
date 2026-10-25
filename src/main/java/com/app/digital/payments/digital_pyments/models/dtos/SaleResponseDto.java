package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleKind;


public class SaleResponseDto {
    private Long id;
    private ClientDto client;
    private String descriptionProduct;
    private BigDecimal priceTotal;
    private LocalDate dateSale;
    private LocalDate finalPaymentDate; // Fecha final de pago estimada 
    private LocalDate realFinalPayment;
    private Payments typePayments;
    private Integer quantityFees;
    // private boolean completed; // Si está completamente pagada
    private BigDecimal amountFee;
    private List<FeeDto> fees;
    private BigDecimal cost;
    private String status;

    private BigDecimal interestRate;

    private ProductTypeDto productType;

    // Producto del catálogo asociado (opcional)
    private ProductDto product;

    private SaleKind kind;

    // Cantidad de unidades del producto vendidas (V9; solo VENTA)
    private Integer quantity;

    
    private Integer paidFeesCount;  // Cuotas pagadas
 
    // private Integer additionalFees = 0; // Cuotas extras agregadas
    
    private BigDecimal remainingAmount;
    private Integer totalFees; // Total de cuotas (originales + extras)

    // Monto ya cobrado antes de la anulación (suma de cuotas pagadas)
    private BigDecimal collectedAmount;

    // Monto devuelto al cliente al anular (0 si el monto cobrado se retiene)
    private BigDecimal refundAmount;

    // Moneda (ISO 4217) congelada al crear la transacción (snapshot)
    private String currency;

    // Remito emitido (si existe). Se usa para mostrar el número y habilitar
    // la reimpresión sin pedir el documento completo en cada fila.
    private RemitoResumenDto remito;





    public SaleResponseDto() {
    }

    public SaleResponseDto(Long id, ClientDto client, String descriptionProduct, BigDecimal priceTotal,
                           LocalDate dateSale, LocalDate finalPaymentDate, Payments typePayments,
                        Integer quantityFees, boolean completed, BigDecimal amountFe,
                           List<FeeDto> fees, Integer paidFeesCount, BigDecimal remainingAmount, Integer totalFees,
                           String status) {
        this.id = id;
        this.client = client;
        this.descriptionProduct = descriptionProduct;
        this.priceTotal = priceTotal;
        this.dateSale = dateSale;
        this.finalPaymentDate = finalPaymentDate;
        this.typePayments = typePayments;
        this.quantityFees = quantityFees;
        // this.completed = completed;
        this.amountFee = amountFe;
        this.fees = fees;
        this.paidFeesCount = paidFeesCount;
        this.remainingAmount = remainingAmount;
        this.totalFees = totalFees;
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
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

    public BigDecimal getPriceTotal() {
        return priceTotal;
    }

    public void setPriceTotal(BigDecimal priceTotal) {
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


    public Integer getQuantityFees() {
        return quantityFees;
    }

    public void setQuantityFees(Integer quantityFees) {
        this.quantityFees = quantityFees;
    }

    // public boolean isCompleted() {
    //     return completed;
    // }

    // public void setCompleted(boolean completed) {
    //     this.completed = completed;
    // }

    public BigDecimal getAmountFee() {
        return amountFee;
    }       

    public void setAmountFee(BigDecimal amountFee) {
        this.amountFee = amountFee;
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


    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public BigDecimal getCollectedAmount() {
        return collectedAmount;
    }

    public void setCollectedAmount(BigDecimal collectedAmount) {
        this.collectedAmount = collectedAmount;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getTotalFees() {
        return totalFees;
    }

    public void setTotalFees(Integer totalFees) {
        this.totalFees = totalFees;
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

    public ProductTypeDto getProductType() {
        return productType;
    }

    public void setProductType(ProductTypeDto productType) {
        this.productType = productType;
    }

    public ProductDto getProduct() {
        return product;
    }

    public void setProduct(ProductDto product) {
        this.product = product;
    }

    public SaleKind getKind() {
        return kind;
    }

    public void setKind(SaleKind kind) {
        this.kind = kind;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getRealFinalPayment() {
        return realFinalPayment;
    }

    public void setRealFinalPayment(LocalDate realFinalPayment) {
        this.realFinalPayment = realFinalPayment;
    }

    public RemitoResumenDto getRemito() {
        return remito;
    }

    public void setRemito(RemitoResumenDto remito) {
        this.remito = remito;
    }
}
