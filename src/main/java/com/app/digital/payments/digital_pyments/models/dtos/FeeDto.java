package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FeeDto {
    private Long id;
    private Long saleId; // Nuevo: referencia a la venta
    private Integer numberFee;
    private BigDecimal amount;
    private LocalDate expirationDate; // Mejor usar LocalDate directamente
    private Boolean paid = false;
    private LocalDate paymentDate; // Renombrado a paymentDate para consistencia
    private Boolean postponed = false; // Nuevo: indica si fue pospuesta
    private String productDescription; // Nuevo: descripción del producto
    // private ClientDto client; // Nuevo: datos básicos del cliente
    private String paymentMethod; // EFECTIVO | TRANSFERENCIA
    
    // Campos calculados
    private String status; // "PENDING", "PAID"


    public FeeDto() {
    }

    public FeeDto(Long id, Long saleId, Integer numberFee, BigDecimal amount, LocalDate expirationDate, Boolean paid,
            LocalDate paymentDate, Boolean postponed, String productDescription, ClientDto client, String status,
            String paymentMethod
            ) {
        this.id = id;
        this.saleId = saleId;
        this.numberFee = numberFee;
        this.amount = amount;
        this.expirationDate = expirationDate;
        this.paid = paid;
        this.paymentDate = paymentDate;
        this.postponed = postponed;
        this.productDescription = productDescription;
        // this.client = client;
        this.status = status;
        this.paymentMethod = paymentMethod;
    }


      // Método para calcular estado (opcional, puede hacerse en servicio)
    public void calculateStatus() {
        if (paid) {
            status = "PAID";
        }
        else{
            status = "PENDING";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSaleId() {
        return saleId;
    }

    public void setSaleId(Long saleId) {
        this.saleId = saleId;
    }

    public Integer getNumberFee() {
        return numberFee;
    }

    public void setNumberFee(Integer numberFee) {
        this.numberFee = numberFee;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
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

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Boolean getPostponed() {
        return postponed;
    }

    public void setPostponed(Boolean postponed) {
        this.postponed = postponed;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    // public ClientDto getClient() {
    //     return client;
    // }

    // public void setClient(ClientDto client) {
    //     this.client = client;
    // }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
}
