package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.mappers.PaymentPromiseMapper;
import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.PaymentPromise;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.PaymentPromiseDto;
import com.app.digital.payments.digital_pyments.models.dtos.PaymentPromiseRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.IPaymentPromiseRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.PromiseStatus;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

/**
 * Promesa de pago: un cliente se compromete a pagar un monto en una fecha. Se
 * registra por cliente + moneda y nunca se mezclan monedas.
 */
class PaymentPromiseServicesTest {

    private static final Long CLIENTE_ID = 7L;
    private static final Long NEGOCIO_ID = 1L;
    private static final String ARS = "ARS";

    @Mock
    private IPaymentPromiseRepository promiseRepository;
    @Mock
    private IClientRepository clientRepository;
    @Mock
    private ISaleRepository saleRepository;
    @Mock
    private IBusinessRepository businessRepository;

    private PaymentPromiseServices promiseServices;
    private Business negocio;
    private Client cliente;
    private Sale operacion;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        promiseServices = new PaymentPromiseServices(
                promiseRepository, clientRepository, saleRepository, businessRepository);

        negocio = new Business();
        negocio.setId(NEGOCIO_ID);

        cliente = new Client();
        cliente.setId(CLIENTE_ID);
        cliente.setName("Juan");
        cliente.setBusiness(negocio);

        operacion = new Sale();
        operacion.setId(1L);
        operacion.setClient(cliente);
        operacion.setBusiness(negocio);
        operacion.setCurrency(ARS);
        operacion.setStatus(SaleStatus.ACTIVE);
        operacion.setPriceTotal(new BigDecimal("1000"));
        operacion.setFees(new ArrayList<>());

        TenantContext.setBusinessId(NEGOCIO_ID);
        when(clientRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
        when(businessRepository.getReferenceById(NEGOCIO_ID)).thenReturn(negocio);
        when(saleRepository.findByClientIdAndStatusNot(CLIENTE_ID, SaleStatus.CANCELED))
                .thenReturn(List.of(operacion));
        when(promiseRepository.save(any(PaymentPromise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private PaymentPromiseRequestDto request(String moneda, String monto, LocalDate fecha) {
        PaymentPromiseRequestDto dto = new PaymentPromiseRequestDto();
        dto.setCurrency(moneda);
        dto.setAmount(new BigDecimal(monto));
        dto.setPromisedDate(fecha);
        dto.setNote("  prometio el viernes  ");
        return dto;
    }

    @Test
    void creaUnaPromesaPendiente() {
        LocalDate manana = LocalDate.now().plusDays(1);

        PaymentPromiseDto dto = promiseServices.crear(CLIENTE_ID, request(ARS, "600", manana));

        assertEquals(CLIENTE_ID, dto.getClientId());
        assertEquals("Juan", dto.getClientName());
        assertEquals(ARS, dto.getCurrency());
        assertEquals(0, new BigDecimal("600").compareTo(dto.getAmount()));
        assertEquals(manana, dto.getPromisedDate());
        assertEquals(PromiseStatus.PENDING, dto.getStatus());
        assertFalse(dto.isVencida());
        assertEquals("prometio el viernes", dto.getNote());
        assertNotNull(dto.getCreatedAt());
        verify(promiseRepository).save(any(PaymentPromise.class));
    }

    @Test
    void rechazaUnaMonedaNoSoportada() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> promiseServices.crear(CLIENTE_ID, request("EUR", "100", LocalDate.now().plusDays(1))));

        assertEquals("La moneda no está soportada: EUR", error.getMessage());
        verify(promiseRepository, never()).save(any(PaymentPromise.class));
    }

    @Test
    void rechazaUnaMonedaVacia() {
        assertThrows(IllegalArgumentException.class,
                () -> promiseServices.crear(CLIENTE_ID, request(null, "100", LocalDate.now().plusDays(1))));
        assertThrows(IllegalArgumentException.class,
                () -> promiseServices.crear(CLIENTE_ID, request("   ", "100", LocalDate.now().plusDays(1))));
        verify(promiseRepository, never()).save(any(PaymentPromise.class));
    }

    @Test
    void rechazaUnMontoNoPositivo() {
        assertThrows(IllegalArgumentException.class,
                () -> promiseServices.crear(CLIENTE_ID, request(ARS, "0", LocalDate.now().plusDays(1))));
        assertThrows(IllegalArgumentException.class,
                () -> promiseServices.crear(CLIENTE_ID, request(ARS, "-10", LocalDate.now().plusDays(1))));
        verify(promiseRepository, never()).save(any(PaymentPromise.class));
    }

    @Test
    void rechazaUnaFechaPasada() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> promiseServices.crear(CLIENTE_ID, request(ARS, "100", LocalDate.now().minusDays(1))));

        assertEquals("La fecha prometida no puede ser anterior a hoy", error.getMessage());
        verify(promiseRepository, never()).save(any(PaymentPromise.class));
    }

    @Test
    void rechazaUnMontoMayorALaDeuda() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> promiseServices.crear(CLIENTE_ID, request(ARS, "1000.01", LocalDate.now().plusDays(1))));

        assertEquals("El monto prometido no puede superar la deuda del cliente en ARS", error.getMessage());
        verify(promiseRepository, never()).save(any(PaymentPromise.class));
    }

    @Test
    void rechazaUnClienteSinDeudaEnLaMoneda() {
        operacion.setCurrency("USD");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> promiseServices.crear(CLIENTE_ID, request(ARS, "100", LocalDate.now().plusDays(1))));

        assertEquals("El cliente no tiene deuda pendiente en ARS", error.getMessage());
        verify(promiseRepository, never()).save(any(PaymentPromise.class));
    }

    @Test
    void cancelaUnaPromesaPendiente() {
        PaymentPromise promesa = new PaymentPromise(negocio, cliente, ARS, new BigDecimal("100"),
                LocalDate.now().plusDays(2), null, LocalDateTime.now());
        when(promiseRepository.findById(10L)).thenReturn(Optional.of(promesa));

        PaymentPromiseDto dto = promiseServices.cancelar(10L);

        assertEquals(PromiseStatus.CANCELLED, dto.getStatus());
        assertNotNull(dto.getResolvedAt());
    }

    @Test
    void noCancelaUnaPromesaYaResuelta() {
        PaymentPromise promesa = new PaymentPromise(negocio, cliente, ARS, new BigDecimal("100"),
                LocalDate.now().plusDays(2), null, LocalDateTime.now());
        promesa.setStatus(PromiseStatus.CANCELLED);
        when(promiseRepository.findById(10L)).thenReturn(Optional.of(promesa));

        assertThrows(IllegalStateException.class, () -> promiseServices.cancelar(10L));
    }

    @Test
    void cumpleUnaPromesaPendiente() {
        PaymentPromise promesa = new PaymentPromise(negocio, cliente, ARS, new BigDecimal("100"),
                LocalDate.now().plusDays(2), null, LocalDateTime.now());
        when(promiseRepository.findById(10L)).thenReturn(Optional.of(promesa));

        PaymentPromiseDto dto = promiseServices.cumplir(10L);

        assertEquals(PromiseStatus.FULFILLED, dto.getStatus());
        assertNotNull(dto.getResolvedAt());
    }

    @Test
    void noCumpleUnaPromesaYaResuelta() {
        PaymentPromise promesa = new PaymentPromise(negocio, cliente, ARS, new BigDecimal("100"),
                LocalDate.now().plusDays(2), null, LocalDateTime.now());
        promesa.setStatus(PromiseStatus.FULFILLED);
        when(promiseRepository.findById(10L)).thenReturn(Optional.of(promesa));

        assertThrows(IllegalStateException.class, () -> promiseServices.cumplir(10L));
    }

    @Test
    void elMapperMarcaVencidaUnaPromesaConFechaPasada() {
        PaymentPromise promesa = new PaymentPromise(negocio, cliente, ARS, new BigDecimal("100"),
                LocalDate.now().minusDays(3), null, LocalDateTime.now().minusDays(5));

        PaymentPromiseDto dto = PaymentPromiseMapper.toDto(promesa, LocalDate.now());

        assertTrue(dto.isVencida());
        assertEquals(3L, dto.getDiasAtraso().longValue());
    }

    @Test
    void unaPromesaCumplidaNoSeMarcaVencida() {
        PaymentPromise promesa = new PaymentPromise(negocio, cliente, ARS, new BigDecimal("100"),
                LocalDate.now().minusDays(3), null, LocalDateTime.now().minusDays(5));
        promesa.setStatus(PromiseStatus.FULFILLED);

        PaymentPromiseDto dto = PaymentPromiseMapper.toDto(promesa, LocalDate.now());

        assertFalse(dto.isVencida());
        assertNull(dto.getDiasAtraso());
    }

    @Test
    void listaVigentesSoloLasPendientes() {
        PaymentPromise pendiente = new PaymentPromise(negocio, cliente, ARS, new BigDecimal("100"),
                LocalDate.now().plusDays(2), null, LocalDateTime.now());
        when(promiseRepository.findByClientIdInAndStatusOrderByPromisedDateAsc(
                List.of(CLIENTE_ID), PromiseStatus.PENDING)).thenReturn(List.of(pendiente));

        List<PaymentPromiseDto> promesas = promiseServices.listarVigentesDeClientes(List.of(CLIENTE_ID));

        assertEquals(1, promesas.size());
        assertEquals(PromiseStatus.PENDING, promesas.get(0).getStatus());
    }
}
