package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.ClientPaymentResultDto;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.IPaymentRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

/**
 * Cobro por cliente: un monto, una transaccion, distribuido entre las
 * operaciones del cliente con deuda en una moneda. La deuda mas vieja entra
 * primero (FIFO por vencimiento de la cuota impaga, desempate por id de
 * operacion) y dentro de cada operacion la cascada es la misma del cobro por
 * cuota.
 */
class ClientPaymentByClientTest {

    private static final Long CLIENTE_ID = 7L;
    private static final String ARS = "ARS";
    private static final LocalDate FECHA_COBRO = LocalDate.of(2026, 3, 1);

    @Mock
    private IFeeRepository feeRepository;
    @Mock
    private ISaleRepository saleRepository;
    @Mock
    private IPaymentRepository paymentRepository;
    @Mock
    private IClientRepository clientRepository;

    private FeeServices feeServices;
    private Business negocio;
    private Client cliente;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        feeServices = new FeeServices(feeRepository, saleRepository, paymentRepository, clientRepository);

        negocio = new Business();
        negocio.setId(1L);

        cliente = new Client();
        cliente.setId(CLIENTE_ID);
        cliente.setName("Juan");
        cliente.setBusiness(negocio);

        TenantContext.setBusinessId(negocio.getId());
        when(clientRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Fee cuota(long id, int numero, String monto, LocalDate vencimiento) {
        Fee cuota = new Fee();
        cuota.setId(id);
        cuota.setNumberFee(numero);
        cuota.setAmount(new BigDecimal(monto));
        cuota.setExpirationDate(vencimiento);
        cuota.setPaid(false);
        return cuota;
    }

    private Sale operacion(long id, String descripcion, String moneda, SaleStatus estado, String total, Fee... cuotas) {
        Sale sale = new Sale();
        sale.setId(id);
        sale.setClient(cliente);
        sale.setBusiness(negocio);
        sale.setDescriptionProduct(descripcion);
        sale.setKind(SaleKind.VENTA);
        sale.setCurrency(moneda);
        sale.setStatus(estado);
        sale.setPriceTotal(new BigDecimal(total));
        sale.setRemainingAmount(new BigDecimal(total));
        sale.setAmountFee(new BigDecimal(total));
        sale.setFees(new ArrayList<>());
        for (Fee cuota : cuotas) {
            cuota.setSale(sale);
            sale.getFees().add(cuota);
        }
        return sale;
    }

    private void devolverOperaciones(Sale... operaciones) {
        when(saleRepository.findByClientIdAndStatusNot(CLIENTE_ID, SaleStatus.CANCELED))
                .thenReturn(List.of(operaciones));
    }

    // S2: FIFO entre operaciones por la cuota impaga mas antigua, no por el id
    @Test
    void distribuyePrimeroLaOperacionConLaCuotaImpagaMasAntigua() {
        Sale nueva = operacion(1L, "Operacion nueva", ARS, SaleStatus.ACTIVE, "100",
                cuota(11L, 1, "100", LocalDate.of(2026, 2, 1)));
        Sale vieja = operacion(2L, "Operacion vieja", ARS, SaleStatus.ACTIVE, "100",
                cuota(21L, 1, "100", LocalDate.of(2026, 1, 1)));
        devolverOperaciones(nueva, vieja);

        ClientPaymentResultDto resultado = feeServices.registerClientPayment(
                CLIENTE_ID, new BigDecimal("100"), FECHA_COBRO, "EFECTIVO", ARS);

        // La operacion con vencimiento mas viejo (id 2) se cobra entera
        assertEquals(SaleStatus.COMPLETED, vieja.getStatus());
        assertTrue(vieja.getFees().get(0).getPaid());
        assertEquals(0, vieja.getRemainingAmount().signum());
        assertEquals(FECHA_COBRO, vieja.getRealFinalDate());
        // La nueva queda intacta
        assertEquals(SaleStatus.ACTIVE, nueva.getStatus());
        assertFalse(nueva.getFees().get(0).getPaid());
        assertEquals(0, new BigDecimal("100").compareTo(nueva.getRemainingAmount()));

        assertEquals(CLIENTE_ID, resultado.getClientId());
        assertEquals("Juan", resultado.getClientName());
        assertEquals(ARS, resultado.getCurrency());
        assertEquals(0, new BigDecimal("100").compareTo(resultado.getAppliedAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(resultado.getRemainingDebt()));
        assertEquals(1, resultado.getApplications().size());
        ClientPaymentResultDto.Application aplicada = resultado.getApplications().get(0);
        assertEquals(2L, aplicada.getSaleId());
        assertEquals("Operacion vieja", aplicada.getSaleDescription());
        assertEquals("VENTA", aplicada.getSaleKind());
        assertEquals(0, new BigDecimal("100").compareTo(aplicada.getAppliedAmount()));
        assertEquals(0, aplicada.getSaleRemainingAmount().signum());
        assertEquals(1, aplicada.getFeesPaid());
        assertTrue(aplicada.getSaleCompleted());

        verify(saleRepository).save(vieja);
        verify(saleRepository, never()).save(nueva);
    }

    // S2: un pago parcial deja la operacion abierta y no la completa
    @Test
    void pagoParcialNoCompletaLaOperacion() {
        Sale operacion = operacion(1L, "Operacion", ARS, SaleStatus.ACTIVE, "200",
                cuota(11L, 1, "100", LocalDate.of(2026, 1, 1)),
                cuota(12L, 2, "100", LocalDate.of(2026, 2, 1)));
        devolverOperaciones(operacion);

        ClientPaymentResultDto resultado = feeServices.registerClientPayment(
                CLIENTE_ID, new BigDecimal("50"), FECHA_COBRO, "EFECTIVO", ARS);

        assertEquals(SaleStatus.ACTIVE, operacion.getStatus());
        assertEquals(0, new BigDecimal("50").compareTo(operacion.getFees().get(0).getPaidAmount()));
        assertFalse(operacion.getFees().get(0).getPaid());
        assertEquals(0, operacion.getFees().get(1).getPaidAmount().signum());
        assertEquals(0, new BigDecimal("150").compareTo(operacion.getRemainingAmount()));

        assertEquals(0, new BigDecimal("50").compareTo(resultado.getAppliedAmount()));
        assertEquals(0, new BigDecimal("150").compareTo(resultado.getRemainingDebt()));
        assertEquals(1, resultado.getApplications().size());
        ClientPaymentResultDto.Application aplicada = resultado.getApplications().get(0);
        assertEquals(0, new BigDecimal("50").compareTo(aplicada.getAppliedAmount()));
        assertEquals(0, new BigDecimal("150").compareTo(aplicada.getSaleRemainingAmount()));
        assertEquals(1, aplicada.getFeesPaid());
        assertFalse(aplicada.getSaleCompleted());
    }

    // S2: el monto alcanza para completar una operacion y pagar parte de la siguiente
    @Test
    void completaUnaOperacionYPagaParcialmenteLaSiguiente() {
        Sale primera = operacion(1L, "Primera", ARS, SaleStatus.ACTIVE, "100",
                cuota(11L, 1, "100", LocalDate.of(2026, 1, 1)));
        Sale segunda = operacion(2L, "Segunda", ARS, SaleStatus.ACTIVE, "100",
                cuota(21L, 1, "100", LocalDate.of(2026, 2, 1)));
        devolverOperaciones(primera, segunda);

        ClientPaymentResultDto resultado = feeServices.registerClientPayment(
                CLIENTE_ID, new BigDecimal("150"), FECHA_COBRO, "EFECTIVO", ARS);

        assertEquals(SaleStatus.COMPLETED, primera.getStatus());
        assertTrue(primera.getFees().get(0).getPaid());
        assertEquals(0, primera.getRemainingAmount().signum());
        assertEquals(SaleStatus.ACTIVE, segunda.getStatus());
        assertEquals(0, new BigDecimal("50").compareTo(segunda.getFees().get(0).getPaidAmount()));
        assertEquals(0, new BigDecimal("50").compareTo(segunda.getRemainingAmount()));

        assertEquals(2, resultado.getApplications().size());
        assertEquals(1L, resultado.getApplications().get(0).getSaleId());
        assertEquals(0, new BigDecimal("100").compareTo(resultado.getApplications().get(0).getAppliedAmount()));
        assertTrue(resultado.getApplications().get(0).getSaleCompleted());
        assertEquals(2L, resultado.getApplications().get(1).getSaleId());
        assertEquals(0, new BigDecimal("50").compareTo(resultado.getApplications().get(1).getAppliedAmount()));
        assertFalse(resultado.getApplications().get(1).getSaleCompleted());
        assertEquals(0, new BigDecimal("50").compareTo(resultado.getRemainingDebt()));
    }

    // S3e: el monto no puede superar la deuda del cliente en la moneda
    @Test
    void rechazaUnMontoMayorALaDeudaDelCliente() {
        Sale operacion = operacion(1L, "Operacion", ARS, SaleStatus.ACTIVE, "100",
                cuota(11L, 1, "100", LocalDate.of(2026, 1, 1)));
        devolverOperaciones(operacion);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> feeServices.registerClientPayment(
                        CLIENTE_ID, new BigDecimal("100.01"), FECHA_COBRO, "EFECTIVO", ARS));

        assertEquals("El monto del pago no puede superar la deuda del cliente en ARS", error.getMessage());
        assertFalse(operacion.getFees().get(0).getPaid());
        assertEquals(0, operacion.getFees().get(0).getPaidAmount().signum());
        assertEquals(0, new BigDecimal("100").compareTo(operacion.getRemainingAmount()));
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    // S3d: sin deuda pendiente en la moneda pedida no hay nada que cobrar
    @Test
    void rechazaUnaMonedaSinDeudaPendiente() {
        Sale enDolares = operacion(1L, "Operacion USD", "USD", SaleStatus.ACTIVE, "100",
                cuota(11L, 1, "100", LocalDate.of(2026, 1, 1)));
        devolverOperaciones(enDolares);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> feeServices.registerClientPayment(
                        CLIENTE_ID, new BigDecimal("50"), FECHA_COBRO, "EFECTIVO", ARS));

        assertEquals("El cliente no tiene deuda pendiente en ARS", error.getMessage());
        assertFalse(enDolares.getFees().get(0).getPaid());
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    // S3b: el monto debe ser mayor a cero
    @Test
    void rechazaUnMontoNoPositivo() {
        Sale operacion = operacion(1L, "Operacion", ARS, SaleStatus.ACTIVE, "100",
                cuota(11L, 1, "100", LocalDate.of(2026, 1, 1)));
        devolverOperaciones(operacion);

        assertThrows(IllegalArgumentException.class, () -> feeServices.registerClientPayment(
                CLIENTE_ID, BigDecimal.ZERO, FECHA_COBRO, "EFECTIVO", ARS));
        assertThrows(IllegalArgumentException.class, () -> feeServices.registerClientPayment(
                CLIENTE_ID, new BigDecimal("-10"), FECHA_COBRO, "EFECTIVO", ARS));

        assertFalse(operacion.getFees().get(0).getPaid());
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    // S3c: la moneda es obligatoria para registrar el cobro
    @Test
    void rechazaUnaMonedaVacia() {
        assertThrows(IllegalArgumentException.class, () -> feeServices.registerClientPayment(
                CLIENTE_ID, new BigDecimal("50"), FECHA_COBRO, "EFECTIVO", null));
        assertThrows(IllegalArgumentException.class, () -> feeServices.registerClientPayment(
                CLIENTE_ID, new BigDecimal("50"), FECHA_COBRO, "EFECTIVO", "   "));
    }

    // S3f: una operacion anulada nunca entra en la distribucion
    @Test
    void ignoraLasOperacionesAnuladas() {
        Sale anulada = operacion(1L, "Anulada", ARS, SaleStatus.CANCELED, "100",
                cuota(11L, 1, "100", LocalDate.of(2026, 1, 1)));
        Sale activa = operacion(2L, "Activa", ARS, SaleStatus.ACTIVE, "100",
                cuota(21L, 1, "100", LocalDate.of(2026, 2, 1)));
        devolverOperaciones(anulada, activa);

        ClientPaymentResultDto resultado = feeServices.registerClientPayment(
                CLIENTE_ID, new BigDecimal("100"), FECHA_COBRO, "EFECTIVO", ARS);

        // Aunque la anulada tenga la cuota mas vieja, no recibe cobro
        assertFalse(anulada.getFees().get(0).getPaid());
        assertEquals(0, anulada.getFees().get(0).getPaidAmount().signum());
        assertEquals(SaleStatus.CANCELED, anulada.getStatus());
        assertTrue(activa.getFees().get(0).getPaid());
        assertEquals(SaleStatus.COMPLETED, activa.getStatus());
        assertEquals(1, resultado.getApplications().size());
        assertEquals(2L, resultado.getApplications().get(0).getSaleId());
        verify(saleRepository, never()).save(anulada);
    }

    // S2: un fallo al aplicar no deja nada aplicado (sin persistencia ni mutacion)
    @Test
    void unFalloAlAplicarNoDejaNadaAplicado() {
        // La operacion debe 200 pero su plan solo cubre 100: el cobro de 150 no
        // se puede asignar entero y el allocator rechaza sin aplicar nada.
        Sale operacion = operacion(1L, "Plan inconsistente", ARS, SaleStatus.ACTIVE, "200",
                cuota(11L, 1, "100", LocalDate.of(2026, 1, 1)));
        devolverOperaciones(operacion);

        assertThrows(IllegalStateException.class, () -> feeServices.registerClientPayment(
                CLIENTE_ID, new BigDecimal("150"), FECHA_COBRO, "EFECTIVO", ARS));

        assertFalse(operacion.getFees().get(0).getPaid());
        assertEquals(0, operacion.getFees().get(0).getPaidAmount().signum());
        assertEquals(0, new BigDecimal("200").compareTo(operacion.getRemainingAmount()));
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
        verify(feeRepository, never()).delete(any(Fee.class));
    }

    // S2: si falla una operacion posterior, no se persiste ningun cobro
    @Test
    void unFalloEnUnaOperacionPosteriorNoPersisteNingunCobro() {
        Sale primera = operacion(1L, "Primera", ARS, SaleStatus.ACTIVE, "100",
                cuota(11L, 1, "100", LocalDate.of(2026, 1, 1)));
        Sale inconsistente = operacion(2L, "Plan inconsistente", ARS, SaleStatus.ACTIVE, "200",
                cuota(21L, 1, "100", LocalDate.of(2026, 2, 1)));
        devolverOperaciones(primera, inconsistente);

        assertThrows(IllegalStateException.class, () -> feeServices.registerClientPayment(
                CLIENTE_ID, new BigDecimal("250"), FECHA_COBRO, "EFECTIVO", ARS));

        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
        verify(feeRepository, never()).delete(any(Fee.class));
    }

    // S3a: un cliente de otro negocio no es alcanzable desde el token
    @Test
    void rechazaUnClienteDeOtroNegocio() {
        Business otroNegocio = new Business();
        otroNegocio.setId(2L);
        cliente.setBusiness(otroNegocio);
        devolverOperaciones();

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> feeServices.registerClientPayment(
                        CLIENTE_ID, new BigDecimal("50"), FECHA_COBRO, "EFECTIVO", ARS));

        assertEquals("Recurso no encontrado", error.getMessage());
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }
}
