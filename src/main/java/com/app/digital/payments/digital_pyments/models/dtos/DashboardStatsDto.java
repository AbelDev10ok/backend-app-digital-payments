package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estadísticas del dashboard.
 *
 * Regla de dominio: los montos de monedas distintas NUNCA se suman entre sí.
 * Por eso cada métrica monetaria es un mapa {moneda ISO 4217 → monto}; los
 * conteos (ventas, estados, clientes) sí son globales porque contar no mezcla
 * monedas.
 */
public class DashboardStatsDto {

    private int year;
    private int month;
    /** Monedas (ISO 4217) presentes en los datos: alimentan el selector de moneda */
    private List<String> monedasDisponibles = new ArrayList<>();
    private Resumen resumen = new Resumen();
    private Anual anual = new Anual();
    private List<Monthly> serie12Meses = new ArrayList<>();
    private List<TopClient> topClientes = new ArrayList<>();
    private List<ProductTypeSales> porTipoProducto = new ArrayList<>();
    private Clientes clientes = new Clientes();

    public DashboardStatsDto() {
    }

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

    public Resumen getResumen() {
        return resumen;
    }

    public void setResumen(Resumen resumen) {
        this.resumen = resumen;
    }

    public Anual getAnual() {
        return anual;
    }

    public void setAnual(Anual anual) {
        this.anual = anual;
    }

    public List<Monthly> getSerie12Meses() {
        return serie12Meses;
    }

    public void setSerie12Meses(List<Monthly> serie12Meses) {
        this.serie12Meses = serie12Meses;
    }

    public List<TopClient> getTopClientes() {
        return topClientes;
    }

    public void setTopClientes(List<TopClient> topClientes) {
        this.topClientes = topClientes;
    }

    public List<ProductTypeSales> getPorTipoProducto() {
        return porTipoProducto;
    }

    public void setPorTipoProducto(List<ProductTypeSales> porTipoProducto) {
        this.porTipoProducto = porTipoProducto;
    }

    public Clientes getClientes() {
        return clientes;
    }

    public void setClientes(Clientes clientes) {
        this.clientes = clientes;
    }

    public static class Resumen {
        private long totalVentas;
        private Map<String, BigDecimal> totalVendido = new LinkedHashMap<>();
        private Map<String, BigDecimal> totalCobrado = new LinkedHashMap<>();
        private Map<String, BigDecimal> pendienteDelMes = new LinkedHashMap<>();
        private Map<String, BigDecimal> deudaTotal = new LinkedHashMap<>();
        private Map<String, BigDecimal> ganancia = new LinkedHashMap<>();
        private Map<String, BigDecimal> reembolsos = new LinkedHashMap<>();
        private long completadas;
        private long pendientes;
        private long canceladas;
        private CuotasVencidas cuotasVencidas = new CuotasVencidas();

        public long getTotalVentas() {
            return totalVentas;
        }

        public void setTotalVentas(long totalVentas) {
            this.totalVentas = totalVentas;
        }

        public Map<String, BigDecimal> getTotalVendido() {
            return totalVendido;
        }

        public void setTotalVendido(Map<String, BigDecimal> totalVendido) {
            this.totalVendido = totalVendido;
        }

        public Map<String, BigDecimal> getTotalCobrado() {
            return totalCobrado;
        }

        public void setTotalCobrado(Map<String, BigDecimal> totalCobrado) {
            this.totalCobrado = totalCobrado;
        }

        public Map<String, BigDecimal> getPendienteDelMes() {
            return pendienteDelMes;
        }

        public void setPendienteDelMes(Map<String, BigDecimal> pendienteDelMes) {
            this.pendienteDelMes = pendienteDelMes;
        }

        public Map<String, BigDecimal> getDeudaTotal() {
            return deudaTotal;
        }

        public void setDeudaTotal(Map<String, BigDecimal> deudaTotal) {
            this.deudaTotal = deudaTotal;
        }

        public Map<String, BigDecimal> getGanancia() {
            return ganancia;
        }

        public void setGanancia(Map<String, BigDecimal> ganancia) {
            this.ganancia = ganancia;
        }

        public Map<String, BigDecimal> getReembolsos() {
            return reembolsos;
        }

        public void setReembolsos(Map<String, BigDecimal> reembolsos) {
            this.reembolsos = reembolsos;
        }

        public long getCompletadas() {
            return completadas;
        }

        public void setCompletadas(long completadas) {
            this.completadas = completadas;
        }

        public long getPendientes() {
            return pendientes;
        }

        public void setPendientes(long pendientes) {
            this.pendientes = pendientes;
        }

        public long getCanceladas() {
            return canceladas;
        }

        public void setCanceladas(long canceladas) {
            this.canceladas = canceladas;
        }

        public CuotasVencidas getCuotasVencidas() {
            return cuotasVencidas;
        }

        public void setCuotasVencidas(CuotasVencidas cuotasVencidas) {
            this.cuotasVencidas = cuotasVencidas;
        }
    }

    public static class CuotasVencidas {
        private long cantidad;
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

    public static class Anual {
        private long totalVentas;
        private Map<String, BigDecimal> totalVendido = new LinkedHashMap<>();
        private Map<String, BigDecimal> totalCobrado = new LinkedHashMap<>();
        private Map<String, BigDecimal> ganancia = new LinkedHashMap<>();

        public long getTotalVentas() {
            return totalVentas;
        }

        public void setTotalVentas(long totalVentas) {
            this.totalVentas = totalVentas;
        }

        public Map<String, BigDecimal> getTotalVendido() {
            return totalVendido;
        }

        public void setTotalVendido(Map<String, BigDecimal> totalVendido) {
            this.totalVendido = totalVendido;
        }

        public Map<String, BigDecimal> getTotalCobrado() {
            return totalCobrado;
        }

        public void setTotalCobrado(Map<String, BigDecimal> totalCobrado) {
            this.totalCobrado = totalCobrado;
        }

        public Map<String, BigDecimal> getGanancia() {
            return ganancia;
        }

        public void setGanancia(Map<String, BigDecimal> ganancia) {
            this.ganancia = ganancia;
        }
    }

    public static class Monthly {
        private int year;
        private int month;
        private Map<String, BigDecimal> vendido = new LinkedHashMap<>();
        private Map<String, BigDecimal> cobrado = new LinkedHashMap<>();
        private Map<String, BigDecimal> ganancia = new LinkedHashMap<>();

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

        public Map<String, BigDecimal> getVendido() {
            return vendido;
        }

        public void setVendido(Map<String, BigDecimal> vendido) {
            this.vendido = vendido;
        }

        public Map<String, BigDecimal> getCobrado() {
            return cobrado;
        }

        public void setCobrado(Map<String, BigDecimal> cobrado) {
            this.cobrado = cobrado;
        }

        public Map<String, BigDecimal> getGanancia() {
            return ganancia;
        }

        public void setGanancia(Map<String, BigDecimal> ganancia) {
            this.ganancia = ganancia;
        }
    }

    public static class TopClient {
        private Long clientId;
        private String clientName;
        private long totalVentas;
        private Map<String, BigDecimal> totalVendido = new LinkedHashMap<>();
        private Map<String, BigDecimal> totalCobrado = new LinkedHashMap<>();

        public Long getClientId() {
            return clientId;
        }

        public void setClientId(Long clientId) {
            this.clientId = clientId;
        }

        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        public long getTotalVentas() {
            return totalVentas;
        }

        public void setTotalVentas(long totalVentas) {
            this.totalVentas = totalVentas;
        }

        public Map<String, BigDecimal> getTotalVendido() {
            return totalVendido;
        }

        public void setTotalVendido(Map<String, BigDecimal> totalVendido) {
            this.totalVendido = totalVendido;
        }

        public Map<String, BigDecimal> getTotalCobrado() {
            return totalCobrado;
        }

        public void setTotalCobrado(Map<String, BigDecimal> totalCobrado) {
            this.totalCobrado = totalCobrado;
        }
    }

    public static class ProductTypeSales {
        private String tipo;
        private long totalVentas;
        private Map<String, BigDecimal> totalVendido = new LinkedHashMap<>();

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        public long getTotalVentas() {
            return totalVentas;
        }

        public void setTotalVentas(long totalVentas) {
            this.totalVentas = totalVentas;
        }

        public Map<String, BigDecimal> getTotalVendido() {
            return totalVendido;
        }

        public void setTotalVendido(Map<String, BigDecimal> totalVendido) {
            this.totalVendido = totalVendido;
        }
    }

    public static class Clientes {
        private long totalClientes;
        private long activos;

        public long getTotalClientes() {
            return totalClientes;
        }

        public void setTotalClientes(long totalClientes) {
            this.totalClientes = totalClientes;
        }

        public long getActivos() {
            return activos;
        }

        public void setActivos(long activos) {
            this.activos = activos;
        }
    }
}
