package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Respuesta de la agenda de cobros: cuotas pendientes que vencen hoy
 * mas las ya vencidas, con un resumen por grupo para los chips de la UI.
 */
public class CobrosHoyResponseDto {

    private Resumen hoy = new Resumen();
    private Resumen vencidas = new Resumen();
    private List<CuotaListItemDto> items = new ArrayList<>();
    private LocalDate fecha;

    public static class Resumen {
        private long cantidad;
        // Montos por moneda ISO 4217: las monedas distintas nunca se suman
        private Map<String, BigDecimal> monto = new LinkedHashMap<>();

        public long getCantidad() {
            return cantidad;
        }

        public void setCantidad(long cantidad) {
            this.cantidad = cantidad;
        }

        public Map<String, BigDecimal> getMonto() {
            return monto;
        }

        public void setMonto(Map<String, BigDecimal> monto) {
            this.monto = monto;
        }
    }

    public Resumen getHoy() {
        return hoy;
    }

    public void setHoy(Resumen hoy) {
        this.hoy = hoy;
    }

    public Resumen getVencidas() {
        return vencidas;
    }

    public void setVencidas(Resumen vencidas) {
        this.vencidas = vencidas;
    }

    public List<CuotaListItemDto> getItems() {
        return items;
    }

    public void setItems(List<CuotaListItemDto> items) {
        this.items = items;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
}
