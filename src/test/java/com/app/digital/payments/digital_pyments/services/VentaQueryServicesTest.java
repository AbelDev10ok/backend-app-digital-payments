package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.models.dtos.DashboardStatsDto;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;

class VentaQueryServicesTest {

    @Mock
    private ISaleRepository saleRepository;

    @Mock
    private IClientRepository clientRepository;

    private VentaQueryServices ventaQueryServices;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ventaQueryServices = new VentaQueryServices(saleRepository, clientRepository);

        // Filas agrupadas por moneda: (moneda, monto)
        when(saleRepository.findTotalSalesAmountByMonthAndYearGrouped(anyInt(), anyInt()))
                .thenReturn(List.<Object[]>of(new Object[] { "ARS", new BigDecimal("1000") }));
        when(saleRepository.findTotalCollectedAmountByMonthAndYearGrouped(anyInt(), anyInt()))
                .thenReturn(List.<Object[]>of(new Object[] { "ARS", new BigDecimal("700") }));
        when(saleRepository.findTotalProfitByMonthAndYearGrouped(anyInt(), anyInt()))
                .thenReturn(List.<Object[]>of(new Object[] { "ARS", new BigDecimal("300") }));
        when(saleRepository.findTotalRefundsByMonthAndYearGrouped(anyInt(), anyInt()))
                .thenReturn(List.<Object[]>of(new Object[] { "ARS", new BigDecimal("50") }));
        when(saleRepository.sumRemainingAmountByStatusAndMonthYearGrouped(any(SaleStatus.class), anyInt(), anyInt()))
                .thenReturn(List.<Object[]>of(new Object[] { "ARS", new BigDecimal("250") }));
        when(saleRepository.sumRemainingAmountByStatusGrouped(any(SaleStatus.class)))
                .thenReturn(List.<Object[]>of(new Object[] { "ARS", new BigDecimal("900") }));
        // Filas agrupadas: (moneda, estado, cantidad)
        when(saleRepository.countSalesByStatusInMonthGrouped(anyInt(), anyInt())).thenReturn(List.<Object[]>of(
                new Object[] { "ARS", SaleStatus.COMPLETED, 3L },
                new Object[] { "ARS", SaleStatus.ACTIVE, 2L },
                new Object[] { "ARS", SaleStatus.CANCELED, 1L }));
        // Filas agrupadas: (moneda, cantidad, monto)
        when(saleRepository.findOverdueFeesGrouped(any(LocalDate.class)))
                .thenReturn(List.<Object[]>of(new Object[] { "ARS", 2L, new BigDecimal("120") }));

        when(saleRepository.findTotalSalesAmountByYearGrouped(anyInt()))
                .thenReturn(List.<Object[]>of(new Object[] { "ARS", new BigDecimal("8000") }));
        when(saleRepository.findTotalCollectedAmountByYearGrouped(anyInt()))
                .thenReturn(List.<Object[]>of(new Object[] { "ARS", new BigDecimal("5000") }));
        when(saleRepository.findTotalProfitByYearGrouped(anyInt()))
                .thenReturn(List.<Object[]>of(new Object[] { "ARS", new BigDecimal("2500") }));
        when(saleRepository.countSalesByStatusInYearGrouped(anyInt())).thenReturn(List.<Object[]>of(
                new Object[] { "ARS", SaleStatus.COMPLETED, 20L },
                new Object[] { "ARS", SaleStatus.ACTIVE, 10L },
                new Object[] { "ARS", SaleStatus.CANCELED, 2L }));
        when(saleRepository.findDistinctCurrencies()).thenReturn(List.of("ARS", "USD"));

        when(saleRepository.findMonthlySalesSeriesGrouped(any(LocalDate.class))).thenReturn(List.<Object[]>of());
        when(saleRepository.findMonthlyCollectedSeriesGrouped(any(LocalDateTime.class))).thenReturn(List.<Object[]>of());
        when(saleRepository.findTopClientsByYearGrouped(anyInt())).thenReturn(List.<Object[]>of());
        when(saleRepository.findCollectedAmountByClientInYearGrouped(anyInt())).thenReturn(List.<Object[]>of());
        when(saleRepository.findSalesByKindInMonthGrouped(anyInt(), anyInt())).thenReturn(List.<Object[]>of());

        when(clientRepository.count()).thenReturn(10L);
        when(clientRepository.countActiveClients()).thenReturn(6L);
    }

    @Test
    void getDashboardStatsEnsamblaElResumenDelMes() {
        DashboardStatsDto dto = ventaQueryServices.getDashboardStats(2026, 8, null);

        assertEquals(2026, dto.getYear());
        assertEquals(8, dto.getMonth());

        assertEquals(0, new BigDecimal("1000").compareTo(dto.getResumen().getTotalVendido().get("ARS")));
        assertEquals(0, new BigDecimal("700").compareTo(dto.getResumen().getTotalCobrado().get("ARS")));
        assertEquals(0, new BigDecimal("300").compareTo(dto.getResumen().getGanancia().get("ARS")));
        assertEquals(0, new BigDecimal("50").compareTo(dto.getResumen().getReembolsos().get("ARS")));
        assertEquals(0, new BigDecimal("250").compareTo(dto.getResumen().getPendienteDelMes().get("ARS")));
        assertEquals(0, new BigDecimal("900").compareTo(dto.getResumen().getDeudaTotal().get("ARS")));

        assertEquals(6, dto.getResumen().getTotalVentas());
        assertEquals(3, dto.getResumen().getCompletadas());
        assertEquals(2, dto.getResumen().getPendientes());
        assertEquals(1, dto.getResumen().getCanceladas());
        assertEquals(2, dto.getResumen().getCuotasVencidas().getCantidad());
        assertEquals(0, new BigDecimal("120").compareTo(dto.getResumen().getCuotasVencidas().getMonto().get("ARS")));

        assertEquals(32, dto.getAnual().getTotalVentas());
        assertEquals(0, new BigDecimal("8000").compareTo(dto.getAnual().getTotalVendido().get("ARS")));
        assertEquals(0, new BigDecimal("5000").compareTo(dto.getAnual().getTotalCobrado().get("ARS")));
        assertEquals(0, new BigDecimal("2500").compareTo(dto.getAnual().getGanancia().get("ARS")));

        assertEquals(10, dto.getClientes().getTotalClientes());
        assertEquals(6, dto.getClientes().getActivos());
    }

    @Test
    void getDashboardStatsNoSumaMonedasDistintas() {
        // Dos monedas en el mismo mes: cada monto queda etiquetado por moneda,
        // jamás se suman entre sí.
        when(saleRepository.findTotalSalesAmountByMonthAndYearGrouped(anyInt(), anyInt()))
                .thenReturn(List.<Object[]>of(
                        new Object[] { "ARS", new BigDecimal("150000") },
                        new Object[] { "USD", new BigDecimal("320") }));
        when(saleRepository.findOverdueFeesGrouped(any(LocalDate.class)))
                .thenReturn(List.<Object[]>of(
                        new Object[] { "ARS", 1L, new BigDecimal("100") },
                        new Object[] { "USD", 1L, new BigDecimal("20") }));

        DashboardStatsDto dto = ventaQueryServices.getDashboardStats(2026, 8, null);

        assertEquals(2, dto.getResumen().getTotalVendido().size());
        assertEquals(0, new BigDecimal("150000").compareTo(dto.getResumen().getTotalVendido().get("ARS")));
        assertEquals(0, new BigDecimal("320").compareTo(dto.getResumen().getTotalVendido().get("USD")));

        assertEquals(2, dto.getResumen().getCuotasVencidas().getCantidad());
        assertEquals(0, new BigDecimal("100").compareTo(dto.getResumen().getCuotasVencidas().getMonto().get("ARS")));
        assertEquals(0, new BigDecimal("20").compareTo(dto.getResumen().getCuotasVencidas().getMonto().get("USD")));
    }

    @Test
    void getDashboardStatsCalculaSerieYTopClientes() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        // Filas: (año, mes, moneda, vendido, ganancia) / (año, mes, moneda, cobrado)
        when(saleRepository.findMonthlySalesSeriesGrouped(any(LocalDate.class))).thenReturn(List.<Object[]>of(
                new Object[] { year, month, "ARS", new BigDecimal("900"), new BigDecimal("250") }));
        when(saleRepository.findMonthlyCollectedSeriesGrouped(any(LocalDateTime.class))).thenReturn(List.<Object[]>of(
                new Object[] { year, month, "ARS", new BigDecimal("600") }));
        // Filas: (clienteId, nombre, moneda, cantidad, vendido) / (clienteId, moneda, cobrado)
        when(saleRepository.findTopClientsByYearGrouped(anyInt())).thenReturn(List.<Object[]>of(
                new Object[] { 1L, "Juan", "ARS", 5L, new BigDecimal("48000") }));
        when(saleRepository.findCollectedAmountByClientInYearGrouped(anyInt())).thenReturn(List.<Object[]>of(
                new Object[] { 1L, "ARS", new BigDecimal("30000") }));
        // Filas: (kind, moneda, cantidad, vendido)
        when(saleRepository.findSalesByKindInMonthGrouped(anyInt(), anyInt())).thenReturn(List.<Object[]>of(
                new Object[] { SaleKind.VENTA, "ARS", 8L, new BigDecimal("90000") },
                new Object[] { SaleKind.PRESTAMO, "ARS", 4L, new BigDecimal("30000") }));

        DashboardStatsDto dto = ventaQueryServices.getDashboardStats(year, month, null);

        assertEquals(12, dto.getSerie12Meses().size());
        DashboardStatsDto.Monthly current = dto.getSerie12Meses().stream()
                .filter(m -> m.getYear() == year && m.getMonth() == month)
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("900").compareTo(current.getVendido().get("ARS")));
        assertEquals(0, new BigDecimal("600").compareTo(current.getCobrado().get("ARS")));
        assertEquals(0, new BigDecimal("250").compareTo(current.getGanancia().get("ARS")));

        assertEquals(1, dto.getTopClientes().size());
        assertEquals("Juan", dto.getTopClientes().get(0).getClientName());
        assertEquals(5, dto.getTopClientes().get(0).getTotalVentas());
        assertEquals(0, new BigDecimal("48000").compareTo(dto.getTopClientes().get(0).getTotalVendido().get("ARS")));
        assertEquals(0, new BigDecimal("30000").compareTo(dto.getTopClientes().get(0).getTotalCobrado().get("ARS")));

        assertEquals(2, dto.getPorTipoProducto().size());
        assertEquals("VENTA", dto.getPorTipoProducto().get(0).getTipo());
        assertEquals("PRESTAMO", dto.getPorTipoProducto().get(1).getTipo());
        assertEquals(0, new BigDecimal("90000").compareTo(dto.getPorTipoProducto().get(0).getTotalVendido().get("ARS")));
    }

    @Test
    void getDashboardStatsOrdenaTopClientesPorCantidadSinCruzarMonedas() {
        LocalDate today = LocalDate.now();
        // El cliente 2 tiene más transacciones pero en USD: el orden es por
        // cantidad (nunca por una suma que cruzaría monedas).
        when(saleRepository.findTopClientsByYearGrouped(anyInt())).thenReturn(List.<Object[]>of(
                new Object[] { 1L, "Ana", "ARS", 2L, new BigDecimal("500000") },
                new Object[] { 2L, "Luis", "USD", 7L, new BigDecimal("900") }));

        DashboardStatsDto dto = ventaQueryServices.getDashboardStats(today.getYear(), today.getMonthValue(), null);

        assertEquals(2, dto.getTopClientes().size());
        assertEquals("Luis", dto.getTopClientes().get(0).getClientName());
        assertEquals("Ana", dto.getTopClientes().get(1).getClientName());
        assertTrue(dto.getTopClientes().get(0).getTotalVendido().containsKey("USD"));
    }

    @Test
    void getDashboardStatsFiltraPorMonedaAcotandoMontosYConteos() {
        when(saleRepository.findTotalSalesAmountByMonthAndYearGrouped(anyInt(), anyInt()))
                .thenReturn(List.<Object[]>of(
                        new Object[] { "ARS", new BigDecimal("150000") },
                        new Object[] { "USD", new BigDecimal("320") }));
        when(saleRepository.countSalesByStatusInMonthGrouped(anyInt(), anyInt())).thenReturn(List.<Object[]>of(
                new Object[] { "ARS", SaleStatus.COMPLETED, 3L },
                new Object[] { "USD", SaleStatus.COMPLETED, 1L },
                new Object[] { "USD", SaleStatus.ACTIVE, 1L }));

        DashboardStatsDto dto = ventaQueryServices.getDashboardStats(2026, 8, "usd");

        // Conteos acotados a USD (2 operaciones), no las 6 de todas las monedas
        assertEquals(2, dto.getResumen().getTotalVentas());
        assertEquals(1, dto.getResumen().getCompletadas());
        assertEquals(1, dto.getResumen().getPendientes());
        assertEquals(0, dto.getResumen().getCanceladas());
        // Montos solo de USD
        assertEquals(1, dto.getResumen().getTotalVendido().size());
        assertEquals(0, new BigDecimal("320").compareTo(dto.getResumen().getTotalVendido().get("USD")));
        // El selector conoce todas las monedas aunque el filtro esté activo
        assertEquals(List.of("ARS", "USD"), dto.getMonedasDisponibles());
    }

    @Test
    void getDashboardStatsConTodasIncluyeTodasLasMonedas() {
        when(saleRepository.findTotalSalesAmountByMonthAndYearGrouped(anyInt(), anyInt()))
                .thenReturn(List.<Object[]>of(
                        new Object[] { "ARS", new BigDecimal("150000") },
                        new Object[] { "USD", new BigDecimal("320") }));

        DashboardStatsDto dto = ventaQueryServices.getDashboardStats(2026, 8, "TODAS");

        assertEquals(2, dto.getResumen().getTotalVendido().size());
        assertEquals(6, dto.getResumen().getTotalVentas());
        assertEquals(List.of("ARS", "USD"), dto.getMonedasDisponibles());
    }

    @Test
    void getDashboardStatsInformaLaDeudaTotalSinDependerDelMes() {
        // La deuda de la tarjeta es la de todas las ventas activas, no la del mes
        // mirado: septiembre y octubre devuelven el mismo número...
        when(saleRepository.sumRemainingAmountByStatusGrouped(any(SaleStatus.class)))
                .thenReturn(List.<Object[]>of(
                        new Object[] { "ARS", new BigDecimal("125000") },
                        new Object[] { "USD", new BigDecimal("80") }));
        // ...mientras el pendiente del mes sí cambia (es otro dato)
        when(saleRepository.sumRemainingAmountByStatusAndMonthYearGrouped(any(SaleStatus.class), anyInt(), anyInt()))
                .thenReturn(List.<Object[]>of(new Object[] { "ARS", new BigDecimal("5000") }));

        DashboardStatsDto septiembre = ventaQueryServices.getDashboardStats(2026, 9, null);
        DashboardStatsDto octubre = ventaQueryServices.getDashboardStats(2026, 10, null);

        assertEquals(0, new BigDecimal("125000").compareTo(septiembre.getResumen().getDeudaTotal().get("ARS")));
        assertEquals(0, new BigDecimal("80").compareTo(septiembre.getResumen().getDeudaTotal().get("USD")));
        assertEquals(0, new BigDecimal("125000").compareTo(octubre.getResumen().getDeudaTotal().get("ARS")));
        assertEquals(0, new BigDecimal("5000").compareTo(octubre.getResumen().getPendienteDelMes().get("ARS")));
        // La deuda es la de las ventas activas (nunca la de las canceladas)
        verify(saleRepository, times(2)).sumRemainingAmountByStatusGrouped(SaleStatus.ACTIVE);
    }

    @Test
    void getDashboardStatsAcotaLaDeudaTotalALaMonedaFiltrada() {
        when(saleRepository.sumRemainingAmountByStatusGrouped(any(SaleStatus.class)))
                .thenReturn(List.<Object[]>of(
                        new Object[] { "ARS", new BigDecimal("125000") },
                        new Object[] { "USD", new BigDecimal("80") }));

        DashboardStatsDto dto = ventaQueryServices.getDashboardStats(2026, 8, "usd");

        assertEquals(1, dto.getResumen().getDeudaTotal().size());
        assertEquals(0, new BigDecimal("80").compareTo(dto.getResumen().getDeudaTotal().get("USD")));
    }
}
