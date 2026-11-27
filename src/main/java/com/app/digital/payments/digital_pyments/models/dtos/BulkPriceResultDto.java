package com.app.digital.payments.digital_pyments.models.dtos;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de un ajuste masivo: todas las filas del alcance con su antes y
 * después, y el batch que agrupa los cambios aplicados (null en el preview).
 */
public class BulkPriceResultDto {

    private List<BulkPriceRowDto> rows = new ArrayList<>();
    private int totalCount;
    private int changedCount;
    private int unchangedCount;
    private String batchId;

    public List<BulkPriceRowDto> getRows() {
        return rows;
    }

    public void setRows(List<BulkPriceRowDto> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getChangedCount() {
        return changedCount;
    }

    public void setChangedCount(int changedCount) {
        this.changedCount = changedCount;
    }

    public int getUnchangedCount() {
        return unchangedCount;
    }

    public void setUnchangedCount(int unchangedCount) {
        this.unchangedCount = unchangedCount;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }
}