package com.app.digital.payments.digital_pyments.models.dtos;

import jakarta.validation.constraints.Size;

/**
 * Datos opcionales al emitir un remito. El número y el snapshot los genera el
 * backend; el cliente solo puede agregar observaciones.
 */
public class SaleDocumentRequestDto {

    @Size(max = 1000, message = "Las observaciones no pueden superar los 1000 caracteres")
    private String notes;

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
