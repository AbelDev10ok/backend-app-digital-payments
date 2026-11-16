package com.app.digital.payments.digital_pyments.models.dtos;

import java.util.ArrayList;
import java.util.List;

/**
 * Ingreso de mercadería por lote: varias líneas en una sola operación. El
 * {@code note} queda como motivo de todos los movimientos creados.
 */
public class StockEntryRequestDto {

    private String note;
    private List<StockEntryLineDto> lines = new ArrayList<>();

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<StockEntryLineDto> getLines() {
        return lines;
    }

    public void setLines(List<StockEntryLineDto> lines) {
        this.lines = lines;
    }
}
