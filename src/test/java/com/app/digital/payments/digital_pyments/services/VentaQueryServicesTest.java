package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

        when(saleRepository.findTotalSalesAmountByMonthAndYear(anyInt(), anyInt()))
                .thenReturn(Optional.of(new BigDecimal("1000")));
        when(saleRepository.findTotalCollectedAmountByMonthAndYear(anyInt(), anyInt()))
                .thenReturn(Optional.of(new BigDecimal("700")));
        when(saleRepository.findTotalProfitByMonthAndYear(anyInt(), anyInt()))
                .thenReturn(Optional.of(new BigDecimal("300")));
        when(saleRepository.findTotalRefundsByMonthAndYear(anyInt(), anyInt()))
                .thenReturn(Optional.of(new BigDecimal("50")));
        when(saleRepository.sumRemainingAmountByStatus(any(SaleStatus.class)))
                .thenReturn(Optional.of(new BigDecimal("250")));
        when(saleRepository.sumRemainingAmountByStatusAndMonthYear(any(SaleStatus.class), anyInt(), anyInt()))
                .thenReturn(Optional.of(new BigDecimal("250")));
        when(saleRepository.countSalesByStatusInMonth(anyInt(), anyInt())).thenReturn(List.<Object[]>of(
                new Object[] { SaleStatus.COMPLETED, 3L },
                new Object[] { SaleStatus.ACTIVE, 2L },
                new Object[] { SaleStatus.CANCELED, 1L }));
        when(saleRepository.findOverdueFees(any(LocalDate.class)))
                .thenReturn(List.<Object[]>of(new Object[] { 2L, new BigDecimal("120") }));

        when(saleRepository.findTotalSalesAmountByYear(anyInt())).thenReturn(Optional.of(new BigDecimal("8000")));
        when(saleRepository.findTotalCollectedAmountByYear(anyInt())).thenReturn(Optional.of(new BigDecimal("5000")));
        when(saleRepository.findTotalProfitByYear(anyInt())).thenReturn(Optional.of(new BigDecimal("2500")));
        when(saleRepository.countSalesByStatusInYear(anyInt())).thenReturn(List.<Object[]>of(
                new Object[] { SaleStatus.COMPLETED, 20L },
                new Object[] { SaleStatus.ACTIVE, 10L },
                new Object[] { SaleStatus.CANCELED, 2L }));

        when(saleRepository.findMonthlySalesSeries(any(LocalDate.class))).thenReturn(List.<Object[]>of());
        when(saleRepository.findMonthlyCollectedSeries(any(LocalDate.class))).thenReturn(List.<Object[]>of());
        when(saleRepository.findTopClientsByYear(anyInt())).thenReturn(List.<Object[]>of());
        when(saleRepository.findCollectedAmountByClientInYear(anyInt())).thenReturn(List.<Object[]>of());
        when(saleRepository.findSalesByKindInMonth(anyInt(), anyInt())).thenReturn(List.<Object[]>of());

        when(clientRepository.count()).thenReturn(10L);
        when(clientRepository.countActiveClients()).thenReturn(6L);
    }

    @Test
    void getDashboardStatsEnsamblaElResumenDelMes() {
        DashboardStatsDto dto = ventaQueryServices.getDashboardStats(2026, 8);

        assertEquals(2026, dto.getYear());
        assertEquals(8, dto.getMonth());

        assertEquals(0, new BigDecimal("1000").compareTo(dto.getResumen().getTotalVendido()));
        assertEquals(0, new BigDecimal("700").compareTo(dto.getResumen().getTotalCobrado()));
        assertEquals(0, new BigDecimal("300").compareTo(dto.getResumen().getGanancia()));
        assertEquals(0, new BigDecimal("50").compareTo(dto.getResumen().getReembolsos()));
        assertEquals(0, new BigDecimal("250").compareTo(dto.getResumen().getPendientePorCobrar()));

        assertEquals(6, dto.getResumen().getTotalVentas());
        assertEquals(3, dto.getResumen().getCompletadas());
        assertEquals(2, dto.getResumen().getPendientes());
        assertEquals(1, dto.getResumen().getCanceladas());
        assertEquals(2, dto.getResumen().getCuotasVencidas().getCantidad());
        assertEquals(0, new BigDecimal("120").compareTo(dto.getResumen().getCuotasVencidas().getMonto()));

        assertEquals(32, dto.getAnual().getTotalVentas());
        assertEquals(0, new BigDecimal("8000").compareTo(dto.getAnual().getTotalVendido()));
        assertEquals(0, new BigDecimal("5000").compareTo(dto.getAnual().getTotalCobrado()));
        assertEquals(0, new BigDecimal("2500").compareTo(dto.getAnual().getGanancia()));

        assertEquals(10, dto.getClientes().getTotalClientes());
        assertEquals(6, dto.getClientes().getActivos());
    }

    @Test
    void getDashboardStatsCalculaSerieYTopClientes() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        when(saleRepository.findMonthlySalesSeries(any(LocalDate.class))).thenReturn(List.<Object[]>of(
                new Object[] { year, month, new BigDecimal("900"), new BigDecimal("250") }));
        when(saleRepository.findMonthlyCollectedSeries(any(LocalDate.class))).thenReturn(List.<Object[]>of(
                new Object[] { year, month, new BigDecimal("600") }));
        when(saleRepository.findTopClientsByYear(anyInt())).thenReturn(List.<Object[]>of(
                new Object[] { 1L, "Juan", 5L, new BigDecimal("48000") }));
        when(saleRepository.findCollectedAmountByClientInYear(anyInt())).thenReturn(List.<Object[]>of(
                new Object[] { 1L, new BigDecimal("30000") }));
        when(saleRepository.findSalesByKindInMonth(anyInt(), anyInt())).thenReturn(List.<Object[]>of(
                new Object[] { SaleKind.VENTA, 8L, new BigDecimal("90000") },
                new Object[] { SaleKind.PRESTAMO, 4L, new BigDecimal("30000") }));

        DashboardStatsDto dto = ventaQueryServices.getDashboardStats(year, month);

        assertEquals(12, dto.getSerie12Meses().size());
        DashboardStatsDto.Monthly current = dto.getSerie12Meses().stream()
                .filter(m -> m.getYear() == year && m.getMonth() == month)
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("900").compareTo(current.getVendido()));
        assertEquals(0, new BigDecimal("600").compareTo(current.getCobrado()));
        assertEquals(0, new BigDecimal("250").compareTo(current.getGanancia()));

        assertEquals(1, dto.getTopClientes().size());
        assertEquals("Juan", dto.getTopClientes().get(0).getClientName());
        assertEquals(5, dto.getTopClientes().get(0).getTotalVentas());
        assertEquals(0, new BigDecimal("48000").compareTo(dto.getTopClientes().get(0).getTotalVendido()));
        assertEquals(0, new BigDecimal("30000").compareTo(dto.getTopClientes().get(0).getTotalCobrado()));

        assertEquals(2, dto.getPorTipoProducto().size());
        assertEquals("VENTA", dto.getPorTipoProducto().get(0).getTipo());
        assertEquals("PRESTAMO", dto.getPorTipoProducto().get(1).getTipo());
    }
}
