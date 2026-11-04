package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.app.digital.payments.digital_pyments.models.dtos.SellerDeactivationRequest;
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

    private Client clienteConCartera(Long id, String nombre, Client vendedor) {
        Client cliente = new Client();
        cliente.setId(id);
        cliente.setName(nombre);
        Business business = new Business();
        business.setId(1L);
        cliente.setBusiness(business);
        cliente.setSeller(vendedor);
        return cliente;
    }

    private Client vendedorDestino(Long id, String nombre) {
        Client vendedor = clienteVendedor();
        vendedor.setId(id);
        vendedor.setName(nombre);
        return vendedor;
    }

    @Test
    void deshabilitarVendedorTraspasaTodaLaCarteraAlOtroVendedor() {
        Client vendedor = clienteVendedor();
        Client destino = vendedorDestino(7L, "luis");
        Client uno = clienteConCartera(10L, "cliente uno", vendedor);
        Client dos = clienteConCartera(11L, "cliente dos", vendedor);
        List<Client> cartera = List.of(uno, dos);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(vendedor));
        when(clientRepository.findCarteraBySellerId(1L, 1L)).thenReturn(cartera);
        when(clientRepository.findById(7L)).thenReturn(Optional.of(destino));
        when(clientRepository.save(vendedor)).thenReturn(vendedor);

        SellerDeactivationRequest request = new SellerDeactivationRequest();
        request.setTransferToSellerId(7L);

        clientServices.deshabilitarClientVendedor(1L, request);

        assertEquals(7L, uno.getSeller().getId());
        assertEquals(7L, dos.getSeller().getId());
        assertEquals(false, vendedor.isSeller());
        verify(clientRepository).saveAll(cartera);
        verify(clientRepository).save(vendedor);
    }

    @Test
    void deshabilitarVendedorDejaLaCarteraSinVendedorCuandoLoPideElNegocio() {
        Client vendedor = clienteVendedor();
        Client uno = clienteConCartera(10L, "cliente uno", vendedor);
        Client dos = clienteConCartera(11L, "cliente dos", vendedor);
        List<Client> cartera = List.of(uno, dos);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(vendedor));
        when(clientRepository.findCarteraBySellerId(1L, 1L)).thenReturn(cartera);
        when(clientRepository.save(vendedor)).thenReturn(vendedor);

        SellerDeactivationRequest request = new SellerDeactivationRequest();
        request.setLeaveClientsUnassigned(true);

        clientServices.deshabilitarClientVendedor(1L, request);

        assertNull(uno.getSeller());
        assertNull(dos.getSeller());
        assertEquals(false, vendedor.isSeller());
        verify(clientRepository).saveAll(cartera);
    }

    @Test
    void deshabilitarVendedorConCarteraExigeElegirQueHacer() {
        Client vendedor = clienteVendedor();
        Client uno = clienteConCartera(10L, "cliente uno", vendedor);
        Client dos = clienteConCartera(11L, "cliente dos", vendedor);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(vendedor));
        when(clientRepository.findCarteraBySellerId(1L, 1L)).thenReturn(List.of(uno, dos));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> clientServices.deshabilitarClientVendedor(1L, null));

        assertTrue(ex.getMessage().contains("2 clientes"),
                "el mensaje tiene que decir cuántos clientes quedan en cartera");
        assertEquals(true, vendedor.isSeller());
        verify(clientRepository, never()).save(vendedor);
    }

    @Test
    void deshabilitarVendedorRechazaUnDestinoQueNoEsVendedor() {
        Client vendedor = clienteVendedor();
        Client noVendedor = new Client();
        noVendedor.setId(7L);
        noVendedor.setSeller(false);
        Business business = new Business();
        business.setId(1L);
        noVendedor.setBusiness(business);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(vendedor));
        when(clientRepository.findCarteraBySellerId(1L, 1L))
                .thenReturn(List.of(clienteConCartera(10L, "cliente uno", vendedor)));
        when(clientRepository.findById(7L)).thenReturn(Optional.of(noVendedor));

        SellerDeactivationRequest request = new SellerDeactivationRequest();
        request.setTransferToSellerId(7L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> clientServices.deshabilitarClientVendedor(1L, request));

        assertEquals("El cliente con ID: 7 no es un vendedor válido.", ex.getMessage());
        assertEquals(true, vendedor.isSeller());
    }

    @Test
    void deshabilitarVendedorRechazaUnDestinoQueEstaEnLaPropiaCartera() {
        Client vendedor = clienteVendedor();
        Client destino = vendedorDestino(10L, "luis");
        Client otro = clienteConCartera(11L, "cliente dos", vendedor);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(vendedor));
        // El destino es un vendedor, pero es cliente de quien se está deshabilitando:
        // traspasarle la cartera lo dejaría apuntando a un vendedor inactivo.
        when(clientRepository.findCarteraBySellerId(1L, 1L))
                .thenReturn(List.of(clienteConCartera(10L, "luis", vendedor), otro));
        when(clientRepository.findById(10L)).thenReturn(Optional.of(destino));

        SellerDeactivationRequest request = new SellerDeactivationRequest();
        request.setTransferToSellerId(10L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> clientServices.deshabilitarClientVendedor(1L, request));

        assertTrue(ex.getMessage().contains("cartera"));
        assertEquals(true, vendedor.isSeller());
    }

    @Test
    void deshabilitarVendedorPermiteSiNoTieneClientes() {
        Client cliente = clienteVendedor();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clientRepository.findCarteraBySellerId(1L, 1L)).thenReturn(List.of());
        when(clientRepository.save(cliente)).thenReturn(cliente);

        clientServices.deshabilitarClientVendedor(1L, null);

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

    // --- Atribución: vendedor asignado y vendedor de operaciones ---

    @Test
    void getClientByIdDevuelveElIdDelVendedorAsignado() {
        Client cliente = new Client();
        cliente.setId(5L);
        Business business = new Business();
        business.setId(1L);
        cliente.setBusiness(business);
        Client vendedor = new Client();
        vendedor.setId(9L);
        vendedor.setName("luis");
        cliente.setSeller(vendedor);

        when(clientRepository.findById(5L)).thenReturn(Optional.of(cliente));

        ClientDto dto = clientServices.getClientById(5L);

        assertEquals(9L, dto.getSellerId());
        assertEquals("luis", dto.getSellerName());
    }

    @Test
    void updateClientRechazaSerSuPropioVendedor() {
        Client existente = clienteVendedor();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(existente));

        ClientRequestDto dto = dtoValido();
        dto.setSellerId(1L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> clientServices.updateClient(1L, dto));

        assertEquals("Un cliente no puede ser su propio vendedor ni generar un ciclo de vendedores.",
                ex.getMessage());
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void updateClientRechazaCicloDeVendedores() {
        // B (id 2) ya tiene a A (id 1) como su vendedor: asignarle B como
        // vendedor a A cerraría el círculo A → B → A.
        Business business = new Business();
        business.setId(1L);

        Client a = new Client();
        a.setId(1L);
        a.setBusiness(business);

        Client b = new Client();
        b.setId(2L);
        b.setSeller(true);
        b.setBusiness(business);
        b.setSeller(a);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(a));
        when(clientRepository.findById(2L)).thenReturn(Optional.of(b));

        ClientRequestDto dto = dtoValido();
        dto.setSellerId(2L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> clientServices.updateClient(1L, dto));

        assertEquals("Un cliente no puede ser su propio vendedor ni generar un ciclo de vendedores.",
                ex.getMessage());
    }

    @Test
    void deletedClientRechazaSiTieneOperacionesAtribuidasComoVendedor() {
        Client cliente = new Client();
        cliente.setId(3L);
        Business business = new Business();
        business.setId(1L);
        cliente.setBusiness(business);
        when(clientRepository.findById(3L)).thenReturn(Optional.of(cliente));
        when(clientRepository.countBySellerId(3L)).thenReturn(0L);
        when(clientRepository.existenClientesConTransaccionesAsociadas(3L)).thenReturn(false);
        when(clientRepository.existenTransaccionesAtribuidasComoVendedor(3L)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> clientServices.deletedClient(3L));

        assertEquals("No se puede eliminar el cliente porque tiene operaciones atribuidas como vendedor.",
                ex.getMessage());
        verify(clientRepository, never()).delete(cliente);
    }
}
