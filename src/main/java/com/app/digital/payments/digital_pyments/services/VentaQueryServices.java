package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.dtos.DashboardStatsDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.services.iservices.IVentasQuerysServices;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;

import static com.app.digital.payments.digital_pyments.mappers.SaleMapper.convertirAVentaResponseDTO;


@Service
public class VentaQueryServices implements IVentasQuerysServices {

    private final ISaleRepository saleRepository;

    private final IClientRepository clientRepository;

    public VentaQueryServices(ISaleRepository saleRepository, IClientRepository clientRepository) {
        this.saleRepository = saleRepository;
        this.clientRepository = clientRepository;
    }

    // obtener venta por descripción de producto
    @Transactional(readOnly = true)
    public Page<SaleResponseDto> getProductsByDescription(String description, Pageable pageable) {
        return saleRepository.findByDescriptionProductContainingIgnoreCase(description, pageable)
                .map(sale -> convertirAVentaResponseDTO(sale));
    }

    @Transactional(readOnly = true)
    public Page<SaleResponseDto> getAllVentasByProduct(String productTypeName, Pageable pageable) {
        return saleRepository.findByKindAndStatusNot(SaleKind.fromString(productTypeName), SaleStatus.CANCELED, pageable)
                .map(sale -> convertirAVentaResponseDTO(sale));
    }

    // Obtener productos de un cliente
    @Transactional(readOnly = true)
    public Page<SaleResponseDto> getProductsByClientId(Long clientId, Pageable pageable) {
        return saleRepository.findByClientIdAndStatusNotOrderByDateCreationDesc(
                clientId,
                SaleStatus.CANCELED,
                pageable
        ).map(sale -> convertirAVentaResponseDTO(sale));
    }

    // Obtener productos de un cliente no pagados
    @Transactional(readOnly = true)
    public Page<SaleResponseDto> getProductsNoCompletedByClient(Long clientId, Pageable pageable) {
        return saleRepository.findNotCompletedByClientIdOrderByDateCreationDesc(
                clientId, SaleStatus.COMPLETED, SaleStatus.CANCELED, pageable)
                .map(sale -> convertirAVentaResponseDTO(sale));
    }

    // Obtener productos pagados por cliente
    @Transactional(readOnly = true)
    public Page<SaleResponseDto> getProductsCompletedByClient(Long clientId, Pageable pageable) {
        return saleRepository.findCompletedByClientIdOrderByDateCreationDesc(clientId, SaleStatus.COMPLETED, pageable)
                .map(sale -> convertirAVentaResponseDTO(sale));
    }

    // Obtener la cantidad de productos pagados por cliente
    @Transactional(readOnly = true)
    public Long getCantidadProductsCompletedByClient(Long clientId) {
        return saleRepository.countCompletedByClientId(clientId, SaleStatus.COMPLETED);
    }

    // Obtener la cantidad de productos no pagados por cliente
    @Transactional(readOnly = true)
    public Long getCantidadProductsNotCompletedByClient(Long clientId) {
        return saleRepository.countNotCompletedByClientId(clientId, SaleStatus.COMPLETED, SaleStatus.CANCELED);
    }

    // obtener monto total de productos pagados un año especifico (de cualquier tipo)
    @Transactional(readOnly = true)
    public BigDecimal getMontoTotalProductsCompletedByYear(int year, String productType) {
        return saleRepository.findTotalPaidSalesAmountByYear(year, SaleKind.fromString(productType)).orElse(BigDecimal.ZERO);
    }

    // obtener el total de valor de todos los productos hechos un año especifico (de cualquier tipo)
    @Transactional(readOnly = true)
    public BigDecimal getMontoTotalProductsByYear(int year, String productType) {
        return saleRepository.findTotalSalesAmountByYear(year, SaleKind.fromString(productType)).orElse(BigDecimal.ZERO);
    }

    // obtener el monto total que vamos cobrando de los productos un año específico (de cualquier tipo)
    @Transactional(readOnly = true)
    public BigDecimal getTotalCollectProductsByYear(int year, String productType) {
        BigDecimal totalFees = saleRepository.findTotalCollectedAmountByYear(year, SaleKind.fromString(productType)).orElse(BigDecimal.ZERO);
        BigDecimal totalRefunds = saleRepository.findTotalRefundsByYear(year, SaleKind.fromString(productType)).orElse(BigDecimal.ZERO);
        return totalFees.subtract(totalRefunds);
    }

    // obtener monto total de productos pagados un mes específico (de cualquier tipo)
    @Transactional(readOnly = true)
    public BigDecimal getMontoTotalProductsCompletedByMonth(int month, int year, String productType) {
        return saleRepository.findTotalPaidSalesAmountByMonthAndYear(month, year, SaleKind.fromString(productType)).orElse(BigDecimal.ZERO);
    }

    // obtener el total del valor de todos los productos hechos un mes específico (de cualquier tipo)
    @Transactional(readOnly = true)
    public BigDecimal getMontoTotalProductsByMonth(int month, int year, String productType) {
        return saleRepository.findTotalSalesAmountByMonthAndYear(month, year, SaleKind.fromString(productType)).orElse(BigDecimal.ZERO);
    }

    // obtener monto total que vamos cobrando de los productos un mes específico (de cualquier tipo)
    @Transactional(readOnly = true)
    public BigDecimal getTotalCollectProductsByMonth(int month, int year, String productType) {
        BigDecimal totalFees = saleRepository.findTotalCollectedAmountByMonthAndYear(month, year, SaleKind.fromString(productType)).orElse(BigDecimal.ZERO);
        BigDecimal totalRefunds = saleRepository.findTotalRefundsByMonthAndYear(month, year, SaleKind.fromString(productType)).orElse(BigDecimal.ZERO);
        return totalFees.subtract(totalRefunds);
    }

    // obtener ganancias totales de un mes específico (de cualquier tipo)
    @Transactional(readOnly = true)
    public BigDecimal findTotalProfitOfProductsByMonthAndYear(int month, int year, String productType) {
        return saleRepository.findTotalProfitByMonthAndYear(month, year, SaleKind.fromString(productType)).orElse(BigDecimal.ZERO);
    }

    // obtener ganancias totales de un año especifico (de cualquier tipo)
    @Transactional(readOnly = true)
    public BigDecimal findTotalProfitOfProductsByYear(int year, String productType) {
        return saleRepository.findTotalProfitByYear(year, SaleKind.fromString(productType)).orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public Page<SaleResponseDto> getSalesWithPendingFees(Pageable pageable) {
        return saleRepository.findSalesWithPendingFeesDueBeforeOrOn(java.time.LocalDate.now(), pageable)
                .map(sale -> convertirAVentaResponseDTO(sale));
    }

    // Método para obtener estadísticas de ventas por mes/año
    public Map<String, Object> getSalesStats(int year, int month) {
        return getSalesStats(year, month, null);
    }

    // Método para obtener estadísticas de ventas por mes/año, con filtro opcional por tipo de transacción (kind)
    public Map<String, Object> getSalesStats(int year, int month, String productTypeName) {
        Map<String, Object> stats = new HashMap<>();

        boolean filterByKind = productTypeName != null && !productTypeName.isEmpty();
        SaleKind kind = filterByKind ? SaleKind.fromString(productTypeName) : null;

        Object[] row = filterByKind
                ? saleRepository.findSalesAndProfitByMonthAndYear(month, year, kind).orElse(null)
                : saleRepository.findSalesAndProfitByMonthAndYear(month, year).orElse(null);

        BigDecimal totalSalesAmount = row != null && row[0] != null ? (BigDecimal) row[0] : BigDecimal.ZERO;
        BigDecimal totalProfit = row != null && row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;

        BigDecimal totalCollectedAmount = filterByKind
                ? saleRepository.findTotalCollectedAmountByMonthAndYear(month, year, kind).orElse(BigDecimal.ZERO)
                : saleRepository.findTotalCollectedAmountByMonthAndYear(month, year).orElse(BigDecimal.ZERO);

        stats.put("totalVendido", totalSalesAmount);
        stats.put("totalCobrado", totalCollectedAmount);
        stats.put("gananciasEsperadas", totalProfit);

        return stats;
    }

    // Método para obtener estadísticas de clientes
    public Map<String, Object> getClientStats() {
        long totalActiveClients = clientRepository.countActiveClients();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActiveClients", totalActiveClients);

        return stats;
    }

    // Método para obtener todas las estadísticas del dashboard para un mes/año
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(int year, int month) {
        LocalDate today = LocalDate.now();
        DashboardStatsDto dto = new DashboardStatsDto();
        dto.setYear(year);
        dto.setMonth(month);

        // ----- Resumen del mes -----
        DashboardStatsDto.Resumen resumen = dto.getResumen();
        resumen.setTotalVendido(saleRepository.findTotalSalesAmountByMonthAndYear(month, year).orElse(BigDecimal.ZERO));
        resumen.setTotalCobrado(saleRepository.findTotalCollectedAmountByMonthAndYear(month, year).orElse(BigDecimal.ZERO));
        resumen.setGanancia(saleRepository.findTotalProfitByMonthAndYear(month, year).orElse(BigDecimal.ZERO));
        resumen.setReembolsos(saleRepository.findTotalRefundsByMonthAndYear(month, year).orElse(BigDecimal.ZERO));
        resumen.setPendientePorCobrar(saleRepository.sumRemainingAmountByStatusAndMonthYear(SaleStatus.ACTIVE, year, month).orElse(BigDecimal.ZERO));

        long completadas = 0;
        long pendientes = 0;
        long canceladas = 0;
        for (Object[] row : saleRepository.countSalesByStatusInMonth(year, month)) {
            SaleStatus status = (SaleStatus) row[0];
            long count = ((Number) row[1]).longValue();
            if (status == SaleStatus.COMPLETED) {
                completadas = count;
            } else if (status == SaleStatus.ACTIVE) {
                pendientes = count;
            } else if (status == SaleStatus.CANCELED) {
                canceladas = count;
            }
        }
        resumen.setCompletadas(completadas);
        resumen.setPendientes(pendientes);
        resumen.setCanceladas(canceladas);
        resumen.setTotalVentas(completadas + pendientes + canceladas);

        List<Object[]> overdue = saleRepository.findOverdueFees(today);
        if (!overdue.isEmpty() && overdue.get(0)[0] != null) {
            Object[] row = overdue.get(0);
            resumen.getCuotasVencidas().setCantidad(((Number) row[0]).longValue());
            resumen.getCuotasVencidas().setMonto(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
        }

        // ----- Anual -----
        DashboardStatsDto.Anual anual = dto.getAnual();
        anual.setTotalVendido(saleRepository.findTotalSalesAmountByYear(year).orElse(BigDecimal.ZERO));
        anual.setTotalCobrado(saleRepository.findTotalCollectedAmountByYear(year).orElse(BigDecimal.ZERO));
        anual.setGanancia(saleRepository.findTotalProfitByYear(year).orElse(BigDecimal.ZERO));

        long yearCompletadas = 0;
        long yearPendientes = 0;
        long yearCanceladas = 0;
        for (Object[] row : saleRepository.countSalesByStatusInYear(year)) {
            SaleStatus status = (SaleStatus) row[0];
            long count = ((Number) row[1]).longValue();
            if (status == SaleStatus.COMPLETED) {
                yearCompletadas = count;
            } else if (status == SaleStatus.ACTIVE) {
                yearPendientes = count;
            } else if (status == SaleStatus.CANCELED) {
                yearCanceladas = count;
            }
        }
        anual.setTotalVentas(yearCompletadas + yearPendientes + yearCanceladas);

        // ----- Serie de los últimos 12 meses -----
        LocalDate first = today.minusMonths(11).withDayOfMonth(1);
        Map<Integer, DashboardStatsDto.Monthly> byMonth = new TreeMap<>();
        for (int i = 0; i < 12; i++) {
            LocalDate monthDate = first.plusMonths(i);
            int key = monthDate.getYear() * 100 + monthDate.getMonthValue();
            DashboardStatsDto.Monthly entry = new DashboardStatsDto.Monthly();
            entry.setYear(monthDate.getYear());
            entry.setMonth(monthDate.getMonthValue());
            byMonth.put(key, entry);
        }
        for (Object[] row : saleRepository.findMonthlySalesSeries(first)) {
            int key = ((Number) row[0]).intValue() * 100 + ((Number) row[1]).intValue();
            DashboardStatsDto.Monthly entry = byMonth.get(key);
            if (entry != null) {
                entry.setVendido(row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO);
                entry.setGanancia(row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO);
            }
        }
        for (Object[] row : saleRepository.findMonthlyCollectedSeries(first)) {
            int key = ((Number) row[0]).intValue() * 100 + ((Number) row[1]).intValue();
            DashboardStatsDto.Monthly entry = byMonth.get(key);
            if (entry != null) {
                entry.setCobrado(row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO);
            }
        }
        dto.setSerie12Meses(new ArrayList<>(byMonth.values()));

        // ----- Top clientes del año -----
        Map<Long, BigDecimal> collectedByClient = new HashMap<>();
        for (Object[] row : saleRepository.findCollectedAmountByClientInYear(year)) {
            collectedByClient.put(((Number) row[0]).longValue(),
                    row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
        }

        List<DashboardStatsDto.TopClient> topClients = new ArrayList<>();
        List<Object[]> topRows = saleRepository.findTopClientsByYear(year);
        for (Object[] row : topRows.subList(0, Math.min(5, topRows.size()))) {
            DashboardStatsDto.TopClient topClient = new DashboardStatsDto.TopClient();
            topClient.setClientId(((Number) row[0]).longValue());
            topClient.setClientName((String) row[1]);
            topClient.setTotalVentas(((Number) row[2]).longValue());
            topClient.setTotalVendido(row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO);
            topClient.setTotalCobrado(collectedByClient.getOrDefault(topClient.getClientId(), BigDecimal.ZERO));
            topClients.add(topClient);
        }
        dto.setTopClientes(topClients);

        // ----- Ventas por tipo de transacción (kind) en el mes -----
        List<DashboardStatsDto.ProductTypeSales> byProductType = new ArrayList<>();
        for (Object[] row : saleRepository.findSalesByKindInMonth(year, month)) {
            DashboardStatsDto.ProductTypeSales entry = new DashboardStatsDto.ProductTypeSales();
            entry.setTipo(((SaleKind) row[0]).name());
            entry.setTotalVentas(((Number) row[1]).longValue());
            entry.setTotalVendido(row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO);
            byProductType.add(entry);
        }
        dto.setPorTipoProducto(byProductType);

        // ----- Clientes -----
        dto.getClientes().setTotalClientes(clientRepository.count());
        dto.getClientes().setActivos(clientRepository.countActiveClients());

        return dto;
    }
}
