package com.app.digital.payments.digital_pyments.models.dtos;

import java.time.LocalDateTime;

/**
 * Resumen mínimo del remito para mostrar en la venta (detalle, listado y
 * confirmación de borrado) sin cargar el documento completo.
 */
public class RemitoResumenDto {

    private Long id;
    private Long number;
    private String numberLabel;
    private LocalDateTime issuedAt;

    public RemitoResumenDto() {
    }

    public RemitoResumenDto(Long id, Long number, String numberLabel, LocalDateTime issuedAt) {
        this.id = id;
        this.number = number;
        this.numberLabel = numberLabel;
        this.issuedAt = issuedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public String getNumberLabel() {
        return numberLabel;
    }

    public void setNumberLabel(String numberLabel) {
        this.numberLabel = numberLabel;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }
}
