package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Subscription;
import com.app.digital.payments.digital_pyments.models.SubscriptionEvent;
import com.app.digital.payments.digital_pyments.models.dtos.SubscriptionStatusDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IPlanRepository;
import com.app.digital.payments.digital_pyments.repositories.ISubscriptionEventRepository;
import com.app.digital.payments.digital_pyments.repositories.ISubscriptionRepository;
import com.app.digital.payments.digital_pyments.utils.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests del procesamiento de webhooks de MP. Cubren el despacho por topic
 * (cada notificacion trae data.id de un recurso distinto: factura, pago o
 * preapproval) y la serializacion por bloqueo de fila cuando llegan los dos
 * eventos de un mismo ciclo de suscripcion casi simultaneos.
 */
class SubscriptionServiceTest {

    private static final String PREAPPROVAL_ID = "PRE-1";
    private static final String FACTURA_ID = "7032881553";
    private static final LocalDate FECHA_COBRO = LocalDate.of(2026, 10, 12);
    private static final LocalDate PROXIMO_COBRO = LocalDate.of(2026, 11, 12);

    @Mock
    private ISubscriptionRepository subscriptionRepository;
    @Mock
    private IPlanRepository planRepository;
    @Mock
    private IBusinessRepository businessRepository;
    @Mock
    private ISubscriptionEventRepository eventRepository;
    @Mock
    private MercadoPagoService mpService;

    private AutoCloseable mocks;
    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new SubscriptionService(
                subscriptionRepository, planRepository, businessRepository,
                eventRepository, mpService, new ObjectMapper());
    }

    @AfterEach
    void tearDown() throws Exception {
        TenantContext.clear();
        mocks.close();
    }

    private Subscription suscripcionPendiente() {
        Subscription sub = new Subscription();
        sub.setBusiness(new Business());
        sub.setStatus("PENDING");
        return sub;
    }

    /** Factura tal como la devuelve GET /authorized_payments/{id}. */
    private Map<String, Object> facturaAprobada() {
        Map<String, Object> factura = new HashMap<>();
        factura.put("preapproval_id", PREAPPROVAL_ID);
        factura.put("debit_date", FECHA_COBRO + "T00:44:13.000-03:00");
        factura.put("payment", Map.of("id", Long.valueOf(FACTURA_ID), "status", "approved"));
        return factura;
    }

    /** Preapproval tal como lo devuelve GET /preapproval/{id}. */
    private Map<String, Object> preapprovalAutorizado() {
        Map<String, Object> preapproval = new HashMap<>();
        preapproval.put("status", "authorized");
        preapproval.put("next_payment_date", PROXIMO_COBRO + "T00:44:13.000-03:00");
        return preapproval;
    }

    private void assertSuscripcionActiva(Subscription sub) {
        assertEquals("ACTIVE", sub.getStatus());
        assertEquals(FECHA_COBRO, sub.getCurrentPeriodStart());
        assertEquals(PROXIMO_COBRO, sub.getCurrentPeriodEnd());
        assertEquals("ACTIVE", sub.getBusiness().getPlanStatus());
        assertEquals(PROXIMO_COBRO, sub.getBusiness().getSubscriptionUntil());
    }

    @Test
    void facturaAutorizadaResuelvePreapprovalYActivaSuscripcion() throws Exception {
        Subscription sub = suscripcionPendiente();
        when(subscriptionRepository.findByMpPreapprovalIdForUpdate(PREAPPROVAL_ID)).thenReturn(Optional.of(sub));
        when(mpService.getAuthorizedPayment(FACTURA_ID)).thenReturn(facturaAprobada());
        when(mpService.getPreapproval(PREAPPROVAL_ID)).thenReturn(preapprovalAutorizado());

        service.processWebhook(null, null, null, FACTURA_ID, "subscription_authorized_payment");

        assertSuscripcionActiva(sub);
        // El data.id es una factura: jamas debe consultarse como preapproval
        verify(mpService, never()).getPreapproval(FACTURA_ID);
        verify(subscriptionRepository).save(sub);
        verify(eventRepository).save(any(SubscriptionEvent.class));
    }

    @Test
    void pagoDeSuscripcionActualizaPeriodo() throws Exception {
        Subscription sub = suscripcionPendiente();
        when(subscriptionRepository.findByMpPreapprovalIdForUpdate(PREAPPROVAL_ID)).thenReturn(Optional.of(sub));

        Map<String, Object> pago = new HashMap<>();
        pago.put("preapproval_id", PREAPPROVAL_ID);
        pago.put("status", "approved");
        pago.put("date_approved", FECHA_COBRO + "T01:00:00.000-03:00");
        when(mpService.getPayment(FACTURA_ID)).thenReturn(pago);
        when(mpService.getPreapproval(PREAPPROVAL_ID)).thenReturn(preapprovalAutorizado());

        service.processWebhook(null, null, null, FACTURA_ID, "payment");

        assertSuscripcionActiva(sub);
        verify(subscriptionRepository).save(sub);
    }

    @Test
    void pagoSinSuscripcionSeIgnora() throws Exception {
        Map<String, Object> pago = new HashMap<>();
        pago.put("status", "approved");
        when(mpService.getPayment("123")).thenReturn(pago);

        service.processWebhook(null, null, null, "123", "payment");

        verify(mpService, never()).getPreapproval(anyString());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void preapprovalCanceladoConservaElAccesoHastaFinDelPeriodo() throws Exception {
        Subscription sub = suscripcionPendiente();
        sub.getBusiness().setSubscriptionUntil(PROXIMO_COBRO);
        when(subscriptionRepository.findByMpPreapprovalIdForUpdate(PREAPPROVAL_ID)).thenReturn(Optional.of(sub));

        Map<String, Object> preapproval = new HashMap<>();
        preapproval.put("status", "cancelled");
        when(mpService.getPreapproval(PREAPPROVAL_ID)).thenReturn(preapproval);

        service.processWebhook(null, null, null, PREAPPROVAL_ID, "subscription_preapproval");

        assertEquals("CANCELLED", sub.getStatus());
        assertEquals("CANCELLED", sub.getBusiness().getPlanStatus());
        // Politica SaaS: el acceso no se corta de inmediato, dura hasta el
        // fin del periodo pagado (subscriptionUntil no se mueve aqui)
        assertEquals(PROXIMO_COBRO, sub.getBusiness().getSubscriptionUntil());
        verify(subscriptionRepository).save(sub);
    }

    @Test
    void topicDesconocidoIgnoraSinLlamarAMp() throws Exception {
        service.processWebhook(null, null, null, "999", "merchant_order");

        verify(mpService, never()).getPreapproval(anyString());
        verify(mpService, never()).getAuthorizedPayment(anyString());
        verify(mpService, never()).getPayment(anyString());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void suscripcionLocalDesconocidaNoFalla() {
        when(subscriptionRepository.findByMpPreapprovalIdForUpdate("PRE-X")).thenReturn(Optional.empty());

        service.processWebhook(null, null, null, "PRE-X", "subscription_preapproval");

        verify(subscriptionRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void error404DeMpNoPropagaExcepcion() throws Exception {
        when(subscriptionRepository.findByMpPreapprovalIdForUpdate(PREAPPROVAL_ID))
                .thenReturn(Optional.of(suscripcionPendiente()));
        when(mpService.getPreapproval(PREAPPROVAL_ID))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

        assertDoesNotThrow(() ->
                service.processWebhook(null, null, null, PREAPPROVAL_ID, "subscription_preapproval"));

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void bloqueoNoDisponibleNoRompeElWebhook() {
        when(subscriptionRepository.findByMpPreapprovalIdForUpdate(PREAPPROVAL_ID))
                .thenThrow(new PessimisticLockingFailureException("lock timeout"));

        assertDoesNotThrow(() ->
                service.processWebhook(null, null, null, PREAPPROVAL_ID, "subscription_preapproval"));

        verify(subscriptionRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void eventoDuplicadoSeIgnoraAntesDeLlamarAMp() throws Exception {
        when(eventRepository.findByMpIdAndEvent(PREAPPROVAL_ID, "webhook:subscription_preapproval:" + PREAPPROVAL_ID))
                .thenReturn(Optional.of(new SubscriptionEvent()));

        service.processWebhook(null, null, null, PREAPPROVAL_ID, "subscription_preapproval");

        verify(mpService, never()).getPreapproval(anyString());
        verify(mpService, never()).getAuthorizedPayment(anyString());
        verify(mpService, never()).getPayment(anyString());
        verify(subscriptionRepository, never()).findByMpPreapprovalIdForUpdate(anyString());
    }

    @Test
    void eventoDuplicadoBajoBloqueoSeIgnora() {
        // 1er chequeo (sin bloqueo): vacio. 2do chequeo (bajo bloqueo): ya procesado
        when(eventRepository.findByMpIdAndEvent(PREAPPROVAL_ID, "webhook:subscription_preapproval:" + PREAPPROVAL_ID))
                .thenReturn(Optional.empty(), Optional.of(new SubscriptionEvent()));
        when(subscriptionRepository.findByMpPreapprovalIdForUpdate(PREAPPROVAL_ID))
                .thenReturn(Optional.of(suscripcionPendiente()));

        service.processWebhook(null, null, null, PREAPPROVAL_ID, "subscription_preapproval");

        verify(subscriptionRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }

    /**
     * Los dos eventos de un mismo ciclo (factura + preapproval) deben converger
     * al mismo estado final sin importar el orden en que se procesen.
     */
    @Test
    void eventosDelMismoCicloConvergenIndependientementeDelOrden() throws Exception {
        // Orden 1: primero la factura, despues el preapproval
        Subscription facturaPrimero = suscripcionPendiente();
        when(subscriptionRepository.findByMpPreapprovalIdForUpdate(PREAPPROVAL_ID))
                .thenReturn(Optional.of(facturaPrimero));
        when(mpService.getAuthorizedPayment(FACTURA_ID)).thenReturn(facturaAprobada());
        when(mpService.getPreapproval(PREAPPROVAL_ID)).thenReturn(preapprovalAutorizado());

        service.processWebhook(null, null, null, FACTURA_ID, "subscription_authorized_payment");
        service.processWebhook(null, null, null, PREAPPROVAL_ID, "subscription_preapproval");

        assertSuscripcionActiva(facturaPrimero);

        // Orden 2: primero el preapproval, despues la factura
        Subscription preapprovalPrimero = suscripcionPendiente();
        when(subscriptionRepository.findByMpPreapprovalIdForUpdate(PREAPPROVAL_ID))
                .thenReturn(Optional.of(preapprovalPrimero));

        service.processWebhook(null, null, null, PREAPPROVAL_ID, "subscription_preapproval");
        service.processWebhook(null, null, null, FACTURA_ID, "subscription_authorized_payment");

        assertSuscripcionActiva(preapprovalPrimero);
    }

    /**
     * Los dos webhooks de un mismo ciclo llegaron simultaneos en produccion
     * (misma ventana de milisegundos). Se simulan en dos threads reales y el
     * estado final debe quedar consistente (los datos de MP concuerdan:
     * debit_date + 1 mes == next_payment_date).
     */
    @Test
    void eventosConcurrentesConvergen() throws Exception {
        Subscription sub = suscripcionPendiente();
        when(subscriptionRepository.findByMpPreapprovalIdForUpdate(PREAPPROVAL_ID)).thenReturn(Optional.of(sub));
        when(mpService.getAuthorizedPayment(FACTURA_ID)).thenReturn(facturaAprobada());
        when(mpService.getPreapproval(PREAPPROVAL_ID)).thenReturn(preapprovalAutorizado());

        CyclicBarrier arranqueSimultaneo = new CyclicBarrier(2);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Thread factura = new Thread(() -> {
            try {
                arranqueSimultaneo.await();
                service.processWebhook(null, null, null, FACTURA_ID, "subscription_authorized_payment");
            } catch (Throwable t) {
                error.set(t);
            }
        }, "webhook-factura");
        Thread preapproval = new Thread(() -> {
            try {
                arranqueSimultaneo.await();
                service.processWebhook(null, null, null, PREAPPROVAL_ID, "subscription_preapproval");
            } catch (Throwable t) {
                error.set(t);
            }
        }, "webhook-preapproval");

        factura.start();
        preapproval.start();
        factura.join(5000);
        preapproval.join(5000);

        assertNull(error.get());
        assertSuscripcionActiva(sub);
    }

    @Test
    void webhookSinFirmaSeIgnoraSiLaFirmaEsObligatoria() {
        when(mpService.isSignatureRequired()).thenReturn(true);

        service.processWebhook(null, null, null, PREAPPROVAL_ID, "subscription_preapproval");

        verify(mpService, never()).validarFirma(any(), any(), any(), any());
        verify(subscriptionRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void firmaInvalidaIgnoraElEvento() {
        when(mpService.isSignatureRequired()).thenReturn(true);
        when(mpService.validarFirma("ts=1,v1=bad", "req-1", PREAPPROVAL_ID, null)).thenReturn(false);

        service.processWebhook(null, "ts=1,v1=bad", "req-1", PREAPPROVAL_ID, "subscription_preapproval");

        verify(subscriptionRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void firmaValidaProcesaElEvento() throws Exception {
        Subscription sub = suscripcionPendiente();
        when(mpService.isSignatureRequired()).thenReturn(true);
        when(mpService.validarFirma("ts=1,v1=ok", "req-1", PREAPPROVAL_ID, null)).thenReturn(true);
        when(subscriptionRepository.findByMpPreapprovalIdForUpdate(PREAPPROVAL_ID)).thenReturn(Optional.of(sub));
        when(mpService.getPreapproval(PREAPPROVAL_ID)).thenReturn(preapprovalAutorizado());

        service.processWebhook(null, "ts=1,v1=ok", "req-1", PREAPPROVAL_ID, "subscription_preapproval");

        assertEquals("ACTIVE", sub.getStatus());
        verify(subscriptionRepository).save(sub);
    }

    // ------------------------------------------------------------------
    // Gestion manual: cancelar / pausar / reactivar
    // ------------------------------------------------------------------

    private Subscription suscripcionActiva() {
        Subscription sub = new Subscription();
        Business business = new Business();
        business.setSubscriptionUntil(PROXIMO_COBRO);
        sub.setBusiness(business);
        sub.setStatus("ACTIVE");
        sub.setMpPreapprovalId(PREAPPROVAL_ID);
        sub.setCurrentPeriodStart(FECHA_COBRO);
        sub.setCurrentPeriodEnd(PROXIMO_COBRO);
        return sub;
    }

    private Subscription prepararOperacionManual(Subscription sub) {
        TenantContext.setBusinessId(7L);
        when(businessRepository.findById(7L)).thenReturn(Optional.of(sub.getBusiness()));
        when(subscriptionRepository.findByBusinessId(7L)).thenReturn(Optional.of(sub));
        return sub;
    }

    @Test
    void cancelarSuscripcionActualizaEstadoYRegistraEvento() throws Exception {
        Subscription sub = prepararOperacionManual(suscripcionActiva());

        SubscriptionStatusDto dto = service.cancelSubscription();

        assertEquals("CANCELLED", sub.getStatus());
        assertEquals("CANCELLED", sub.getBusiness().getPlanStatus());
        // El periodo pagado no se toca: el acceso dura hasta PROXIMO_COBRO
        assertEquals(PROXIMO_COBRO, sub.getBusiness().getSubscriptionUntil());
        verify(mpService).updatePreapprovalStatus(PREAPPROVAL_ID, "cancelled");
        verify(subscriptionRepository).save(sub);
        verify(eventRepository).save(any(SubscriptionEvent.class));
        assertEquals("CANCELLED", dto.getStatus());
    }

    @Test
    void cancelarSuscripcionYaCanceladaFalla() throws Exception {
        Subscription sub = prepararOperacionManual(suscripcionActiva());
        sub.setStatus("CANCELLED");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.cancelSubscription());

        assertEquals("La suscripcion ya esta cancelada", ex.getReason());
        verify(mpService, never()).updatePreapprovalStatus(anyString(), anyString());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void pausarSuscripcionActivaFunciona() throws Exception {
        Subscription sub = prepararOperacionManual(suscripcionActiva());

        service.pauseSubscription();

        assertEquals("PAUSED", sub.getStatus());
        assertEquals("PAUSED", sub.getBusiness().getPlanStatus());
        assertEquals(PROXIMO_COBRO, sub.getBusiness().getSubscriptionUntil());
        verify(mpService).updatePreapprovalStatus(PREAPPROVAL_ID, "paused");
    }

    @Test
    void pausarSoloSePermiteDesdeActiva() throws Exception {
        Subscription sub = prepararOperacionManual(suscripcionActiva());
        sub.setStatus("PENDING");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.pauseSubscription());

        assertEquals("Solo se puede pausar una suscripcion activa", ex.getReason());
        verify(mpService, never()).updatePreapprovalStatus(anyString(), anyString());
    }

    @Test
    void reactivarSuscripcionPausadaFunciona() throws Exception {
        Subscription sub = prepararOperacionManual(suscripcionActiva());
        sub.setStatus("PAUSED");
        sub.getBusiness().setPlanStatus("PAUSED");

        service.resumeSubscription();

        assertEquals("ACTIVE", sub.getStatus());
        assertEquals("ACTIVE", sub.getBusiness().getPlanStatus());
        assertEquals(PROXIMO_COBRO, sub.getBusiness().getSubscriptionUntil());
        verify(mpService).updatePreapprovalStatus(PREAPPROVAL_ID, "authorized");
    }

    @Test
    void reactivarSoloSePermiteDesdePausada() throws Exception {
        Subscription sub = prepararOperacionManual(suscripcionActiva());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resumeSubscription());

        assertEquals("Solo se puede reactivar una suscripcion pausada", ex.getReason());
        verify(mpService, never()).updatePreapprovalStatus(anyString(), anyString());
    }

    @Test
    void errorDeMpAlCancelarPropagaMensajeSinCambiarEstado() throws Exception {
        Subscription sub = prepararOperacionManual(suscripcionActiva());
        when(mpService.updatePreapprovalStatus(PREAPPROVAL_ID, "cancelled"))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.cancelSubscription());

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().startsWith("Mercado Pago rechazo la solicitud"));
        assertEquals("ACTIVE", sub.getStatus());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void accesoSeConservaHastaFinDelPeriodoTrasCancelar() {
        Subscription sub = suscripcionActiva();
        sub.setStatus("CANCELLED");
        when(subscriptionRepository.findByBusinessId(7L)).thenReturn(Optional.of(sub));
        when(businessRepository.findById(7L)).thenReturn(Optional.of(sub.getBusiness()));

        assertTrue(service.hasActiveAccess(7L));
    }

    @Test
    void sinAccesoCuandoElPeriodoPagadoVencio() {
        Subscription sub = suscripcionActiva();
        sub.setStatus("CANCELLED");
        sub.getBusiness().setSubscriptionUntil(LocalDate.now().minusDays(1));
        when(subscriptionRepository.findByBusinessId(7L)).thenReturn(Optional.of(sub));
        when(businessRepository.findById(7L)).thenReturn(Optional.of(sub.getBusiness()));

        assertFalse(service.hasActiveAccess(7L));
    }

    // ------------------------------------------------------------------
    // Renovacion rechazada: aviso al usuario sin cortar el acceso
    // ------------------------------------------------------------------

    @Test
    void webhookConCobroRechazadoMarcaElAvisoSinExtenderPeriodo() throws Exception {
        Subscription sub = suscripcionActiva();
        when(subscriptionRepository.findByMpPreapprovalIdForUpdate(PREAPPROVAL_ID)).thenReturn(Optional.of(sub));

        Map<String, Object> pago = new HashMap<>();
        pago.put("preapproval_id", PREAPPROVAL_ID);
        pago.put("status", "rejected");
        when(mpService.getPayment(FACTURA_ID)).thenReturn(pago);
        // La suscripcion sigue autorizada en MP mientras reintenta el cobro
        when(mpService.getPreapproval(PREAPPROVAL_ID)).thenReturn(preapprovalAutorizado());

        service.processWebhook(null, null, null, FACTURA_ID, "payment");

        assertNotNull(sub.getLastPaymentFailedAt());
        // El periodo no se toca: el acceso sigue hasta el fin del periodo pagado
        assertEquals(PROXIMO_COBRO, sub.getCurrentPeriodEnd());
        verify(subscriptionRepository).save(sub);
    }

    @Test
    void cobroAprobadoLimpiaElAvisoDeRenovacionRechazada() throws Exception {
        Subscription sub = suscripcionActiva();
        sub.setLastPaymentFailedAt(LocalDateTime.now().minusDays(1));
        when(subscriptionRepository.findByMpPreapprovalIdForUpdate(PREAPPROVAL_ID)).thenReturn(Optional.of(sub));
        when(mpService.getAuthorizedPayment(FACTURA_ID)).thenReturn(facturaAprobada());
        when(mpService.getPreapproval(PREAPPROVAL_ID)).thenReturn(preapprovalAutorizado());

        service.processWebhook(null, null, null, FACTURA_ID, "subscription_authorized_payment");

        assertNull(sub.getLastPaymentFailedAt());
        assertSuscripcionActiva(sub);
    }

    @Test
    void cancelacionManualLimpiaElAvisoDeCobroRechazado() throws Exception {
        Subscription sub = prepararOperacionManual(suscripcionActiva());
        sub.setLastPaymentFailedAt(LocalDateTime.now().minusDays(1));

        service.cancelSubscription();

        assertNull(sub.getLastPaymentFailedAt());
    }
}
