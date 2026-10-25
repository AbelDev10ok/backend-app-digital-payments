package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.dtos.ClientDto;
import com.app.digital.payments.digital_pyments.models.dtos.ClientRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

class ClientServicesTest {

    @Mock
    private IClientRepository clientRepository;

    @Mock
    private IBusinessRepository businessRepository;

    @InjectMocks
    private ClientServices clientServices;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TenantContext.setBusinessId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private ClientRequestDto dtoValido() {
        ClientRequestDto dto = new ClientRequestDto();
        dto.setName("Juan Pérez");
        dto.setTelefono("1144556677");
        dto.setEmail("juan@example.com");
        dto.setDireccion("Av. Siempre Viva 123");
        dto.setDni("12345678");
        return dto;
    }

    @Test
    void createClientRespetaElNombreTalCualLoEscribioElUsuario() {
        when(clientRepository.findByDni("12345678")).thenReturn(Optional.empty());
        when(clientRepository.findByEmail("juan@example.com")).thenReturn(Optional.empty());
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientDto result = clientServices.createClient(dtoValido());

        assertEquals("Juan Pérez", result.getName());
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void updateClientGuardaElNombreConSuFormato() {
        Client existente = new Client();
        existente.setId(1L);
        Business business = new Business();
        business.setId(1L);
        existente.setBusiness(business);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientDto result = clientServices.updateClient(1L, dtoValido());

        assertEquals("Juan Pérez", result.getName());
    }

    @Test
    void updateClientRechazaNombreRepetidoIgnorandoMayusculas() {
        Client existente = new Client();
        existente.setId(1L);
        Business business = new Business();
        business.setId(1L);
        existente.setBusiness(business);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(existente));

        Client otro = new Client();
        otro.setId(99L);
        otro.setName("JUAN PEREZ");
        when(clientRepository.findByBusinessIdAndNameIgnoreCase(1L, "Juan Pérez"))
                .thenReturn(List.of(otro));

        ClientRequestDto dto = dtoValido();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> clientServices.updateClient(1L, dto));

        assertEquals("El nombre del cliente ya está en uso por otro cliente.", ex.getMessage());
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void createClientRechazaDniDuplicado() {
        Client existente = new Client();
        existente.setId(99L);
        when(clientRepository.findByDni("12345678")).thenReturn(Optional.of(existente));

        assertThrows(RuntimeException.class, () -> clientServices.createClient(dtoValido()));
    }

    @Test
    void createClientRechazaEmailDuplicado() {
        Client existente = new Client();
        existente.setId(99L);
        when(clientRepository.findByDni("12345678")).thenReturn(Optional.empty());
        when(clientRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(existente));

        assertThrows(RuntimeException.class, () -> clientServices.createClient(dtoValido()));
    }

    private Client clienteVendedor() {
        Client cliente = new Client();
        cliente.setId(1L);
        cliente.setName("ana");
        cliente.setSeller(true);
        Business business = new Business();
        business.setId(1L);
        cliente.setBusiness(business);
        return cliente;
    }

    @Test
    void deshabilitarVendedorBloqueadoCuandoTieneClientesAsignados() {
        Client cliente = clienteVendedor();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clientRepository.countBySellerId(1L)).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> clientServices.deshabilitarClientVendedor(1L));

        assertEquals("El vendedor aún tiene clientes asignados. Reasigna o desasigna sus clientes antes de deshabilitarlo.",
                ex.getMessage());
        assertEquals(true, cliente.isSeller());
        verify(clientRepository, never()).save(cliente);
    }

    @Test
    void deshabilitarVendedorPermiteSiNoTieneClientes() {
        Client cliente = clienteVendedor();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clientRepository.countBySellerId(1L)).thenReturn(0L);
        when(clientRepository.save(cliente)).thenReturn(cliente);

        clientServices.deshabilitarClientVendedor(1L);

        assertEquals(false, cliente.isSeller());
        verify(clientRepository).save(cliente);
    }

    @Test
    void obtenerVendedoresActivosFiltraPorNegocioAutenticado() {
        Client vendedor = new Client();
        vendedor.setId(7L);
        vendedor.setName("luis");
        vendedor.setSeller(true);
        when(clientRepository.findAllSellersActivos(1L)).thenReturn(List.of(vendedor));

        List<ClientDto> result = clientServices.obtenerTodosLosVendedoresConClientesActivos();

        assertEquals(1, result.size());
        assertEquals("luis", result.get(0).getName());
        verify(clientRepository).findAllSellersActivos(1L);
    }

    // --- Tests de cobranza (fee paid) ---

    @Test
    void calcularTotalVentasPagadas_conCuotasPagadas_devuelveSumaPorMoneda() {
        when(clientRepository.sumCollectedAmountByClientIdExcludingKindGrouped(1L, SaleKind.PRESTAMO))
                .thenReturn(List.<Object[]>of(new Object[] { "ARS", new BigDecimal("40") }));

        Map<String, BigDecimal> result = clientServices.calcularTotalVentasPagadas(1L);

        assertEquals(0, new BigDecimal("40").compareTo(result.get("ARS")));
    }

    @Test
    void calcularTotalPrestamosPagados_conCuotasPagadas_devuelveSumaPorMoneda() {
        when(clientRepository.sumCollectedAmountByClientIdByKindGrouped(1L, SaleKind.PRESTAMO))
                .thenReturn(List.<Object[]>of(new Object[] { "USD", new BigDecimal("200") }));

        Map<String, BigDecimal> result = clientServices.calcularTotalPrestamosPagados(1L);

        assertEquals(0, new BigDecimal("200").compareTo(result.get("USD")));
    }

    @Test
    void calcularTotalVentasPagadas_sinPagos_devuelveMapaVacio() {
        when(clientRepository.sumCollectedAmountByClientIdExcludingKindGrouped(1L, SaleKind.PRESTAMO))
                .thenReturn(List.<Object[]>of());

        Map<String, BigDecimal> result = clientServices.calcularTotalVentasPagadas(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void calcularDeudaTotalVentas_agrupaPorMonedaSinSumar() {
        when(clientRepository.sumRemainingAmountByClientIdAndSalesNotCompletedGrouped(1L, SaleStatus.COMPLETED, SaleKind.PRESTAMO))
                .thenReturn(List.<Object[]>of(
                        new Object[] { "ARS", new BigDecimal("150000") },
                        new Object[] { "USD", new BigDecimal("320") }));

        Map<String, BigDecimal> result = clientServices.calcularDeudaTotalVentas(1L);

        assertEquals(2, result.size());
        assertEquals(0, new BigDecimal("150000").compareTo(result.get("ARS")));
        assertEquals(0, new BigDecimal("320").compareTo(result.get("USD")));
    }
}
