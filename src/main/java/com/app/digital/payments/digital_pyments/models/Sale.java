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
    private Integer quiantityFees;
    
    @Column(nullable = false)
    private Double amountFe;
    
    @OneToMany(mappedBy = "sales", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Fee> fees = new ArrayList<>();


    public Sale() {
    }
    public Sale(Client client, String descriptiononProduct, Double priceTotal, LocalDate dateSale,
            Payments typePayments, Integer quiantityFees, Double amountFe) {
        this.client = client;
        this.descriptiononProduct = descriptiononProduct;
        this.priceTotal = priceTotal;
        this.dateSale = dateSale;
        this.typePayments = typePayments;
        this.quiantityFees = quiantityFees;
        this.amountFe = amountFe;
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
        return amountFe;
    }

    public void setAmountFe(Double amountFe) {
        this.amountFe = amountFe;
    }

    public List<Fee> getFees() {
        return fees;
    }

    public void setFees(List<Fee> fees) {
        this.fees = fees;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((client == null) ? 0 : client.hashCode());
        result = prime * result + ((descriptiononProduct == null) ? 0 : descriptiononProduct.hashCode());
        result = prime * result + ((priceTotal == null) ? 0 : priceTotal.hashCode());
        result = prime * result + ((dateSale == null) ? 0 : dateSale.hashCode());
        result = prime * result + ((typePayments == null) ? 0 : typePayments.hashCode());
        result = prime * result + ((quiantityFees == null) ? 0 : quiantityFees.hashCode());
        result = prime * result + ((amountFe == null) ? 0 : amountFe.hashCode());
        result = prime * result + ((fees == null) ? 0 : fees.hashCode());
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
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
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
        if (amountFe == null) {
            if (other.amountFe != null)
                return false;
        } else if (!amountFe.equals(other.amountFe))
            return false;
        if (fees == null) {
            if (other.fees != null)
                return false;
        } else if (!fees.equals(other.fees))
            return false;
        return true;
    }
    
}
