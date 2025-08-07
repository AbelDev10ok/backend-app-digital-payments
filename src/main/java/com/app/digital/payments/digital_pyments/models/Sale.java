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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity // Sigue siendo una entidad
@Table(name = "sale") // Su propia tabla para las propiedades específicas (si las hubiera)
public class Sale{ // Ahora extiende PayableItem

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client; // Asume que ya tienes la entidad Client

    @Column(nullable = false)
    private String descriptionProduct; // Descripción del producto/servicio

    @Column(nullable = false)
    private Double priceTotal; // Precio total original del ítem

    @Column(nullable = false)
    private LocalDate dateCreation; // Fecha de creación/venta/préstamo

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Payments typePayments; // Tipo de pago (enum, lo veremos más adelante)

    @Column(nullable = false)
    private Integer quiantityFees; // Cantidad de cuotas originales

    @Column(nullable = false)
    private Double cost;

    @Column(nullable = false)
    private Double remainingAmount; // Monto restante por pagar (se inicializará)

    @Column(nullable = false)
    private Double amountFee; // Monto de cada cuota

    @Column(nullable = false)
    private String productType;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Fee> fees = new ArrayList<>(); // Lista de cuotas asociadas

    private LocalDate finalPaymentDate; // Fecha de pago final esperada
    private LocalDate realFinalDate;    // Fecha de pago final real
    private Integer daysLate = 0;       // Días de retraso
    private boolean completed;          // Si está completamente pagada
    

    // Constructor para inicializar remainingAmount (puedes ajustarlo si prefieres setters)
    public Sale() {
        this.remainingAmount = this.priceTotal; // Esto solo funciona si priceTotal se establece antes
                                                // Es mejor inicializarlo en un constructor específico
                                                // o con un método setter después de setPriceTotal.
    }


    public void deductFromRemainingAmount(Double amount) {
        this.remainingAmount = Math.max(0, this.remainingAmount - amount);
        if(this.remainingAmount <= 0) {
            this.completed = true;
        }
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


    public Double getPriceTotal() {
        return priceTotal;
    }


    public void setPriceTotal(Double priceTotal) {
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

    public Double getRemainingAmount() {
        return remainingAmount;
    }


    public void setRemainingAmount(Double remainingAmount) {
        this.remainingAmount = remainingAmount;
    }


    public Double getAmountFee() {
        return amountFee;
    }


    public void setAmountFee(Double amountFee) {
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


    public String getDescriptionProduct() {
        return descriptionProduct;
    }


    public void setDescriptionProduct(String descriptionProduct) {
        this.descriptionProduct = descriptionProduct;
    }


    public Double getCost() {
        return cost;
    }


    public void setCost(Double cost) {
        this.cost = cost;
    }


    public String getProductType() {
        return productType;
    }


    public void setProductType(String productType) {
        this.productType = productType;
    }
    
    
    
}
