package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.IPaymentRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;

/**
 * Verificacion independiente de E1 (cronograma fijo): fija la conducta FIFO, las cuotas del plan y los rechazos sin mutacion.
 */
class FeePaymentFifoTest {

    @Mock private IFeeRepository feeRepository;
    @Mock private ISaleRepository saleRepository;
    @Mock private IPaymentRepository paymentRepository;

    private FeeServices feeServices;
    private Sale sale;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        feeServices = new FeeServices(feeRepository, saleRepository, paymentRepository);
        sale = new Sale();
        sale.setId(1L);
        sale.setDateCreation(LocalDate.of(2026, 8, 1));
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setTypePayments(Payments.MENSUAL);
        sale.setFees(new ArrayList<>());
        com.app.digital.payments.digital_pyments.models.Client cliente = new com.app.digital.payments.digital_pyments.models.Client();
        cliente.setId(1L);
        cliente.setName("Juan");
        sale.setClient(cliente);
    }

    private Fee cuota(long id, int numero, String monto) {
        Fee f = new Fee();
        f.setId(id);
        f.setSale(sale);
        f.setNumberFee(numero);
        f.setAmount(new BigDecimal(monto));
        f.setExpirationDate(LocalDate.of(2026, 8, 1).plusMonths(numero - 1L));
        f.setPaid(false);
        sale.getFees().add(f);
        when(feeRepository.findById(id)).thenReturn(Optional.of(f));
        return f;
    }

    private void plan(String total, String... montos) {
        sale.setPriceTotal(new BigDecimal(total));
        sale.setRemainingAmount(new BigDecimal(total));
        for (int i = 0; i < montos.length; i++) {
            cuota(i + 1, i + 1, montos[i]);
        }
    }

    private String es(Fee f) {
        return f.getAmount() + "|" + f.getPaidAmount() + "|" + f.getPaid() + "|"
                + f.getPayments().size() + "|" + f.getLastPaymentAt();
    }

    private String ss(Sale s) {
        return s.getRemainingAmount() + "|" + s.getStatus() + "|" + s.getRealFinalDate() + "|" + s.getFees().size();
    }

    private void sinPersistir() {
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    // S3/S7: un cobro mayor al saldo no muta nada
    @Test
    void rechazoMontoMayorAlSaldoNoMutaEstado() {
        plan("100", "100");
        String antesSale = ss(sale);
        String antesFee = es(sale.getFees().get(0));

        assertThrows(IllegalArgumentException.class,
                () -> feeServices.registerPayment(1L, new BigDecimal("150"), LocalDate.now(), "EFECTIVO"));

        assertEquals(antesSale, ss(sale));
        assertEquals(antesFee, es(sale.getFees().get(0)));
        sinPersistir();
    }

    // S7: asignacion en dos fases: el plan no cubre el cobro y NINGUNA cuota que
    // si tenia lugar queda con cobro a medias
    @Test
    void rechazoPlanQueNoCubreNoAplicaCobrosParciales() {
        plan("300", "100", "100");
        String antesSale = ss(sale);
        String antesF1 = es(sale.getFees().get(0));
        String antesF2 = es(sale.getFees().get(1));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> feeServices.registerPayment(1L, new BigDecimal("300"), LocalDate.now(), "EFECTIVO"));

        assertEquals("El plan de cuotas no cubre la deuda restante; revisá las cuotas de la operación",
                e.getMessage());
        assertEquals(antesSale, ss(sale));
        assertEquals(antesF1, es(sale.getFees().get(0)));
        assertEquals(antesF2, es(sale.getFees().get(1)));
        sinPersistir();
    }

    // Invariante 4: cobro parcial deja la cuota abierta y el amount intacto
    @Test
    void pagoParcialNoSobrescribeElMontoProgramado() {
        plan("300", "100", "100", "100");

        FeeDto dto = feeServices.registerPayment(1L, new BigDecimal("30"), LocalDate.now(), "EFECTIVO");

        Fee f1 = sale.getFees().get(0);
        assertEquals(Boolean.FALSE, dto.getPaid());
        assertEquals(Boolean.FALSE, f1.getPaid());
        assertEquals(0, new BigDecimal("100").compareTo(f1.getAmount()));
        assertEquals(0, new BigDecimal("30").compareTo(f1.getPaidAmount()));
        assertEquals(1, f1.getPayments().size());
        assertEquals(0, new BigDecimal("270").compareTo(sale.getRemainingAmount()));
        assertTrue(sale.getRemainingAmount().signum() >= 0);
    }

    // Invariante 5: cobrar nombrando una cuota posterior aplica FIFO desde la mas antigua
    @Test
    void cobrarCuotaPosteriorAplicaFifoDesdeLaMasAntigua() {
        plan("300", "100", "100", "100");

        FeeDto dto = feeServices.registerPayment(3L, new BigDecimal("100"), LocalDate.now(), "EFECTIVO");

        assertEquals(Integer.valueOf(1), dto.getNumberFee());
        assertEquals(Boolean.TRUE, sale.getFees().get(0).getPaid());
        assertEquals(Boolean.FALSE, sale.getFees().get(1).getPaid());
        assertEquals(0, sale.getFees().get(1).getPaidAmount().signum());
        assertEquals(0, sale.getFees().get(2).getPaidAmount().signum());
        assertEquals(0, new BigDecimal("200").compareTo(sale.getRemainingAmount()));
        assertEquals(SaleStatus.ACTIVE, sale.getStatus());
    }

    // Invariante 6: cascada 250 sobre 100x3
    @Test
    void cascada250SobreTresCuotasDe100() {
        plan("300", "100", "100", "100");

        FeeDto dto = feeServices.registerPayment(1L, new BigDecimal("250"), LocalDate.now(), "EFECTIVO");

        Fee f1 = sale.getFees().get(0);
        Fee f2 = sale.getFees().get(1);
        Fee f3 = sale.getFees().get(2);
        assertEquals(Integer.valueOf(1), dto.getNumberFee());
        assertEquals(Boolean.TRUE, f1.getPaid());
        assertEquals(0, new BigDecimal("100").compareTo(f1.getPaidAmount()));
        assertEquals(Boolean.TRUE, f2.getPaid());
        assertEquals(0, new BigDecimal("100").compareTo(f2.getPaidAmount()));
        assertEquals(Boolean.FALSE, f3.getPaid());
        assertEquals(3, sale.getFees().size());
        assertEquals(0, new BigDecimal("50").compareTo(f3.getPaidAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(f3.getAmount()));
        assertEquals(0, new BigDecimal("50").compareTo(sale.getRemainingAmount()));
        assertEquals(SaleStatus.ACTIVE, sale.getStatus());
    }

    // S2: con la deuda en cero la operacion queda COMPLETED con la fecha del cobro
    @Test
    void cobrarTodoElPlanCierraLaOperacionConLaFechaDelCobro() {
        plan("300", "100", "100", "100");
        LocalDate fecha = LocalDate.of(2026, 8, 10);

        feeServices.registerPayment(1L, new BigDecimal("300"), fecha, "EFECTIVO");

        assertEquals(0, sale.getRemainingAmount().signum());
        assertEquals(SaleStatus.COMPLETED, sale.getStatus());
        assertEquals(fecha, sale.getRealFinalDate());
        assertTrue(sale.getFees().stream().allMatch(f -> Boolean.TRUE.equals(f.getPaid())));
    }

    // Invariante 1: cuota ya pagada / venta anulada / monto <= 0 / fecha anterior no mutan
    @Test
    void rechazosDeCobroNoMutanEstado() {
        plan("300", "100", "100");
        Fee f1 = sale.getFees().get(0);
        f1.setPaid(true);
        f1.setPaidAmount(new BigDecimal("100"));
        sale.setRemainingAmount(new BigDecimal("200"));

        String antes = ss(sale) + "//" + es(f1);
        assertThrows(IllegalStateException.class,
                () -> feeServices.registerPayment(1L, new BigDecimal("50"), LocalDate.now(), "EFECTIVO"));
        assertEquals(antes, ss(sale) + "//" + es(f1));

        assertThrows(IllegalArgumentException.class,
                () -> feeServices.registerPayment(1L, BigDecimal.ZERO, LocalDate.now(), "EFECTIVO"));
        assertThrows(IllegalArgumentException.class,
                () -> feeServices.registerPayment(1L, new BigDecimal("-10"), LocalDate.now(), "EFECTIVO"));
        assertThrows(IllegalArgumentException.class,
                () -> feeServices.registerPayment(1L, new BigDecimal("50"),
                        sale.getDateCreation().minusDays(1), "EFECTIVO"));
        assertEquals(antes, ss(sale) + "//" + es(f1));
        sinPersistir();
    }

    @Test
    void rechazoDeVentaAnuladaNoMutaEstado() {
        plan("100", "100");
        sale.setStatus(SaleStatus.CANCELED);
        String antes = ss(sale) + "//" + es(sale.getFees().get(0));

        assertThrows(IllegalStateException.class,
                () -> feeServices.registerPayment(1L, new BigDecimal("50"), LocalDate.now(), "EFECTIVO"));

        assertEquals(antes, ss(sale) + "//" + es(sale.getFees().get(0)));
        sinPersistir();
    }

    @Test
    void rechazoDePostponeNoMutaEstado() {
        plan("200", "100", "100");

        assertThrows(IllegalArgumentException.class,
                () -> feeServices.postponeFee(1L, sale.getDateCreation().minusDays(1), null, new BigDecimal("50")));
        String antes = ss(sale) + "//" + es(sale.getFees().get(0));

        sale.setStatus(SaleStatus.CANCELED);
        assertThrows(IllegalStateException.class,
                () -> feeServices.postponeFee(1L, LocalDate.of(2026, 12, 1), null, new BigDecimal("50")));

        sale.setStatus(SaleStatus.ACTIVE);
        assertEquals(antes, ss(sale) + "//" + es(sale.getFees().get(0)));
        verify(feeRepository, never()).save(any(Fee.class));
    }

    // Invariante 2: conductas preexistentes que comparten el codigo tocado
    @Test
    void posponerLaFechaDeUnaCuotaImpagaSigueFuncionando() {
        plan("200", "100", "100");
        FeeDto dto = feeServices.postponeFee(1L, LocalDate.of(2026, 12, 1), null, null);

        assertEquals(LocalDate.of(2026, 12, 1), dto.getExpirationDate());
        assertEquals(0, new BigDecimal("100").compareTo(sale.getFees().get(0).getAmount()));
        assertEquals(0, new BigDecimal("200").compareTo(sale.getRemainingAmount()));
        verify(feeRepository).save(sale.getFees().get(0));
    }

    @Test
    void editarElMontoDeUnaCuotaCobradaSigueAjustandoElSaldo() {
        plan("300", "100", "100");
        Fee f1 = sale.getFees().get(0);
        f1.setPaid(true);
        f1.setPaidAmount(new BigDecimal("100"));
        f1.setLastPaymentAt(LocalDateTime.now());
        sale.setRemainingAmount(new BigDecimal("200"));

        feeServices.postponeFee(1L, null, null, new BigDecimal("150"));

        assertEquals(0, new BigDecimal("150").compareTo(f1.getAmount()));
        assertEquals(0, new BigDecimal("150").compareTo(f1.getPaidAmount()));
        assertEquals(Boolean.TRUE, f1.getPaid());
        assertEquals(0, new BigDecimal("150").compareTo(sale.getRemainingAmount()));
    }

    // Cobrar el resto de una cuota parcial cierra la operacion
    @Test
    void cobrarElRestoDeUnaCuotaParcialCierraLaOperacion() {
        plan("300", "100", "100", "100");
        feeServices.registerPayment(1L, new BigDecimal("250"), LocalDate.now(), "EFECTIVO");
        Fee f3 = sale.getFees().get(2);
        assertEquals(Boolean.FALSE, f3.getPaid());

        feeServices.registerPayment(3L, new BigDecimal("50"), LocalDate.now(), "EFECTIVO");

        assertEquals(Boolean.TRUE, f3.getPaid());
        assertEquals(0, new BigDecimal("100").compareTo(f3.getAmount()));
        assertEquals(0, sale.getRemainingAmount().signum());
        assertEquals(SaleStatus.COMPLETED, sale.getStatus());
    }

    // Dos cobros parciales sobre la misma cuota la cierran sin tocar el monto programado
    @Test
    void dosCobrosParcialesSobreLaMismaCuotaLaCierran() {
        plan("300", "100", "100", "100");
        feeServices.registerPayment(1L, new BigDecimal("40"), LocalDate.now(), "EFECTIVO");
        feeServices.registerPayment(1L, new BigDecimal("60"), LocalDate.now(), "EFECTIVO");

        Fee f1 = sale.getFees().get(0);
        assertEquals(Boolean.TRUE, f1.getPaid());
        assertEquals(0, new BigDecimal("100").compareTo(f1.getAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(f1.getPaidAmount()));
        assertEquals(2, f1.getPayments().size());
        assertEquals(0, new BigDecimal("200").compareTo(sale.getRemainingAmount()));
    }
}