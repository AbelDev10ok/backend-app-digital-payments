package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.dtos.SellerMetricDto;
import com.app.digital.payments.digital_pyments.models.dtos.SellerMetricsDto;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

class SellerMetricServicesTest {

    @Mock
    private ISaleRepository saleRepository;

    @Mock
    private IClientRepository clientRepository;

    private SellerMetricServices services;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        services = new SellerMetricServices(saleRepository, clientRepository);
        TenantContext.setBusinessId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Client vendedor(long id, String name) {
        Client cliente = new Client();
        cliente.setId(id);
        cliente.setName(name);
        cliente.setSeller(true);
        return cliente;
    }

    @Test
    void armaLasFilasConActividadYLaFilaSinVendedor() {
        when(clientRepository.findAllSellersActivos(1L)).thenReturn(List.of(vendedor(2L, "luis")));
        when(clientRepository.countCarteraBySeller()).thenReturn(List.<Object[]>of(
                new Object[] { 2L, 3L },
                new Object[] { null, 5L }));
        when(saleRepository.findSellerMetricsByPeriod(2, 2026, SaleStatus.CANCELED)).thenReturn(List.<Object[]>of(
                new Object[] { 2L, SaleKind.VENTA, "ARS", 4L, 1L, new BigDecimal("1000") },
                new Object[] { null, SaleKind.PRESTAMO, "ARS", 1L, 0L, new BigDecimal("500") }));
        when(saleRepository.findSellerCollectedByPeriod(2, 2026)).thenReturn(List.<Object[]>of(
                new Object[] { 2L, SaleKind.VENTA, "ARS", new BigDecimal("300") }));
        when(saleRepository.findSellerPortfolioDebtByKindAndCurrency(SaleStatus.ACTIVE)).thenReturn(List.<Object[]>of(
                new Object[] { 2L, SaleKind.VENTA, "USD", new BigDecimal("50") }));

        SellerMetricsDto result = services.getSellerMetrics(2026, 2);

        assertEquals(1, result.getVendedores().size());
        SellerMetricDto luis = result.getVendedores().get(0);
        assertEquals(3L, luis.getClientesCartera());
        assertEquals(4L, luis.getOperaciones().get(SaleKind.VENTA));
        assertEquals(1L, luis.getAnuladas().get(SaleKind.VENTA));
        assertEquals(0, new BigDecimal("1000").compareTo(luis.getVendido().get(SaleKind.VENTA).get("ARS")));
        assertEquals(0, new BigDecimal("300").compareTo(luis.getCobrado().get(SaleKind.VENTA).get("ARS")));
        assertEquals(0, new BigDecimal("50").compareTo(luis.getDeuda().get(SaleKind.VENTA).get("USD")));

        // La fila "Sin vendedor" completa la reconciliación con el negocio
        assertEquals(5L, result.getSinVendedor().getClientesCartera());
        assertEquals(1L, result.getSinVendedor().getOperaciones().get(SaleKind.PRESTAMO));

        assertEquals(List.of("ARS", "USD"), result.getMonedasDisponibles());
    }

    @Test
    void marcaInactivoAlVendedorQueYaNoEstaHabilitado() {
        when(clientRepository.findAllSellersActivos(1L)).thenReturn(List.of());
        when(clientRepository.countCarteraBySeller()).thenReturn(List.of());
        when(saleRepository.findSellerMetricsByPeriod(2, 2026, SaleStatus.CANCELED)).thenReturn(List.<Object[]>of(
                new Object[] { 9L, SaleKind.VENTA, "ARS", 1L, 0L, new BigDecimal("100") }));
        when(saleRepository.findSellerCollectedByPeriod(2, 2026)).thenReturn(List.of());
        when(saleRepository.findSellerPortfolioDebtByKindAndCurrency(SaleStatus.ACTIVE)).thenReturn(List.of());
        when(clientRepository.findById(9L)).thenReturn(Optional.of(vendedor(9L, "ana")));

        SellerMetricsDto result = services.getSellerMetrics(2026, 2);

        assertEquals(1, result.getVendedores().size());
        SellerMetricDto fila = result.getVendedores().get(0);
        assertEquals("ana", fila.getSellerName());
        assertFalse(fila.isActivo());
        assertEquals(1L, fila.getOperaciones().get(SaleKind.VENTA));
    }
}
