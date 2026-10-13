package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DashboardStatsDto {

    private int year;
    private int month;
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
        private BigDecimal totalVendido = BigDecimal.ZERO;
        private BigDecimal totalCobrado = BigDecimal.ZERO;
        private BigDecimal pendientePorCobrar = BigDecimal.ZERO;
        private BigDecimal ganancia = BigDecimal.ZERO;
        private BigDecimal reembolsos = BigDecimal.ZERO;
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

        public BigDecimal getTotalVendido() {
            return totalVendido;
        }

        public void setTotalVendido(BigDecimal totalVendido) {
            this.totalVendido = totalVendido;
        }

        public BigDecimal getTotalCobrado() {
            return totalCobrado;
        }

        public void setTotalCobrado(BigDecimal totalCobrado) {
            this.totalCobrado = totalCobrado;
        }

        public BigDecimal getPendientePorCobrar() {
            return pendientePorCobrar;
        }

        public void setPendientePorCobrar(BigDecimal pendientePorCobrar) {
            this.pendientePorCobrar = pendientePorCobrar;
        }

        public BigDecimal getGanancia() {
            return ganancia;
        }

        public void setGanancia(BigDecimal ganancia) {
            this.ganancia = ganancia;
        }

        public BigDecimal getReembolsos() {
            return reembolsos;
        }

        public void setReembolsos(BigDecimal reembolsos) {
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
        private BigDecimal monto = BigDecimal.ZERO;

        public long getCantidad() {
            return cantidad;
        }

        public void setCantidad(long cantidad) {
            this.cantidad = cantidad;
        }

        public BigDecimal getMonto() {
            return monto;
        }

        public void setMonto(BigDecimal monto) {
            this.monto = monto;
        }
    }

    public static class Anual {
        private long totalVentas;
        private BigDecimal totalVendido = BigDecimal.ZERO;
        private BigDecimal totalCobrado = BigDecimal.ZERO;
        private BigDecimal ganancia = BigDecimal.ZERO;

        public long getTotalVentas() {
            return totalVentas;
        }

        public void setTotalVentas(long totalVentas) {
            this.totalVentas = totalVentas;
        }

        public BigDecimal getTotalVendido() {
            return totalVendido;
        }

        public void setTotalVendido(BigDecimal totalVendido) {
            this.totalVendido = totalVendido;
        }

        public BigDecimal getTotalCobrado() {
            return totalCobrado;
        }

        public void setTotalCobrado(BigDecimal totalCobrado) {
            this.totalCobrado = totalCobrado;
        }

        public BigDecimal getGanancia() {
            return ganancia;
        }

        public void setGanancia(BigDecimal ganancia) {
            this.ganancia = ganancia;
        }
    }

    public static class Monthly {
        private int year;
        private int month;
        private BigDecimal vendido = BigDecimal.ZERO;
        private BigDecimal cobrado = BigDecimal.ZERO;
        private BigDecimal ganancia = BigDecimal.ZERO;

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

        public BigDecimal getVendido() {
            return vendido;
        }

        public void setVendido(BigDecimal vendido) {
            this.vendido = vendido;
        }

        public BigDecimal getCobrado() {
            return cobrado;
        }

        public void setCobrado(BigDecimal cobrado) {
            this.cobrado = cobrado;
        }

        public BigDecimal getGanancia() {
            return ganancia;
        }

        public void setGanancia(BigDecimal ganancia) {
            this.ganancia = ganancia;
        }
    }

    public static class TopClient {
        private Long clientId;
        private String clientName;
        private long totalVentas;
        private BigDecimal totalVendido = BigDecimal.ZERO;
        private BigDecimal totalCobrado = BigDecimal.ZERO;

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

        public BigDecimal getTotalVendido() {
            return totalVendido;
        }

        public void setTotalVendido(BigDecimal totalVendido) {
            this.totalVendido = totalVendido;
        }

        public BigDecimal getTotalCobrado() {
            return totalCobrado;
        }

        public void setTotalCobrado(BigDecimal totalCobrado) {
            this.totalCobrado = totalCobrado;
        }
    }

    public static class ProductTypeSales {
        private String tipo;
        private long totalVentas;
        private BigDecimal totalVendido = BigDecimal.ZERO;

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

        public BigDecimal getTotalVendido() {
            return totalVendido;
        }

        public void setTotalVendido(BigDecimal totalVendido) {
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
