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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.CollectionRound;
import com.app.digital.payments.digital_pyments.models.CollectionRoundItem;
import com.app.digital.payments.digital_pyments.models.PaymentPromise;
import com.app.digital.payments.digital_pyments.models.dtos.CollectionRoundDto;
import com.app.digital.payments.digital_pyments.models.dtos.CuotaListItemDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.ICollectionRoundItemRepository;
import com.app.digital.payments.digital_pyments.repositories.ICollectionRoundRepository;
import com.app.digital.payments.digital_pyments.repositories.IPaymentPromiseRepository;
import com.app.digital.payments.digital_pyments.services.iservices.IFeeQueryServices;
import com.app.digital.payments.digital_pyments.utils.PromiseStatus;

/**
 * Ronda de cobro del dia: se arma una sola vez, agrupa por cliente + moneda y
 * ordena por riesgo (promesa vencida primero, despues mayor atraso).
 */
class RoundServicesTest {

    private static final Long NEGOCIO_ID = 1L;
    private static final LocalDate DIA = LocalDate.of(2026, 11, 18);

    @Mock
    private ICollectionRoundRepository roundRepository;
    @Mock
    private ICollectionRoundItemRepository itemRepository;
    @Mock
    private IFeeQueryServices feeQueryServices;
    @Mock
    private IPaymentPromiseRepository promiseRepository;
    @Mock
    private IBusinessRepository businessRepository;

    private RoundServices roundServices;
    private Business negocio;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        roundServices = new RoundServices(roundRepository, itemRepository, feeQueryServices, promiseRepository,
                businessRepository);

        negocio = new Business();
        negocio.setId(NEGOCIO_ID);

        when(businessRepository.findByIdForUpdate(NEGOCIO_ID)).thenReturn(Optional.of(negocio));
        when(roundRepository.findByBusinessIdAndRoundDate(any(), any())).thenReturn(Optional.empty());
        when(roundRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(promiseRepository.findByClientIdInAndStatusOrderByPromisedDateAsc(any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void agrupaPorClienteYMonedaYSeparaLasMonedas() {
        when(feeQueryServices.cuotasPendientesDelNegocio(NEGOCIO_ID, DIA)).thenReturn(List.of(
                cuota(1L, 10L, "Rosa Diaz", 100L, "ARS", "10000", "20000", 3),
                cuota(2L, 10L, "Rosa Diaz", 101L, "ARS", "5000", "20000", 1),
                cuota(3L, 10L, "Rosa Diaz", 102L, "USD", "300", "500", 2)));

        CollectionRoundDto ronda = roundServices.getRondaDelDia(NEGOCIO_ID, DIA);

        assertEquals(2, ronda.getItems().size());
        assertEquals("ARS", ronda.getItems().get(0).getCurrency());
        assertEquals(new BigDecimal("15000"), ronda.getItems().get(0).getAmount());
        assertEquals("USD", ronda.getItems().get(1).getCurrency());
        assertEquals(new BigDecimal("300"), ronda.getItems().get(1).getAmount());
    }

    @Test
    void capaElMontoPorLaDeudaDeLaVenta() {
        when(feeQueryServices.cuotasPendientesDelNegocio(NEGOCIO_ID, DIA)).thenReturn(List.of(
                cuota(1L, 10L, "Rosa Diaz", 100L, "ARS", "10000", "15000", 3),
                cuota(2L, 10L, "Rosa Diaz", 100L, "ARS", "10000", "15000", 3)));

        CollectionRoundDto ronda = roundServices.getRondaDelDia(NEGOCIO_ID, DIA);

        assertEquals(new BigDecimal("15000"), ronda.getItems().get(0).getAmount());
    }

    @Test
    void ordenaPrimeroLasPromesasVencidasYLuegoElMayorAtraso() {
        when(feeQueryServices.cuotasPendientesDelNegocio(NEGOCIO_ID, DIA)).thenReturn(List.of(
                cuota(1L, 10L, "Ana", 100L, "ARS", "10000", "20000", 10),
                cuota(2L, 20L, "Beto", 101L, "ARS", "10000", "20000", 2),
                cuota(3L, 30L, "Carla", 102L, "ARS", "10000", "20000", 5)));
        when(promiseRepository.findByClientIdInAndStatusOrderByPromisedDateAsc(any(), any()))
                .thenReturn(List.of(promesa(20L, "Beto", DIA.minusDays(1))));

        CollectionRoundDto ronda = roundServices.getRondaDelDia(NEGOCIO_ID, DIA);

        assertEquals("Beto", ronda.getItems().get(0).getClientName());
        assertEquals("Ana", ronda.getItems().get(1).getClientName());
        assertEquals("Carla", ronda.getItems().get(2).getClientName());
        assertTrue(ronda.getItems().get(0).isPromiseOverdue());
    }

    @Test
    void esIdempotenteYNoReconstruyeUnaRondaExistente() {
        CollectionRound existente = new CollectionRound(negocio, DIA, LocalDateTime.now());
        existente.setId(99L);
        existente.agregarItem(new CollectionRoundItem(existente, 1, 10L, "Rosa Diaz", null, null, "ARS",
                new BigDecimal("10000"), 3L, null));
        when(roundRepository.findByBusinessIdAndRoundDate(NEGOCIO_ID, DIA)).thenReturn(Optional.of(existente));
        when(feeQueryServices.cuotasPendientesDelNegocio(NEGOCIO_ID, DIA)).thenReturn(List.of());

        CollectionRoundDto ronda = roundServices.getRondaDelDia(NEGOCIO_ID, DIA);

        assertEquals(99L, ronda.getId());
        assertEquals(1, ronda.getItems().size());
        verify(roundRepository, never()).save(any());
    }

    @Test
    void unaParadaQueYaNoDebeQuedaComoCobrada() {
        when(roundRepository.findByBusinessIdAndRoundDate(NEGOCIO_ID, DIA)).thenReturn(Optional.of(rondaConItem()));
        when(feeQueryServices.cuotasPendientesDelNegocio(NEGOCIO_ID, DIA)).thenReturn(List.of());

        CollectionRoundDto ronda = roundServices.getRondaDelDia(NEGOCIO_ID, DIA);

        assertEquals("COBRADO", ronda.getItems().get(0).getEstado());
        assertNull(ronda.getItems().get(0).getPendingAmount());
    }

    @Test
    void marcaUnaParadaComoNoEstaba() {
        CollectionRound ronda = rondaConItem();
        CollectionRoundItem item = ronda.getItems().get(0);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(feeQueryServices.cuotasPendientesDelNegocio(NEGOCIO_ID, DIA)).thenReturn(List.of(
                cuota(1L, 10L, "Rosa Diaz", 100L, "ARS", "10000", "20000", 3)));

        CollectionRoundDto dto = roundServices.marcarVisita(NEGOCIO_ID, 1L, true, "No estaba");

        assertTrue(item.getNoEstaba());
        assertEquals("No estaba", item.getNote());
        assertEquals("NO_ESTABA", dto.getItems().get(0).getEstado());
    }

    @Test
    void rechazaMarcarUnaParadaDeOtroNegocio() {
        CollectionRound ronda = rondaConItem();
        when(itemRepository.findById(1L)).thenReturn(Optional.of(ronda.getItems().get(0)));

        assertThrows(RuntimeException.class, () -> roundServices.marcarVisita(2L, 1L, true, null));
    }

    private CollectionRound rondaConItem() {
        CollectionRound ronda = new CollectionRound(negocio, DIA, LocalDateTime.now());
        ronda.setId(50L);
        CollectionRoundItem item = new CollectionRoundItem(ronda, 1, 10L, "Rosa Diaz", null, null, "ARS",
                new BigDecimal("10000"), 3L, null);
        item.setId(1L);
        ronda.agregarItem(item);
        return ronda;
    }

    private CuotaListItemDto cuota(Long id, Long clientId, String clientName, Long saleId, String currency,
            String monto, String deudaVenta, int diasAtraso) {
        CuotaListItemDto dto = new CuotaListItemDto();
        dto.setId(id);
        dto.setClientId(clientId);
        dto.setClientName(clientName);
        dto.setSaleId(saleId);
        dto.setCurrency(currency);
        dto.setMonto(new BigDecimal(monto));
        dto.setDeudaVenta(new BigDecimal(deudaVenta));
        dto.setFechaVencimiento(DIA.minusDays(diasAtraso));
        dto.setDiasAtraso((long) diasAtraso);
        dto.setPagada(false);
        return dto;
    }

    private PaymentPromise promesa(Long clientId, String clientName, LocalDate fecha) {
        Client client = new Client();
        client.setId(clientId);
        client.setName(clientName);
        client.setBusiness(negocio);
        PaymentPromise promesa = new PaymentPromise(negocio, client, "ARS", new BigDecimal("10000"), fecha, null,
                LocalDateTime.now());
        promesa.setId(900L + clientId);
        promesa.setStatus(PromiseStatus.PENDING);
        return promesa;
    }
}
