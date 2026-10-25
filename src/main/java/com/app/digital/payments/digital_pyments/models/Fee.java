package com.app.digital.payments.digital_pyments.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

import com.app.digital.payments.digital_pyments.utils.PaymentMethod;

@Entity
@Table(name = "fee")
@Filter(name = "tenantFilter", condition = "business_id = :businessId")
public class Fee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Negocio (tenant) al que pertenece la cuota (el mismo de su venta)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    private Sale sale; // ¡CAMBIO AQUÍ! Ahora apunta a la clase base

    @Column(nullable = false)
    private Integer numberFee;

    // Monto programado de la cuota: nunca se sobrescribe al cobrar (el cobro va al log)
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    // Acumulado cobrado de esta cuota: la suma de sus pagos. Es un cache para
    // saber si esta cubierta y sumar deuda sin traer la coleccion de pagos;
    // el log sigue siendo la fuente de verdad del dinero.
    // El default 0 permite agregar la columna sin reventar las filas existentes.
    @Column(name = "paid_amount", columnDefinition = "numeric(12,2) default 0 not null")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate expirationDate;

    @Column(nullable = false)
    private Boolean paid = false;

    // Cache de lectura: fecha y hora del último cobro (para mostrar el horario
    // en los listados sin traer toda la coleccion de pagos).
    @Column(name = "last_payment_at")
    private LocalDateTime lastPaymentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    // El log de cobros de la cuota (parciales y varios por día)
    @OneToMany(mappedBy = "fee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    public Fee() {
    }

    public Fee(Sale sale, Integer numberFee, BigDecimal amount, LocalDate expirationDate) {
        this.sale = sale;
        this.numberFee = numberFee;
        this.amount = amount;
        this.expirationDate = expirationDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
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

    public BigDecimal getPaidAmount() {
        if (paidAmount != null && paidAmount.signum() > 0) {
            return paidAmount;
        }
        // Cuotas cobradas antes del log de pagos: en el modelo viejo el monto de la
        // cuota era lo efectivamente cobrado, así que se usa como respaldo para no
        // contarlas como "cobrado 0".
        if (Boolean.TRUE.equals(paid) && amount != null && amount.signum() > 0) {
            return amount;
        }
        return paidAmount != null ? paidAmount : BigDecimal.ZERO;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
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

    public LocalDateTime getLastPaymentAt() {
        return lastPaymentAt;
    }

    public void setLastPaymentAt(LocalDateTime lastPaymentAt) {
        this.lastPaymentAt = lastPaymentAt;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    /** Monto pendiente de esta cuota (nunca negativo). */
    public BigDecimal getPendingAmount() {
        BigDecimal total = amount != null ? amount : BigDecimal.ZERO;
        return total.subtract(getPaidAmount()).max(BigDecimal.ZERO);
    }

    /** Recalcula el flag {@code paid} a partir del acumulado cobrado. */
    public void refreshPaid() {
        BigDecimal total = amount != null ? amount : BigDecimal.ZERO;
        this.paid = total.signum() > 0 && getPaidAmount().compareTo(total) >= 0;
    }

    /**
     * Registra un cobro: acumula el monto, guarda el horario y actualiza el flag.
     * No toca el monto programado de la cuota.
     */
    public void applyPayment(Payment payment) {
        this.payments.add(payment);
        this.paidAmount = getPaidAmount().add(payment.getAmount());
        this.lastPaymentAt = payment.getPaidAt();
        this.paymentMethod = payment.getPaymentMethod();
        refreshPaid();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((sale == null) ? 0 : sale.hashCode());
        result = prime * result + ((numberFee == null) ? 0 : numberFee.hashCode());
        result = prime * result + ((amount == null) ? 0 : amount.hashCode());
        result = prime * result + ((expirationDate == null) ? 0 : expirationDate.hashCode());
        result = prime * result + ((paid == null) ? 0 : paid.hashCode());
        result = prime * result + ((lastPaymentAt == null) ? 0 : lastPaymentAt.hashCode());
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
        Fee other = (Fee) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (sale == null) {
            if (other.sale != null)
                return false;
        } else if (!sale.equals(other.sale))
            return false;
        if (numberFee == null) {
            if (other.numberFee != null)
                return false;
        } else if (!numberFee.equals(other.numberFee))
            return false;
        if (amount == null) {
            if (other.amount != null)
                return false;
        } else if (!amount.equals(other.amount))
            return false;
        if (expirationDate == null) {
            if (other.expirationDate != null)
                return false;
        } else if (!expirationDate.equals(other.expirationDate))
            return false;
        if (paid == null) {
            if (other.paid != null)
                return false;
        } else if (!paid.equals(other.paid))
            return false;
        if (lastPaymentAt == null) {
            if (other.lastPaymentAt != null)
                return false;
        } else if (!lastPaymentAt.equals(other.lastPaymentAt))
            return false;
        return true;
    }
}
