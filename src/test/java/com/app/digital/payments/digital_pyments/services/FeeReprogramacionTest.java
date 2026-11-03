package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.IPaymentRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;

/**
 * Reprogramación en bloque (S10): corre la cuota nombrada y todas las impagas
 * que le siguen, en períodos de la frecuencia o en días naturales, sin tocar las
 * cobradas, las anteriores ni los montos, y sellando un solo evento de auditoría
 * por llamada. S11: la fecha final pasa a ser el vencimiento de la última cuota.
 */
class FeeReprogramacionTest {

    @Mock
    private IFeeRepository feeRepository;

    @Mock
    private ISaleRepository saleRepository;

    @Mock
    private IPaymentRepository paymentRepository;

    private FeeServices feeServices;

    private Sale sale;

    // Plan semanal de referencia: 01/08, 08/08 y 15/08
    private static final LocalDate PRIMER_VENCIMIENTO = LocalDate.of(2026, 8, 1);
    private static final LocalDate SEGUNDO_VENCIMIENTO = LocalDate.of(2026, 8, 8);
    private static final LocalDate TERCER_VENCIMIENTO = LocalDate.of(2026, 8, 15);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        feeServices = new FeeServices(feeRepository, saleRepository, paymentRepository);

        sale = new Sale();
        sale.setId(1L);
        sale.setDateCreation(PRIMER_VENCIMIENTO);
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setTypePayments(Payments.SEMANAL);
        sale.setPriceTotal(new BigDecimal("300"));
        sale.setRemainingAmount(new BigDecimal("300"));
        sale.setAmountFee(new BigDecimal("100"));
        sale.setDescriptionProduct("Producto de prueba");
        sale.setFees(new ArrayList<>());

        Client cliente = new Client();
        cliente.setId(1L);
        cliente.setName("Juan");
        sale.setClient(cliente);
    }

    private Fee cuota(long id, int numero, LocalDate vencimiento, boolean cobrada) {
        Fee cuota = new Fee();
        cuota.setId(id);
        cuota.setSale(sale);
        cuota.setNumberFee(numero);
        cuota.setAmount(new BigDecimal("100"));
        cuota.setExpirationDate(vencimiento);
        cuota.setPaid(false);
        if (cobrada) {
            cuota.setPaid(true);
            cuota.setPaidAmount(new BigDecimal("100"));
            cuota.setLastPaymentAt(PRIMER_VENCIMIENTO.atTime(10, 0));
        }
        sale.getFees().add(cuota);
        when(feeRepository.findById(id)).thenReturn(Optional.of(cuota));
        return cuota;
    }

    private List<Long> idsDe(List<FeeDto> cuotas) {
        return cuotas.stream().map(FeeDto::getId).toList();
    }

    @Test
    void porPeriodosCorreLaCuotaNombradaYLasSiguientesImpagas() {
        // +2 períodos semanales = +14 días sobre la cuota 2 y la 3; la 1 no se toca
        Fee c1 = cuota(1L, 1, PRIMER_VENCIMIENTO, false);
        Fee c2 = cuota(2L, 2, SEGUNDO_VENCIMIENTO, false);
        Fee c3 = cuota(3L, 3, TERCER_VENCIMIENTO, false);

        List<FeeDto> movidas = feeServices.reprogramFee(2L, 2, null);

        assertEquals(List.of(2L, 3L), idsDe(movidas));
        assertEquals(PRIMER_VENCIMIENTO, c1.getExpirationDate());
        assertEquals(SEGUNDO_VENCIMIENTO.plusDays(14), c2.getExpirationDate());
        assertEquals(TERCER_VENCIMIENTO.plusDays(14), c3.getExpirationDate());
        assertEquals(SEGUNDO_VENCIMIENTO.plusDays(14), movidas.get(0).getExpirationDate());
        verify(feeRepository).save(c2);
        verify(feeRepository).save(c3);
        verify(feeRepository, never()).save(c1);
    }

    @Test
    void porDiasCorreLaCuotaNombradaYLasSiguientesImpagas() {
        Fee c1 = cuota(1L, 1, PRIMER_VENCIMIENTO, false);
        Fee c2 = cuota(2L, 2, SEGUNDO_VENCIMIENTO, false);
        Fee c3 = cuota(3L, 3, TERCER_VENCIMIENTO, false);

        List<FeeDto> movidas = feeServices.reprogramFee(1L, null, 3);

        assertEquals(List.of(1L, 2L, 3L), idsDe(movidas));
        assertEquals(PRIMER_VENCIMIENTO.plusDays(3), c1.getExpirationDate());
        assertEquals(SEGUNDO_VENCIMIENTO.plusDays(3), c2.getExpirationDate());
        assertEquals(TERCER_VENCIMIENTO.plusDays(3), c3.getExpirationDate());
    }

    @Test
    void noMueveLasCuotasAnterioresNiLasCobradas() {
        // La anterior (1) queda donde estaba y la cobrada del tramo (3) tampoco se
        // corre: reprogramar la 2 mueve solo la 2.
        Fee c1 = cuota(1L, 1, PRIMER_VENCIMIENTO, false);
        Fee c2 = cuota(2L, 2, SEGUNDO_VENCIMIENTO, false);
        Fee c3 = cuota(3L, 3, TERCER_VENCIMIENTO, true);

        List<FeeDto> movidas = feeServices.reprogramFee(2L, null, 3);

        assertEquals(List.of(2L), idsDe(movidas));
        assertEquals(PRIMER_VENCIMIENTO, c1.getExpirationDate());
        assertEquals(SEGUNDO_VENCIMIENTO.plusDays(3), c2.getExpirationDate());
        assertEquals(TERCER_VENCIMIENTO, c3.getExpirationDate());
        verify(feeRepository, never()).save(c1);
        verify(feeRepository, never()).save(c3);
    }

    @Test
    void noCambiaLosMontosProgramadosNiLoCobrado() {
        Fee c1 = cuota(1L, 1, PRIMER_VENCIMIENTO, true);
        Fee c2 = cuota(2L, 2, SEGUNDO_VENCIMIENTO, false);
        Fee c3 = cuota(3L, 3, TERCER_VENCIMIENTO, false);
        // Cobro parcial en la cuota que se corre: sigue abierta por su saldo
        c2.setPaidAmount(new BigDecimal("40"));
        c2.setLastPaymentAt(SEGUNDO_VENCIMIENTO.atTime(9, 0));

        feeServices.reprogramFee(2L, 2, null);

        assertEquals(0, new BigDecimal("100").compareTo(c1.getAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(c2.getAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(c3.getAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(c1.getPaidAmount()));
        assertEquals(0, new BigDecimal("40").compareTo(c2.getPaidAmount()));
        assertEquals(0, new BigDecimal("300").compareTo(sale.getRemainingAmount()));
    }

    @Test
    void rechazaUnaVentaAnulada() {
        cuota(1L, 1, PRIMER_VENCIMIENTO, false);
        cuota(2L, 2, SEGUNDO_VENCIMIENTO, false);
        sale.setStatus(SaleStatus.CANCELED);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> feeServices.reprogramFee(1L, 2, null));

        assertEquals("No se puede reprogramar una cuota de una venta anulada", error.getMessage());
        assertEquals(PRIMER_VENCIMIENTO, sale.getFees().get(0).getExpirationDate());
        assertEquals(SEGUNDO_VENCIMIENTO, sale.getFees().get(1).getExpirationDate());
        assertNull(sale.getReprogrammedAt());
        assertEquals(Integer.valueOf(0), sale.getReprogramCount());
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void rechazaUnaVentaCompletada() {
        cuota(1L, 1, PRIMER_VENCIMIENTO, false);
        cuota(2L, 2, SEGUNDO_VENCIMIENTO, false);
        sale.setStatus(SaleStatus.COMPLETED);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> feeServices.reprogramFee(1L, 2, null));

        assertEquals("No se puede reprogramar una cuota de una venta completada", error.getMessage());
        assertEquals(PRIMER_VENCIMIENTO, sale.getFees().get(0).getExpirationDate());
        assertEquals(SEGUNDO_VENCIMIENTO, sale.getFees().get(1).getExpirationDate());
        assertNull(sale.getReprogrammedAt());
        assertEquals(Integer.valueOf(0), sale.getReprogramCount());
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void rechazaUnaCuotaYaCobrada() {
        cuota(1L, 1, PRIMER_VENCIMIENTO, true);
        cuota(2L, 2, SEGUNDO_VENCIMIENTO, false);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> feeServices.reprogramFee(1L, 2, null));

        assertEquals("La cuota ya está pagada", error.getMessage());
        assertEquals(PRIMER_VENCIMIENTO, sale.getFees().get(0).getExpirationDate());
        assertEquals(SEGUNDO_VENCIMIENTO, sale.getFees().get(1).getExpirationDate());
        assertNull(sale.getReprogrammedAt());
        assertEquals(Integer.valueOf(0), sale.getReprogramCount());
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void rechazaPeriodosYDiasJuntos() {
        Fee c1 = cuota(1L, 1, PRIMER_VENCIMIENTO, false);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> feeServices.reprogramFee(1L, 2, 3));

        assertTrue(error.getMessage().contains("exactamente uno"));
        assertEquals(PRIMER_VENCIMIENTO, c1.getExpirationDate());
        assertNull(sale.getReprogrammedAt());
        assertEquals(Integer.valueOf(0), sale.getReprogramCount());
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void rechazaSinPeriodosNiDias() {
        Fee c1 = cuota(1L, 1, PRIMER_VENCIMIENTO, false);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> feeServices.reprogramFee(1L, null, null));

        assertTrue(error.getMessage().contains("exactamente uno"));
        assertEquals(PRIMER_VENCIMIENTO, c1.getExpirationDate());
        assertNull(sale.getReprogrammedAt());
        assertEquals(Integer.valueOf(0), sale.getReprogramCount());
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void rechazaUnValorMenorAUno() {
        Fee c1 = cuota(1L, 1, PRIMER_VENCIMIENTO, false);
        Fee c2 = cuota(2L, 2, SEGUNDO_VENCIMIENTO, false);

        assertThrows(IllegalArgumentException.class, () -> feeServices.reprogramFee(1L, 0, null));
        assertThrows(IllegalArgumentException.class, () -> feeServices.reprogramFee(1L, null, 0));
        assertThrows(IllegalArgumentException.class, () -> feeServices.reprogramFee(1L, -1, null));
        assertThrows(IllegalArgumentException.class, () -> feeServices.reprogramFee(1L, null, -3));

        assertEquals(PRIMER_VENCIMIENTO, c1.getExpirationDate());
        assertEquals(SEGUNDO_VENCIMIENTO, c2.getExpirationDate());
        assertNull(sale.getReprogrammedAt());
        assertEquals(Integer.valueOf(0), sale.getReprogramCount());
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void sellaUnSoloEventoDeAuditoriaPorLlamada() {
        cuota(1L, 1, PRIMER_VENCIMIENTO, false);
        cuota(2L, 2, SEGUNDO_VENCIMIENTO, false);
        cuota(3L, 3, TERCER_VENCIMIENTO, false);
        sale.setReprogramCount(0);

        feeServices.reprogramFee(1L, null, 3);

        // Tres cuotas movidas, un evento de auditoría
        assertEquals(Integer.valueOf(1), sale.getReprogramCount());
        assertNotNull(sale.getReprogrammedAt());
        verify(saleRepository, times(1)).save(sale);
    }

    @Test
    void unaCuentaNulaArrancaEnUno() {
        cuota(1L, 1, PRIMER_VENCIMIENTO, false);
        sale.setReprogramCount(null);

        feeServices.reprogramFee(1L, null, 3);

        assertEquals(Integer.valueOf(1), sale.getReprogramCount());
        assertNotNull(sale.getReprogrammedAt());
    }

    @Test
    void laFechaFinalPasaASerElVencimientoDeLaUltimaCuota() {
        cuota(1L, 1, PRIMER_VENCIMIENTO, false);
        cuota(2L, 2, SEGUNDO_VENCIMIENTO, false);
        Fee c3 = cuota(3L, 3, TERCER_VENCIMIENTO, false);
        sale.setFinalPaymentDate(c3.getExpirationDate());

        feeServices.reprogramFee(2L, 2, null);

        assertEquals(TERCER_VENCIMIENTO.plusDays(14), c3.getExpirationDate());
        assertEquals(TERCER_VENCIMIENTO.plusDays(14), sale.getFinalPaymentDate());
    }

    @Test
    void unaSegundaReprogramacionAcumulaCuentaYDelta() {
        Fee c1 = cuota(1L, 1, PRIMER_VENCIMIENTO, false);
        Fee c2 = cuota(2L, 2, SEGUNDO_VENCIMIENTO, false);
        Fee c3 = cuota(3L, 3, TERCER_VENCIMIENTO, false);

        feeServices.reprogramFee(1L, null, 3);
        feeServices.reprogramFee(1L, null, 2);

        assertEquals(Integer.valueOf(2), sale.getReprogramCount());
        assertNotNull(sale.getReprogrammedAt());
        // Los dos deltas se suman: +3 y +2 días sobre el plan original
        assertEquals(PRIMER_VENCIMIENTO.plusDays(5), c1.getExpirationDate());
        assertEquals(SEGUNDO_VENCIMIENTO.plusDays(5), c2.getExpirationDate());
        assertEquals(TERCER_VENCIMIENTO.plusDays(5), c3.getExpirationDate());
        verify(saleRepository, times(2)).save(sale);
    }

    @Test
    void reprogramaDesdeLaCuotaNombradaAunqueEsteFueraDeOrden() {
        // La lista de la venta no garantiza el orden por número: la reprogramación
        // ordena por numberFee antes de correr el tramo.
        Fee c3 = cuota(3L, 3, TERCER_VENCIMIENTO, false);
        Fee c1 = cuota(1L, 1, PRIMER_VENCIMIENTO, false);
        Fee c2 = cuota(2L, 2, SEGUNDO_VENCIMIENTO, false);

        List<FeeDto> movidas = feeServices.reprogramFee(2L, null, 3);

        assertEquals(List.of(2L, 3L), idsDe(movidas));
        assertEquals(PRIMER_VENCIMIENTO, c1.getExpirationDate());
        assertEquals(SEGUNDO_VENCIMIENTO.plusDays(3), c2.getExpirationDate());
        assertEquals(TERCER_VENCIMIENTO.plusDays(3), c3.getExpirationDate());
    }

    @Test
    void guardaLaVentaConLaAuditoriaYLasCuotasMovidas() {
        Fee c1 = cuota(1L, 1, PRIMER_VENCIMIENTO, false);
        Fee c2 = cuota(2L, 2, SEGUNDO_VENCIMIENTO, false);

        List<FeeDto> movidas = feeServices.reprogramFee(1L, 1, null);

        // Un período semanal: +7 días
        assertEquals(List.of(1L, 2L), idsDe(movidas));
        assertEquals(PRIMER_VENCIMIENTO.plusWeeks(1), c1.getExpirationDate());
        assertEquals(SEGUNDO_VENCIMIENTO.plusWeeks(1), c2.getExpirationDate());
        assertEquals(PRIMER_VENCIMIENTO.plusWeeks(1), movidas.get(0).getExpirationDate());
        verify(feeRepository).save(c1);
        verify(feeRepository).save(c2);
        verify(saleRepository).save(sale);
        assertNotNull(sale.getReprogrammedAt());
        assertTrue(!sale.getReprogrammedAt().isAfter(LocalDateTime.now()));
    }
}
