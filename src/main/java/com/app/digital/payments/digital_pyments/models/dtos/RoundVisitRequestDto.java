package com.app.digital.payments.digital_pyments.models.dtos;

/**
 * Marca de visita sobre una parada de la ronda. {@code noEstaba} deja la parada
 * como "no estaba" (o la vuelve a pendiente con {@code false}); la nota es
 * opcional y explica el resultado de la visita.
 */
public class RoundVisitRequestDto {

    private Boolean noEstaba;
    private String note;

    public Boolean getNoEstaba() {
        return noEstaba;
    }

    public void setNoEstaba(Boolean noEstaba) {
        this.noEstaba = noEstaba;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
