package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.dtos.DashboardStatsDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.services.iservices.IVentasQuerysServices;
import com.app.digital.payments.digital_pyments.utils.CurrencyUtils;
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

    // Método para obtener todas las estadísticas del dashboard para un mes/año.
    // currency = null o "TODAS" muestra todas las monedas; un código ISO 4217
    // acota montos Y conteos a esa moneda (nunca se suman monedas distintas).
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(int year, int month, String currency) {
        LocalDate today = LocalDate.now();
        String filtro = filtroMoneda(currency);
        DashboardStatsDto dto = new DashboardStatsDto();
        dto.setYear(year);
        dto.setMonth(month);

        // Monedas presentes en los datos (para el selector): se calculan sin filtrar
        dto.setMonedasDisponibles(saleRepository.findDistinctCurrencies().stream()
                .filter(m -> m != null)
                .map(m -> m.trim().toUpperCase())
                .distinct()
                .sorted()
                .collect(Collectors.toList()));

        // ----- Resumen del mes (montos agrupados por moneda) -----
        DashboardStatsDto.Resumen resumen = dto.getResumen();
        resumen.setTotalVendido(soloMoneda(CurrencyUtils.montosPorMoneda(saleRepository.findTotalSalesAmountByMonthAndYearGrouped(month, year)), filtro));
        resumen.setTotalCobrado(soloMoneda(CurrencyUtils.montosPorMoneda(saleRepository.findTotalCollectedAmountByMonthAndYearGrouped(month, year)), filtro));
        resumen.setGanancia(soloMoneda(CurrencyUtils.montosPorMoneda(saleRepository.findTotalProfitByMonthAndYearGrouped(month, year)), filtro));
        resumen.setReembolsos(soloMoneda(CurrencyUtils.montosPorMoneda(saleRepository.findTotalRefundsByMonthAndYearGrouped(month, year)), filtro));
        resumen.setPendienteDelMes(soloMoneda(CurrencyUtils.montosPorMoneda(saleRepository.sumRemainingAmountByStatusAndMonthYearGrouped(SaleStatus.ACTIVE, year, month)), filtro));
        // Deuda real a hoy: no depende del mes consultado (sólo del filtro de moneda)
        resumen.setDeudaTotal(soloMoneda(CurrencyUtils.montosPorMoneda(saleRepository.sumRemainingAmountByStatusGrouped(SaleStatus.ACTIVE)), filtro));

        // Filas: (moneda, estado, cantidad) — el filtro acota también los conteos
        long completadas = 0;
        long pendientes = 0;
        long canceladas = 0;
        for (Object[] row : saleRepository.countSalesByStatusInMonthGrouped(year, month)) {
            if (!coincide((String) row[0], filtro)) {
                continue;
            }
            SaleStatus status = (SaleStatus) row[1];
            long count = row[2] != null ? ((Number) row[2]).longValue() : 0;
            if (status == SaleStatus.COMPLETED) {
                completadas += count;
            } else if (status == SaleStatus.ACTIVE) {
                pendientes += count;
            } else if (status == SaleStatus.CANCELED) {
                canceladas += count;
            }
        }
        resumen.setCompletadas(completadas);
        resumen.setPendientes(pendientes);
        resumen.setCanceladas(canceladas);
        resumen.setTotalVentas(completadas + pendientes + canceladas);

        // Filas agrupadas: (moneda, cantidad, monto). La cantidad es global;
        // los montos nunca se suman entre monedas distintas. Alcance: a hoy.
        for (Object[] row : saleRepository.findOverdueFeesGrouped(today)) {
            if (!coincide((String) row[0], filtro)) {
                continue;
            }
            resumen.getCuotasVencidas().setCantidad(
                    resumen.getCuotasVencidas().getCantidad() + (row[1] != null ? ((Number) row[1]).longValue() : 0));
            CurrencyUtils.acumular(resumen.getCuotasVencidas().getMonto(), (String) row[0],
                    row[2] instanceof BigDecimal monto ? monto : null);
        }

        // ----- Anual -----
        DashboardStatsDto.Anual anual = dto.getAnual();
        anual.setTotalVendido(soloMoneda(CurrencyUtils.montosPorMoneda(saleRepository.findTotalSalesAmountByYearGrouped(year)), filtro));
        anual.setTotalCobrado(soloMoneda(CurrencyUtils.montosPorMoneda(saleRepository.findTotalCollectedAmountByYearGrouped(year)), filtro));
        anual.setGanancia(soloMoneda(CurrencyUtils.montosPorMoneda(saleRepository.findTotalProfitByYearGrouped(year)), filtro));

        long yearCompletadas = 0;
        long yearPendientes = 0;
        long yearCanceladas = 0;
        for (Object[] row : saleRepository.countSalesByStatusInYearGrouped(year)) {
            if (!coincide((String) row[0], filtro)) {
                continue;
            }
            SaleStatus status = (SaleStatus) row[1];
            long count = row[2] != null ? ((Number) row[2]).longValue() : 0;
            if (status == SaleStatus.COMPLETED) {
                yearCompletadas += count;
            } else if (status == SaleStatus.ACTIVE) {
                yearPendientes += count;
            } else if (status == SaleStatus.CANCELED) {
                yearCanceladas += count;
            }
        }
        anual.setTotalVentas(yearCompletadas + yearPendientes + yearCanceladas);

        // ----- Serie de 12 meses hasta el mes seleccionado -----
        LocalDate first = LocalDate.of(year, month, 1).minusMonths(11);
        Map<Integer, DashboardStatsDto.Monthly> byMonth = new TreeMap<>();
        for (int i = 0; i < 12; i++) {
            LocalDate monthDate = first.plusMonths(i);
            int key = monthDate.getYear() * 100 + monthDate.getMonthValue();
            DashboardStatsDto.Monthly entry = new DashboardStatsDto.Monthly();
            entry.setYear(monthDate.getYear());
            entry.setMonth(monthDate.getMonthValue());
            byMonth.put(key, entry);
        }
        for (Object[] row : saleRepository.findMonthlySalesSeriesGrouped(first)) {
            int key = ((Number) row[0]).intValue() * 100 + ((Number) row[1]).intValue();
            DashboardStatsDto.Monthly entry = byMonth.get(key);
            if (entry != null) {
                // Filas: (año, mes, moneda, vendido, ganancia) — sin sumar monedas
                if (!coincide((String) row[2], filtro)) {
                    continue;
                }
                CurrencyUtils.acumular(entry.getVendido(), (String) row[2], row[3] instanceof BigDecimal monto ? monto : null);
                CurrencyUtils.acumular(entry.getGanancia(), (String) row[2], row[4] instanceof BigDecimal monto ? monto : null);
            }
        }
        for (Object[] row : saleRepository.findMonthlyCollectedSeriesGrouped(first.atStartOfDay())) {
            int key = ((Number) row[0]).intValue() * 100 + ((Number) row[1]).intValue();
            DashboardStatsDto.Monthly entry = byMonth.get(key);
            if (entry != null) {
                if (!coincide((String) row[2], filtro)) {
                    continue;
                }
                CurrencyUtils.acumular(entry.getCobrado(), (String) row[2], row[3] instanceof BigDecimal monto ? monto : null);
            }
        }
        dto.setSerie12Meses(new ArrayList<>(byMonth.values()));

        // ----- Top clientes del año -----
        // Filas agrupadas por cliente y moneda: los montos se muestran por moneda
        // y el orden es por cantidad de transacciones (un ranking por monto
        // cruzaría monedas).
        Map<Long, Map<String, BigDecimal>> collectedByClient = new HashMap<>();
        for (Object[] row : saleRepository.findCollectedAmountByClientInYearGrouped(year)) {
            if (!coincide((String) row[1], filtro)) {
                continue;
            }
            Long clientId = ((Number) row[0]).longValue();
            Map<String, BigDecimal> montos = collectedByClient.computeIfAbsent(clientId, id -> new LinkedHashMap<>());
            CurrencyUtils.acumular(montos, (String) row[1], row[2] instanceof BigDecimal monto ? monto : null);
        }

        Map<Long, DashboardStatsDto.TopClient> byClient = new LinkedHashMap<>();
        Map<Long, Long> ventasPorCliente = new HashMap<>();
        for (Object[] row : saleRepository.findTopClientsByYearGrouped(year)) {
            if (!coincide((String) row[2], filtro)) {
                continue;
            }
            Long clientId = ((Number) row[0]).longValue();
            DashboardStatsDto.TopClient topClient = byClient.get(clientId);
            if (topClient == null) {
                topClient = new DashboardStatsDto.TopClient();
                topClient.setClientId(clientId);
                topClient.setClientName((String) row[1]);
                byClient.put(clientId, topClient);
            }
            ventasPorCliente.merge(clientId, row[3] != null ? ((Number) row[3]).longValue() : 0L, Long::sum);
            CurrencyUtils.acumular(topClient.getTotalVendido(), (String) row[2], row[4] instanceof BigDecimal monto ? monto : null);
        }

        List<DashboardStatsDto.TopClient> topClients = new ArrayList<>(byClient.values());
        topClients.sort(Comparator.comparingLong(
                (DashboardStatsDto.TopClient tc) -> ventasPorCliente.getOrDefault(tc.getClientId(), 0L)).reversed());
        for (DashboardStatsDto.TopClient topClient : topClients) {
            topClient.setTotalVentas(ventasPorCliente.getOrDefault(topClient.getClientId(), 0L));
            topClient.setTotalCobrado(collectedByClient.getOrDefault(topClient.getClientId(), new LinkedHashMap<>()));
        }
        dto.setTopClientes(topClients.subList(0, Math.min(5, topClients.size())));

        // ----- Ventas por tipo de transacción (kind) en el mes -----
        Map<String, DashboardStatsDto.ProductTypeSales> byKind = new LinkedHashMap<>();
        for (Object[] row : saleRepository.findSalesByKindInMonthGrouped(year, month)) {
            if (!coincide((String) row[1], filtro)) {
                continue;
            }
            String tipo = ((SaleKind) row[0]).name();
            DashboardStatsDto.ProductTypeSales entry = byKind.get(tipo);
            if (entry == null) {
                entry = new DashboardStatsDto.ProductTypeSales();
                entry.setTipo(tipo);
                byKind.put(tipo, entry);
            }
            entry.setTotalVentas(entry.getTotalVentas() + (row[2] != null ? ((Number) row[2]).longValue() : 0));
            CurrencyUtils.acumular(entry.getTotalVendido(), (String) row[1], row[3] instanceof BigDecimal monto ? monto : null);
        }
        dto.setPorTipoProducto(new ArrayList<>(byKind.values()));

        // ----- Clientes (conteos globales: no dependen de la moneda) -----
        dto.getClientes().setTotalClientes(clientRepository.count());
        dto.getClientes().setActivos(clientRepository.countActiveClients());

        return dto;
    }

    /** Normaliza el filtro de moneda del dashboard (null = todas las monedas). */
    private static String filtroMoneda(String currency) {
        if (currency == null || currency.isBlank() || "TODAS".equalsIgnoreCase(currency.trim())) {
            return null;
        }
        return currency.trim().toUpperCase();
    }

    /** ¿La moneda de una fila coincide con el filtro? (null = todas). */
    private static boolean coincide(String moneda, String filtro) {
        return filtro == null || filtro.equals(CurrencyUtils.orDefault(moneda, null));
    }

    /** Deja solo la moneda filtrada en un mapa de montos (null = todas). */
    private static Map<String, BigDecimal> soloMoneda(Map<String, BigDecimal> montos, String filtro) {
        if (filtro == null) {
            return montos;
        }
        Map<String, BigDecimal> resultado = new LinkedHashMap<>();
        BigDecimal monto = montos.get(filtro);
        if (monto != null) {
            resultado.put(filtro, monto);
        }
        return resultado;
    }
}
