package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import com.app.digital.payments.digital_pyments.utils.SaleKind;

/**
 * Métricas de un vendedor en el período consultado, más su cartera actual.
 *
 * Cada bloque viene separado por tipo de operación (VENTA/PRESTAMO) y, dentro
 * de cada tipo, agrupado por moneda: las monedas distintas nunca se suman.
 * Un {@code sellerId} nulo representa la fila "Sin vendedor" (operaciones no
 * atribuidas y clientes sin cartera asignada).
 *
 * "Vendido" y "cobrado" son la actividad del vendedor (el sello de cada
 * operación); "deuda" es la cobranza de su cartera actual, que puede incluir
 * operaciones vendidas por otro. Son preguntas distintas y así se muestran.
 */
public class SellerMetricDto {

    private Long sellerId;
    private String sellerName;
    // ¿Sigue habilitado como vendedor hoy? Los deshabilitados conservan su
    // historial y aparecen marcados, aunque ya no estén en los selectores.
    private boolean activo;
    /** Clientes que hoy tiene asignados (cartera actual). */
    private long clientesCartera;
    /** Operaciones creadas en el período (incluye las luego anuladas). */
    private Map<SaleKind, Long> operaciones = new LinkedHashMap<>();
    /** De esas operaciones, las que hoy están anuladas. */
    private Map<SaleKind, Long> anuladas = new LinkedHashMap<>();
    /** Monto vendido en el período, por tipo y moneda. */
    private Map<SaleKind, Map<String, BigDecimal>> vendido = new LinkedHashMap<>();
    /** Cobrado en el período por fecha de cobro, por tipo y moneda. */
    private Map<SaleKind, Map<String, BigDecimal>> cobrado = new LinkedHashMap<>();
    /** Deuda de su cartera a hoy (operaciones activas de sus clientes), por tipo y moneda. */
    private Map<SaleKind, Map<String, BigDecimal>> deuda = new LinkedHashMap<>();

    public SellerMetricDto() {
        // Todos los tipos presentes desde el arranque: el frontend no tiene que
        // cubrir cada combinación ausente.
        for (SaleKind kind : SaleKind.values()) {
            operaciones.put(kind, 0L);
            anuladas.put(kind, 0L);
            vendido.put(kind, new TreeMap<>());
            cobrado.put(kind, new TreeMap<>());
            deuda.put(kind, new TreeMap<>());
        }
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public long getClientesCartera() {
        return clientesCartera;
    }

    public void setClientesCartera(long clientesCartera) {
        this.clientesCartera = clientesCartera;
    }

    public Map<SaleKind, Long> getOperaciones() {
        return operaciones;
    }

    public void setOperaciones(Map<SaleKind, Long> operaciones) {
        this.operaciones = operaciones;
    }

    public Map<SaleKind, Long> getAnuladas() {
        return anuladas;
    }

    public void setAnuladas(Map<SaleKind, Long> anuladas) {
        this.anuladas = anuladas;
    }

    public Map<SaleKind, Map<String, BigDecimal>> getVendido() {
        return vendido;
    }

    public void setVendido(Map<SaleKind, Map<String, BigDecimal>> vendido) {
        this.vendido = vendido;
    }

    public Map<SaleKind, Map<String, BigDecimal>> getCobrado() {
        return cobrado;
    }

    public void setCobrado(Map<SaleKind, Map<String, BigDecimal>> cobrado) {
        this.cobrado = cobrado;
    }

    public Map<SaleKind, Map<String, BigDecimal>> getDeuda() {
        return deuda;
    }

    public void setDeuda(Map<SaleKind, Map<String, BigDecimal>> deuda) {
        this.deuda = deuda;
    }
}
