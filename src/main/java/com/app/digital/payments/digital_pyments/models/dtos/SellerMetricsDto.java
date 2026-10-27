package com.app.digital.payments.digital_pyments.models.dtos;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de /api/sellers/metrics: las métricas de cada vendedor del negocio
 * más la fila "Sin vendedor", para que los totales cuadren con el tablero.
 */
public class SellerMetricsDto {

    private int year;
    private int month;
    /** Monedas presentes en los datos: alimentan el selector de moneda. */
    private List<String> monedasDisponibles = new ArrayList<>();
    private List<SellerMetricDto> vendedores = new ArrayList<>();
    private SellerMetricDto sinVendedor;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public List<String> getMonedasDisponibles() {
        return monedasDisponibles;
    }

    public void setMonedasDisponibles(List<String> monedasDisponibles) {
        this.monedasDisponibles = monedasDisponibles;
    }

    public List<SellerMetricDto> getVendedores() {
        return vendedores;
    }

    public void setVendedores(List<SellerMetricDto> vendedores) {
        this.vendedores = vendedores;
    }

    public SellerMetricDto getSinVendedor() {
        return sinVendedor;
    }

    public void setSinVendedor(SellerMetricDto sinVendedor) {
        this.sinVendedor = sinVendedor;
    }
}
