package com.app.digital.payments.digital_pyments.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

// Conteos por estado para los badges del sidebar (p.ej. "A Cobrar (4)")
public class SalesCountsDto {

    private long total;
    private long active;
    private long completed;
    private long canceled;
    private long aCobrar;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getActive() {
        return active;
    }

    public void setActive(long active) {
        this.active = active;
    }

    public long getCompleted() {
        return completed;
    }

    public void setCompleted(long completed) {
        this.completed = completed;
    }

    public long getCanceled() {
        return canceled;
    }

    public void setCanceled(long canceled) {
        this.canceled = canceled;
    }

    // Jackson deriva el nombre público del getter: `getACobrar()` se serializaba
    // como "acobrar" y el frontend (que lee `aCobrar`) nunca veía el conteo del
    // sidebar. Se fija el nombre del contrato.
    @JsonProperty("aCobrar")
    public long getACobrar() {
        return aCobrar;
    }

    @JsonProperty("aCobrar")
    public void setACobrar(long aCobrar) {
        this.aCobrar = aCobrar;
    }
}
