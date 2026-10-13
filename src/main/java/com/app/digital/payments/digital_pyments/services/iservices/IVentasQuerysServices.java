package com.app.digital.payments.digital_pyments.services.iservices;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.digital.payments.digital_pyments.models.dtos.DashboardStatsDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;

public interface IVentasQuerysServices {

    // obtener ventas por descripcion de producto (paginado)
    Page<SaleResponseDto> getProductsByDescription(String description, Pageable pageable);

    // obtener ventas por tipo de producto (paginado)
    Page<SaleResponseDto> getAllVentasByProduct(String productTypeName, Pageable pageable);

    // obtener ventas de un cliente (paginado)
    Page<SaleResponseDto> getProductsByClientId(Long clientId, Pageable pageable);

    // obtener ventas de un cliente no pagadas (paginado)
    Page<SaleResponseDto> getProductsNoCompletedByClient(Long clientId, Pageable pageable);

    // obtener ventas pagadas por cliente (paginado)
    Page<SaleResponseDto> getProductsCompletedByClient(Long clientId, Pageable pageable);

    // obtener la cantidad de ventas pagadas por cliente
    Long getCantidadProductsCompletedByClient(Long clientId);

    // obtener la cantidad de ventas no pagadas por cliente
    Long getCantidadProductsNotCompletedByClient(Long clientId);

    // obtener monto total de ventas pagadas en un año especifico (por tipo de producto)
    BigDecimal getMontoTotalProductsCompletedByYear(int year, String productType);

    // obtener el total del valor de todas las ventas hechas en un año especifico (por tipo de producto)
    BigDecimal getMontoTotalProductsByYear(int year, String productType);

    // obtener monto total cobrado de las ventas en un año especifico (por tipo de producto)
    BigDecimal getTotalCollectProductsByYear(int year, String productType);

    // obtener monto total de ventas pagadas en un mes especifico (por tipo de producto)
    BigDecimal getMontoTotalProductsCompletedByMonth(int month, int year, String productType);

    // obtener el total del valor de todas las ventas hechas en un mes especifico (por tipo de producto)
    BigDecimal getMontoTotalProductsByMonth(int month, int year, String productType);

    // obtener monto total cobrado de las ventas en un mes especifico (por tipo de producto)
    BigDecimal getTotalCollectProductsByMonth(int month, int year, String productType);

    // obtener ganancias totales de un mes especifico (por tipo de producto)
    BigDecimal findTotalProfitOfProductsByMonthAndYear(int month, int year, String productType);

    // obtener ganancias totales de un año especifico (por tipo de producto)
    BigDecimal findTotalProfitOfProductsByYear(int year, String productType);

    // obtener ventas con cuotas pendientes vencidas o por vencer (paginado)
    Page<SaleResponseDto> getSalesWithPendingFees(Pageable pageable);

    // obtener estadisticas de ventas por mes/año
    Map<String, Object> getSalesStats(int year, int month);

    // obtener estadisticas de ventas por mes/año, con filtro opcional por tipo de producto
    Map<String, Object> getSalesStats(int year, int month, String productTypeName);

    // obtener estadisticas de clientes
    Map<String, Object> getClientStats();

    // obtener todas las estadisticas del dashboard para un mes/año
    DashboardStatsDto getDashboardStats(int year, int month);
}
