package com.app.digital.payments.digital_pyments.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.app.digital.payments.digital_pyments.utils.DocumentType;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Documento comercial emitido para una venta (hoy: remito).
 *
 * Es un snapshot inmutable: al emitir se copian los datos del negocio, del
 * cliente y de la línea vendida. Si después se edita el negocio, el cliente o
 * el producto, el documento ya emitido no cambia. No se guarda ningún PDF:
 * la vista imprimible se genera desde estos datos.
 */
@Entity
@Table(name = "sale_document", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sale_document_business_type_number", columnNames = {
                "business_id", "document_type", "number" }),
        @UniqueConstraint(name = "uk_sale_document_sale_type", columnNames = {
                "sale_id", "document_type" })
})
public class SaleDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType type;

    // Correlativo por negocio y tipo de documento. La numeración visible es
    // de 6 dígitos (000001), pero el número guardado es el entero.
    @Column(nullable = false)
    private Long number;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    // Snapshot del negocio al momento de emitir.
    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "business_address", length = 255)
    private String businessAddress;

    @Column(name = "business_tax_id", length = 20)
    private String businessTaxId;

    @Column(name = "business_phone", length = 50)
    private String businessPhone;

    // Snapshot del cliente al momento de emitir.
    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "client_doc", length = 20)
    private String clientDoc;

    @Column(name = "client_address", length = 255)
    private String clientAddress;

    @Column(name = "client_phone", length = 50)
    private String clientPhone;

    // Snapshot de la línea vendida (una venta = un SKU).
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "variant_label", length = 255)
    private String variantLabel;

    @Column(length = 120)
    private String sku;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 1000)
    private String notes;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    public SaleDocument() {
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

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }

    public String getBusinessTaxId() {
        return businessTaxId;
    }

    public void setBusinessTaxId(String businessTaxId) {
        this.businessTaxId = businessTaxId;
    }

    public String getBusinessPhone() {
        return businessPhone;
    }

    public void setBusinessPhone(String businessPhone) {
        this.businessPhone = businessPhone;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientDoc() {
        return clientDoc;
    }

    public void setClientDoc(String clientDoc) {
        this.clientDoc = clientDoc;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public String getClientPhone() {
        return clientPhone;
    }

    public void setClientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getVariantLabel() {
        return variantLabel;
    }

    public void setVariantLabel(String variantLabel) {
        this.variantLabel = variantLabel;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }
}
