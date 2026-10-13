package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.CobrosHoyResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.CuotaListItemDto;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.utils.FeeStatus;
import com.app.digital.payments.digital_pyments.utils.SaleKind;

class FeeQueryServicesTest {

    @Mock
    private IFeeRepository feeRepository;

    private FeeQueryServices feeQueryServices;

    private Sale sale;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        feeQueryServices = new FeeQueryServices(feeRepository);

        Client client = new Client();
        client.setId(3L);
        client.setName("Maria Lopez");
        client.setTelefono("0981-555-123");

        sale = new Sale();
        sale.setId(2L);
        sale.setClient(client);
        sale.setDescriptionProduct("Televisor 32 pulgadas");
        sale.setKind(SaleKind.VENTA);
        sale.setQuiantityFees(12);
        sale.setAmountFee(new BigDecimal("150"));

        Fee vencida = new Fee(sale, 1, new BigDecimal("0"), LocalDate.now().minusDays(5));
        vencida.setId(10L);
        vencida.setPaid(false);

        when(feeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(vencida)));
    }

    private Page<CuotaListItemDto> consultar(Pageable pageable) {
        return feeQueryServices.getCuotas(FeeStatus.PENDING, null, null, null, pageable);
    }

    @Test
    void getCuotasMapeaClienteMontoEfectivoYEstadoVencida() {
        Page<CuotaListItemDto> page = consultar(PageRequest.of(0, 20));

        assertEquals(1, page.getContent().size());
        CuotaListItemDto dto = page.getContent().get(0);
        assertEquals(10L, dto.getId());
        assertEquals(2L, dto.getSaleId());
        assertEquals("Maria Lopez", dto.getClientName());
        assertEquals("0981-555-123", dto.getClientPhone());
        assertEquals("VENTA", dto.getSaleKind());
        assertEquals(1, dto.getNumeroCuota());
        assertEquals(12, dto.getTotalCuotas());
        // La cuota no tiene monto fijado (0): se usa el valor acordado de la venta
        assertEquals(new BigDecimal("150"), dto.getMonto());
        assertEquals("DELAYED", dto.getEstado());
        assertEquals(5L, dto.getDiasAtraso());
        assertEquals(Boolean.FALSE, dto.getPagada());
    }

    @Test
    void getCuotasAplicaOrdenPorVencimientoSiNoSeEnviaOrden() {
        consultar(PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(feeRepository).findAll(any(Specification.class), captor.capture());

        assertEquals(Sort.by(Sort.Direction.ASC, "expirationDate"), captor.getValue().getSort());
    }

    @Test
    void getCuotasRespetaElOrdenEnviado() {
        Sort orden = Sort.by(Sort.Direction.DESC, "amount");
        consultar(PageRequest.of(0, 20, orden));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(feeRepository).findAll(any(Specification.class), captor.capture());

        assertEquals(orden, captor.getValue().getSort());
    }

    @Test
    void getCuotasRechazaFiltroPostponed() {
        assertThrows(IllegalArgumentException.class,
                () -> feeQueryServices.getCuotas(FeeStatus.POSTPONED, null, null, null, PageRequest.of(0, 20)));
    }

    @Test
    void getCobrosDeHoyArmaResumenYAgenda() {
        when(feeRepository.sumPendingFeesByExpirationDate(any(LocalDate.class)))
                .thenReturn(List.<Object[]>of(new Object[] { 2L, new BigDecimal("300") }));
        when(feeRepository.sumOverdueFeesBefore(any(LocalDate.class)))
                .thenReturn(List.<Object[]>of(new Object[] { 1L, new BigDecimal("150") }));

        CobrosHoyResponseDto response = feeQueryServices.getCobrosDeHoy(null);

        assertEquals(LocalDate.now(), response.getFecha());
        assertEquals(2, response.getHoy().getCantidad());
        assertEquals(new BigDecimal("300"), response.getHoy().getMonto());
        assertEquals(1, response.getVencidas().getCantidad());
        assertEquals(new BigDecimal("150"), response.getVencidas().getMonto());
        // El item mockeado es la cuota vencida de setUp
        assertEquals("DELAYED", response.getItems().get(0).getEstado());
    }

    @Test
    void getCobrosDeHoyToleraResumenVacio() {
        when(feeRepository.sumPendingFeesByExpirationDate(any(LocalDate.class))).thenReturn(List.of());
        when(feeRepository.sumOverdueFeesBefore(any(LocalDate.class))).thenReturn(List.of());

        CobrosHoyResponseDto response = feeQueryServices.getCobrosDeHoy(LocalDate.now().plusDays(1));

        assertEquals(0, response.getHoy().getCantidad());
        assertEquals(BigDecimal.ZERO, response.getHoy().getMonto());
        assertEquals(0, response.getVencidas().getCantidad());
        assertEquals(LocalDate.now().plusDays(1), response.getFecha());
    }
}
