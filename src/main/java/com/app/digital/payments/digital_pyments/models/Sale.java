package com.app.digital.payments.digital_pyments.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.Filter;

@Entity // Sigue siendo una entidad
@Table(name = "sale") // Su propia tabla para las propiedades específicas (si las hubiera)
@Filter(name = "tenantFilter", condition = "business_id = :businessId")
public class Sale{ // Ahora extiende PayableItem

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Negocio (tenant) al que pertenece la transaccion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client; // Asume que ya tienes la entidad Client

    @Column(nullable = false)
    private String descriptionProduct; // Descripción del producto/servicio

    @Column(nullable = false)
    private BigDecimal priceTotal; // Precio total original del ítem

    @Column(nullable = false)
    private LocalDate dateCreation; // Fecha de creación/venta/préstamo

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Payments typePayments; // Tipo de pago (enum, lo veremos más adelante)

    @Column(nullable = false)
    private Integer quiantityFees; // Cantidad de cuotas originales

    @Column(nullable = false)
    private BigDecimal cost;

    @Column(precision = 12, scale = 2)
    private BigDecimal interestRate; // Tasa de interés del préstamo (solo PRESTAMO)

    @Column(nullable = false)
    private BigDecimal remainingAmount; // Monto restante por pagar

    @Column(nullable = false)
    private BigDecimal amountFee; // Monto de cada cuota

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SaleKind kind; // Tipo de transacción: VENTA o PRESTAMO

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_type_id", nullable = true)
    private ProductType productType; // Categoría de producto (solo para VENTA)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = true)
    private Product product; // Producto del catálogo (opcional, solo VENTA)

    // Cantidad de unidades del producto vendidas (V9). 1 por defecto; solo aplica a VENTA
    @Column(nullable = false)
    private Integer quantity = 1;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Fee> fees = new ArrayList<>(); // Lista de cuotas asociadas

    private LocalDate finalPaymentDate; // Fecha de pago final esperada
    private LocalDate realFinalDate;    // Fecha de pago final real
    // private boolean completed;          // Si está completamente pagada
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SaleStatus status;

    @Column(nullable = false)
    private BigDecimal refundAmount = BigDecimal.ZERO; // campo para el monto del reembolso

    public Sale() {
    }

    public void deductFromRemainingAmount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("El monto a deducir no puede ser negativo o nulo");
        }
        this.remainingAmount = this.remainingAmount == null ? BigDecimal.ZERO : this.remainingAmount.subtract(amount).max(BigDecimal.ZERO);
        if (this.remainingAmount.signum() <= 0) {
            this.realFinalDate = LocalDate.now();
            this.status = SaleStatus.COMPLETED;
        } else {
            this.status = SaleStatus.ACTIVE;
        }
    }


    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }


    public Client getClient() {
        return client;
    }


    public void setClient(Client client) {
        this.client = client;
    }


    public BigDecimal getPriceTotal() {
        return priceTotal;
    }


    public void setPriceTotal(BigDecimal priceTotal) {
        this.priceTotal = priceTotal;
    }


    public LocalDate getDateCreation() {
        return dateCreation;
    }


    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
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

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }


    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }


    public BigDecimal getAmountFee() {
        return amountFee;
    }


    public void setAmountFee(BigDecimal amountFee) {
        this.amountFee = amountFee;
    }


    public List<Fee> getFees() {
        return fees;
    }


    public void setFees(List<Fee> fees) {
        this.fees = fees;
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



    public String getDescriptionProduct() {
        return descriptionProduct;
    }


    public void setDescriptionProduct(String descriptionProduct) {
        this.descriptionProduct = descriptionProduct;
    }


    public BigDecimal getCost() {
        return cost;
    }


    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }


    public ProductType getProductType() {
        return productType;
    }


    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }


    public SaleStatus getStatus() {
        return status;
    }


    public void setStatus(SaleStatus status) {
        this.status = status;
    }

    public SaleKind getKind() {
        return kind;
    }


    public void setKind(SaleKind kind) {
        this.kind = kind;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

}
