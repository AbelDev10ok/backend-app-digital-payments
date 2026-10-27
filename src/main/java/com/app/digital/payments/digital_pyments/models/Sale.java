package com.app.digital.payments.digital_pyments.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
import jakarta.persistence.Version;

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

    // Vendedor de la operación: se sella al crearla y sólo se corrige editando
    // la operación; reasignar la cartera del cliente no lo mueve. Null = venta
    // directa o registro anterior a la atribución. Ojo: @BatchSize no aplica a
    // asociaciones many-to-one (Hibernate 6.6 rechaza el mapeo al bootear).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Client seller;

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

    // Variante/SKU vendido (opcional, solo VENTA). Una venta = un solo SKU.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = true)
    private ProductVariant variant;

    // Cantidad de unidades del producto vendidas (V9). 1 por defecto; solo aplica a VENTA
    @Column(nullable = false)
    private Integer quantity = 1;

    // Precio de venta unitario del catálogo al momento de la operación (snapshot).
    // En cuotas el total financiado puede ser distinto: este valor es la referencia.
    @Column(precision = 12, scale = 2)
    private BigDecimal unitPrice;

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

    // Fecha en que se anuló y se asentó la devolución (si hubo). Los reportes de
    // reembolsos usan esta fecha, no la de la venta: la plata salió al anular.
    private LocalDate refundAt;

    // Auditoría de la anulación: cuándo, quién y por qué (el motivo es opcional).
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "canceled_by", length = 120)
    private String canceledBy;

    @Column(name = "cancel_reason", length = 300)
    private String cancelReason;

    // Moneda (ISO 4217) congelada al crear la transacción: el monto nunca se
    // conviene, solo se etiqueta. Se asigna desde la config del negocio al crear
    // y NO se edita después. Nullable-safe por esquema legado (la UI usa la
    // moneda del negocio como respaldo si viniera nula).
    @Column(name = "currency", length = 3)
    private String currency;

    // Bloqueo optimista: evita que dos cobros concurrentes sobre la misma venta
    // descuenten el saldo dos veces (el segundo recibe 409 y puede reintentar).
    // El default 0 permite agregar la columna sin reventar las filas existentes.
    @Version
    @Column(columnDefinition = "bigint default 0")
    private Long version;

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

    // El saldo es siempre "total menos lo efectivamente cobrado": se deriva de los
    // cobros registrados en vez de arrastrarlo a mano, porque cada borrado o edicion
    // de monto dejaba un numero sin relacion con la plata que entro (borrar una cuota
    // impaga llegaba a perdonar deuda que nadie habia pagado).
    public BigDecimal recalcularSaldo() {
        BigDecimal total = priceTotal != null ? priceTotal : BigDecimal.ZERO;
        BigDecimal cobrado = cuotas().stream()
                .map(Fee::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.remainingAmount = total.subtract(cobrado).max(BigDecimal.ZERO);
        return this.remainingAmount;
    }

    // Cuota "proxima a cobrar": la impaga de menor numero. La transaccion tiene una
    // sola cuota impaga a la vez (la siguiente se agenda al cobrar). Null si no
    // queda nada por cobrar.
    public Fee cuotaPendiente() {
        return cuotas().stream()
                .filter(fee -> !Boolean.TRUE.equals(fee.getPaid()))
                .min(Comparator.comparing(Fee::getNumberFee, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    // Monto de la proxima cuota: el valor de cuota pactado, nunca el saldo entero
    // (y nunca mas que la deuda). Con deuda de 125.000 y cuotas de 25.000, la
    // proxima es de 25.000; si solo quedan 10.000, es de 10.000.
    public BigDecimal montoProximaCuota() {
        BigDecimal saldo = remainingAmount != null ? remainingAmount : BigDecimal.ZERO;
        BigDecimal estandar = amountFee != null ? amountFee : saldo;
        return estandar.min(saldo).max(BigDecimal.ZERO);
    }

    private List<Fee> cuotas() {
        return fees != null ? fees : List.of();
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


    public Client getSeller() {
        return seller;
    }


    public void setSeller(Client seller) {
        this.seller = seller;
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

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
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

    public LocalDate getRefundAt() {
        return refundAt;
    }

    public void setRefundAt(LocalDate refundAt) {
        this.refundAt = refundAt;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(LocalDateTime canceledAt) {
        this.canceledAt = canceledAt;
    }

    public String getCanceledBy() {
        return canceledBy;
    }

    public void setCanceledBy(String canceledBy) {
        this.canceledBy = canceledBy;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
