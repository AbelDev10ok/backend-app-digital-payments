package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;

import com.app.digital.payments.digital_pyments.utils.IValueOfEnum;
import com.app.digital.payments.digital_pyments.utils.Payments;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request para actualizar los datos del negocio propio. Todos los campos son
 * opcionales salvo el nombre. Los datos de contacto se borran enviándolos vacíos;
 * la tasa por defecto se borra con null (es un número: no distingue "ausente" de
 * null) y la frecuencia, vacía. Así "Sin valor por defecto" se puede guardar.
 */
public class BusinessUpdateRequestDto {

    @NotBlank(message = "El nombre del negocio es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
    private String name;

    // Datos opcionales que se imprimen en los remitos. El CUIT se valida con
    // una regla básica (11 dígitos); nunca bloquea la emisión de un remito.
    @Pattern(regexp = "^$|^[0-9]{11}$", message = "El CUIT debe tener 11 dígitos")
    private String cuit;

    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    private String address;

    @Size(max = 50, message = "El teléfono no puede superar los 50 caracteres")
    private String phone;

    // Moneda ISO 4217 (ej: ARS, USD, PYG)
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "La moneda debe ser un codigo ISO de 3 letras")
    private String currency;

    // Tasa de interes por defecto para prestamos, en porcentaje (0 a 500).
    // Null borra el default: "Sin valor por defecto" se guarda null.
    @DecimalMin(value = "0", message = "La tasa de interes no puede ser negativa")
    @DecimalMax(value = "500", message = "La tasa de interes no puede superar el 500%")
    private BigDecimal defaultInterestRate;

    // Frecuencia de cuota por defecto para prestamos. Vacío borra el default;
    // CONTADO no aplica: un préstamo nunca es a contado.
    @IValueOfEnum(enumClass = Payments.class, message = "La frecuencia debe ser SEMANAL, QUINCENAL, MENSUAL o CONTADO")
    private String defaultPaymentFrequency;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCuit() {
        return cuit;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getDefaultInterestRate() {
        return defaultInterestRate;
    }

    public void setDefaultInterestRate(BigDecimal defaultInterestRate) {
        this.defaultInterestRate = defaultInterestRate;
    }

    public String getDefaultPaymentFrequency() {
        return defaultPaymentFrequency;
    }

    public void setDefaultPaymentFrequency(String defaultPaymentFrequency) {
        this.defaultPaymentFrequency = defaultPaymentFrequency;
    }
}
