package com.app.digital.payments.digital_pyments.models.dtos;

import java.time.LocalDateTime;

/**
 * Resultado de registrar un recordatorio de pago: el envio es manual por
 * WhatsApp, aca solo queda la marca de gestion para la agenda de cobros.
 */
public class RecordatorioDto {

    private Long feeId;
    private LocalDateTime ultimoRecordatorio;
    private Integer recordatorios;

    public RecordatorioDto() {
    }

    public RecordatorioDto(Long feeId, LocalDateTime ultimoRecordatorio, Integer recordatorios) {
        this.feeId = feeId;
        this.ultimoRecordatorio = ultimoRecordatorio;
        this.recordatorios = recordatorios;
    }

    public Long getFeeId() {
        return feeId;
    }

    public void setFeeId(Long feeId) {
        this.feeId = feeId;
    }

    public LocalDateTime getUltimoRecordatorio() {
        return ultimoRecordatorio;
    }

    public void setUltimoRecordatorio(LocalDateTime ultimoRecordatorio) {
        this.ultimoRecordatorio = ultimoRecordatorio;
    }

    public Integer getRecordatorios() {
        return recordatorios;
    }

    public void setRecordatorios(Integer recordatorios) {
        this.recordatorios = recordatorios;
    }
}