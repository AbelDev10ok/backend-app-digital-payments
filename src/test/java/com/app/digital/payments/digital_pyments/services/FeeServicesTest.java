package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.PaymentMethod;
import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;

class FeeServicesTest {

    @Mock
    private IFeeRepository feeRepository;

    @Mock
    private ISaleRepository saleRepository;

    private FeeServices feeServices;

    private Sale sale;

    private Fee fee;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        feeServices = new FeeServices(feeRepository, saleRepository);

        sale = spy(new Sale());
        sale.setId(1L);
        sale.setDateCreation(LocalDate.now().minusDays(1));
        sale.setRemainingAmount(new BigDecimal("100"));
        sale.setAmountFee(new BigDecimal("100"));
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setTypePayments(Payments.MENSUAL);
        sale.setDescriptionProduct("Producto de prueba");
        sale.setFees(new ArrayList<>());

        Client client = new Client();
        client.setId(1L);
        client.setName("Juan");
        sale.setClient(client);

        fee = new Fee();
        fee.setId(1L);
        fee.setSale(sale);
        fee.setNumberFee(1);
        fee.setAmount(new BigDecimal("100"));
        fee.setExpirationDate(LocalDate.now());
        fee.setPaid(false);
        sale.getFees().add(fee);

        List<Fee> fees = new ArrayList<>(sale.getFees());
        when(feeRepository.findBySaleId(1L)).thenReturn(fees);
    }

    @Test
    void registerPaymentCompletaLaVentaAlCubrirElSaldo() {
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        FeeDto dto = feeServices.registerPayment(1L, new BigDecimal("100"), LocalDate.now(), "EFECTIVO");

        assertEquals(Boolean.TRUE, dto.getPaid());
        assertEquals(SaleStatus.COMPLETED, sale.getStatus());
        assertEquals(0, sale.getRemainingAmount().compareTo(BigDecimal.ZERO));
        verify(saleRepository).save(sale);
    }

    @Test
    void registerPaymentConEfectivo() {
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        FeeDto dto = feeServices.registerPayment(1L, new BigDecimal("100"), LocalDate.now(), "EFECTIVO");

        assertEquals("EFECTIVO", dto.getPaymentMethod());
        assertEquals(Boolean.TRUE, dto.getPaid());
    }

    @Test
    void registerPaymentConTransferencia() {
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        FeeDto dto = feeServices.registerPayment(1L, new BigDecimal("100"), LocalDate.now(), "TRANSFERENCIA");

        assertEquals("TRANSFERENCIA", dto.getPaymentMethod());
        assertEquals(Boolean.TRUE, dto.getPaid());
    }

    @Test
    void registerPaymentRechazaMetodoInvalido() {
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        assertThrows(IllegalArgumentException.class,
                () -> feeServices.registerPayment(1L, new BigDecimal("100"), LocalDate.now(), "CREDITO"));
    }

    @Test
    void registerPaymentRechazaMetodoNull() {
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        assertThrows(IllegalArgumentException.class,
                () -> feeServices.registerPayment(1L, new BigDecimal("100"), LocalDate.now(), null));
    }

    @Test
    void registerPaymentPermiteMontoMenorAlValorDeLaCuota() {
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        FeeDto dto = feeServices.registerPayment(1L, new BigDecimal("50"), LocalDate.now(), "EFECTIVO");

        assertEquals(Boolean.TRUE, dto.getPaid());
        assertEquals(0, new BigDecimal("50").compareTo(dto.getAmount()));
        assertEquals(0, new BigDecimal("50").compareTo(sale.getRemainingAmount()));
        assertEquals(SaleStatus.ACTIVE, sale.getStatus());
    }

    @Test
    void registerPaymentPermiteMontoMayorAlValorDeLaCuotaSinSuperarLaDeuda() {
        sale.setRemainingAmount(new BigDecimal("300"));
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        FeeDto dto = feeServices.registerPayment(1L, new BigDecimal("150"), LocalDate.now(), "EFECTIVO");

        assertEquals(Boolean.TRUE, dto.getPaid());
        assertEquals(0, new BigDecimal("150").compareTo(dto.getAmount()));
        assertEquals(0, new BigDecimal("150").compareTo(sale.getRemainingAmount()));
        assertEquals(SaleStatus.ACTIVE, sale.getStatus());
    }

    @Test
    void registerPaymentRechazaCuotaYaPagada() {
        fee.setPaid(true);
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        assertThrows(IllegalStateException.class,
                () -> feeServices.registerPayment(1L, new BigDecimal("100"), LocalDate.now(), "EFECTIVO"));
    }

    @Test
    void registerPaymentRechazaMontoNoPositivo() {
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        assertThrows(IllegalArgumentException.class,
                () -> feeServices.registerPayment(1L, BigDecimal.ZERO, LocalDate.now(), "EFECTIVO"));
    }

    @Test
    void registerPaymentNoAceptaFechasAnterioresACreacion() {
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        assertThrows(IllegalArgumentException.class,
                () -> feeServices.registerPayment(1L, new BigDecimal("100"),
                        sale.getDateCreation().minusDays(1), "EFECTIVO"));
    }

    @Test
    void getFeeByIdLanzaSiNoExiste() {
        when(feeRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> feeServices.getFeeById(999L));
    }

    @Test
    void registerPaymentRechazaFechaDuplicada() {
        Fee fee1 = new Fee();
        fee1.setId(1L);
        fee1.setSale(sale);
        fee1.setNumberFee(1);
        fee1.setAmount(new BigDecimal("100"));
        fee1.setExpirationDate(LocalDate.now());
        fee1.setPaid(true);
        fee1.setDatePayment(LocalDate.now());

        Fee fee2 = new Fee();
        fee2.setId(2L);
        fee2.setSale(sale);
        fee2.setNumberFee(2);
        fee2.setAmount(new BigDecimal("50"));
        fee2.setExpirationDate(LocalDate.now());
        fee2.setPaid(false);

        sale.getFees().clear();
        sale.getFees().add(fee1);
        sale.getFees().add(fee2);
        sale.setRemainingAmount(new BigDecimal("150"));

        List<Fee> fees = new ArrayList<>(sale.getFees());
        when(feeRepository.findBySaleId(1L)).thenReturn(fees);
        when(feeRepository.findById(2L)).thenReturn(java.util.Optional.of(fee2));

        assertThrows(IllegalArgumentException.class,
                () -> feeServices.registerPayment(2L, new BigDecimal("50"), LocalDate.now(), "EFECTIVO"));
    }

    @Test
    void postponerSinCambiosNoAlteraNada() {
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        feeServices.postponeFee(1L, null, null, null);

        assertEquals(0, new BigDecimal("100").compareTo(fee.getAmount()));
        assertEquals(Boolean.FALSE, fee.getPaid());
        verify(feeRepository, never()).save(any(Fee.class));
    }

    @Test
    void postponerCuotaImpagaCambiaFechaSinAlterarMonto() {
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        LocalDate nuevaFecha = LocalDate.now().plusDays(5);
        feeServices.postponeFee(1L, nuevaFecha, null, null);

        assertEquals(nuevaFecha, fee.getExpirationDate());
        assertEquals(Boolean.FALSE, fee.getPaid());
        assertEquals(0, new BigDecimal("100").compareTo(fee.getAmount()));
        verify(feeRepository).save(fee);
    }

    @Nested
    class PostponeFeeEdicionMonto {

        @BeforeEach
        void setUp() {
            // Cuota ya pagada con su monto aplicado al saldo de la venta
            fee.setPaid(true);
            fee.setAmount(new BigDecimal("100"));
            fee.setDatePayment(LocalDate.now());
            when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));
        }

        @Test
        void alReducirElMontoDevuelveElSaldoAlRemaining() {
            sale.setRemainingAmount(new BigDecimal("0")); // toda la venta ya estaba cubierta

            feeServices.postponeFee(1L, null, null, new BigDecimal("40"));

            // La cuota "devolvía" 60 hacia el saldo: 0 + (100 - 40) = 60
            assertEquals(0, new BigDecimal("60").compareTo(sale.getRemainingAmount()));
            assertEquals(0, new BigDecimal("40").compareTo(fee.getAmount()));
        }

        @Test
        void rechazaEdicionQueDejaRemainingNegativo() {
            // La venta ya está saldada (remaining 0) y se intenta subir el monto de la cuota
            // de 100 a 200: 0 + (100 - 200) = -100 → no puede quedar negativa
            sale.setRemainingAmount(new BigDecimal("0"));

            assertThrows(IllegalArgumentException.class,
                    () -> feeServices.postponeFee(1L, null, null, new BigDecimal("200")));
        }

        @Test
        void montoCeroDesmarcaElPago() {
            sale.setRemainingAmount(new BigDecimal("0"));

            feeServices.postponeFee(1L, null, null, BigDecimal.ZERO);

            assertEquals(Boolean.FALSE, fee.getPaid());
            assertNull(fee.getDatePayment());
            assertEquals(0, new BigDecimal("100").compareTo(sale.getRemainingAmount()));
        }
    }

    @Nested
    class PostponeFeeEdicionCuotaPagadaSinDuplicarProxima {

        @BeforeEach
        void setUp() {
            // Cuota 1 pagada y cuota 2 impaga ("próxima a cobrar"): agenda rodante.
            Fee pagada = new Fee();
            pagada.setId(1L);
            pagada.setSale(sale);
            pagada.setNumberFee(1);
            pagada.setAmount(new BigDecimal("100"));
            pagada.setExpirationDate(LocalDate.of(2026, 8, 6));
            pagada.setDatePayment(LocalDate.of(2026, 8, 6));
            pagada.setPaid(true);

            Fee proxima = new Fee();
            proxima.setId(2L);
            proxima.setSale(sale);
            proxima.setNumberFee(2);
            proxima.setAmount(new BigDecimal("100"));
            proxima.setExpirationDate(LocalDate.of(2026, 9, 6));
            proxima.setPaid(false);

            sale.getFees().clear();
            sale.getFees().add(pagada);
            sale.getFees().add(proxima);
            sale.setRemainingAmount(new BigDecimal("100"));

            when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(pagada));
        }

        @Test
        void editarFechaDePagoNoCreaUnaSegundaProximaAunqueCambieElSaldo() {
            feeServices.postponeFee(1L,
                    LocalDate.of(2026, 8, 6),
                    LocalDate.now(),
                    new BigDecimal("100"));

            assertEquals(2, sale.getFees().size());
            assertEquals(1, sale.getFees().stream().filter(f -> !f.getPaid()).count());
            assertEquals(SaleStatus.ACTIVE, sale.getStatus());
            verify(feeRepository, never()).save(any(Fee.class));
        }

        @Test
        void editarMontoConProximaExistenteNoDuplicaLaProxima() {
            feeServices.postponeFee(1L,
                    LocalDate.of(2026, 8, 6),
                    null,
                    new BigDecimal("80"));

            assertEquals(2, sale.getFees().size());
            assertEquals(1, sale.getFees().stream().filter(f -> !f.getPaid()).count());
            assertEquals(SaleStatus.ACTIVE, sale.getStatus());
            verify(feeRepository, never()).save(any(Fee.class));
        }
    }

    @Nested
    class DeleteFeeRestauracionDeSaldo {

        @Test
        void restauraElRemainingYReabreLaVentaCompletada() {
            // Dos cuotas; se elimina una pagada de 100 → la venta reabre su saldo
            Fee pagada = new Fee();
            pagada.setId(1L);
            pagada.setSale(sale);
            pagada.setNumberFee(1);
            pagada.setAmount(new BigDecimal("100"));
            pagada.setExpirationDate(LocalDate.of(2026, 8, 6));
            pagada.setPaid(true);
            pagada.setDatePayment(LocalDate.now());

            Fee pagada2 = new Fee();
            pagada2.setId(2L);
            pagada2.setSale(sale);
            pagada2.setNumberFee(2);
            pagada2.setAmount(new BigDecimal("100"));
            pagada2.setExpirationDate(LocalDate.of(2026, 9, 6));
            pagada2.setPaid(true);
            pagada2.setDatePayment(LocalDate.now());

            sale.getFees().clear();
            sale.getFees().add(pagada);
            sale.getFees().add(pagada2);
            sale.setRemainingAmount(new BigDecimal("0"));
            sale.setStatus(SaleStatus.COMPLETED);
            sale.setRealFinalDate(LocalDate.now());

            when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(pagada));

            feeServices.deleteFee(1L);

            assertEquals(0, new BigDecimal("100").compareTo(sale.getRemainingAmount()));
            assertEquals(SaleStatus.ACTIVE, sale.getStatus());
            assertNull(sale.getRealFinalDate());
            // La cuota eliminada (pagada) ya no pertenece a la venta
            assertEquals(0,
                    sale.getFees().stream().filter(f -> Long.valueOf(1L).equals(f.getId())).count());
            verify(feeRepository).delete(pagada);
        }

        @Test
        void rechazaEliminarLaUnicaCuotaDeLaVenta() {
            when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

            assertThrows(IllegalArgumentException.class, () -> feeServices.deleteFee(1L));
            assertEquals(1, sale.getFees().size());
            verify(feeRepository, never()).delete(fee);
        }

        @Test
        void conSaldoRestanteYCuotasPagadasRegeneraLaCuotaSiguiente() {
            // Dos cuotas pagadas y la venta aún con saldo restante; al eliminar una,
            // el resto queda sin cuotas impagas y se regenera la siguiente.
            Fee pagada = new Fee();
            pagada.setId(1L);
            pagada.setSale(sale);
            pagada.setNumberFee(1);
            pagada.setAmount(new BigDecimal("100"));
            pagada.setExpirationDate(LocalDate.of(2026, 8, 6));
            pagada.setPaid(true);
            pagada.setDatePayment(LocalDate.now());

            Fee pagada2 = new Fee();
            pagada2.setId(2L);
            pagada2.setSale(sale);
            pagada2.setNumberFee(2);
            pagada2.setAmount(new BigDecimal("100"));
            pagada2.setExpirationDate(LocalDate.of(2026, 9, 6));
            pagada2.setPaid(true);
            pagada2.setDatePayment(LocalDate.now());

            sale.getFees().clear();
            sale.getFees().add(pagada);
            sale.getFees().add(pagada2);
            sale.setRemainingAmount(new BigDecimal("100"));

            when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(pagada));

            feeServices.deleteFee(1L);

            // remaining restaurado = 100 + 100 = 200
            assertEquals(0, new BigDecimal("200").compareTo(sale.getRemainingAmount()));

            // regenera la cuota siguiente a partir de la última pagada (cuota 2)
            ArgumentCaptor<Fee> captor = ArgumentCaptor.forClass(Fee.class);
            verify(feeRepository).save(captor.capture());
            Fee regenerada = captor.getValue();
            assertEquals(LocalDate.of(2026, 9, 6).plusMonths(1), regenerada.getExpirationDate());
            assertEquals(3, regenerada.getNumberFee());
        }

        @Test
        void sinSaldoRestanteNoRegeneraCuota() {
            // Dos cuotas, ambas sin monto cobrado y la venta sin saldo pendiente;
            // al eliminar una, el remaining no queda positivo y no se regenera cuota.
            Fee impagaSinMonto = new Fee();
            impagaSinMonto.setId(1L);
            impagaSinMonto.setSale(sale);
            impagaSinMonto.setNumberFee(1);
            impagaSinMonto.setAmount(BigDecimal.ZERO);
            impagaSinMonto.setExpirationDate(LocalDate.of(2026, 8, 6));
            impagaSinMonto.setPaid(false);

            Fee otra = new Fee();
            otra.setId(2L);
            otra.setSale(sale);
            otra.setNumberFee(2);
            otra.setAmount(BigDecimal.ZERO);
            otra.setExpirationDate(LocalDate.of(2026, 9, 6));
            otra.setPaid(false);

            sale.getFees().clear();
            sale.getFees().add(impagaSinMonto);
            sale.getFees().add(otra);
            sale.setRemainingAmount(new BigDecimal("0"));

            when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(impagaSinMonto));

            feeServices.deleteFee(1L);

            assertEquals(0, new BigDecimal("0").compareTo(sale.getRemainingAmount()));
            verify(feeRepository, never()).save(any(Fee.class));
        }

        @Test
        void eliminarCuotaImpagaNoAlteraLaDeuda() {
            // Cuota 1 pagada y cuota 2 impaga; eliminar la impaga no cambia la deuda
            // porque ese monto nunca fue cobrado.
            Fee pagada = new Fee();
            pagada.setId(1L);
            pagada.setSale(sale);
            pagada.setNumberFee(1);
            pagada.setAmount(new BigDecimal("100"));
            pagada.setExpirationDate(LocalDate.of(2026, 8, 6));
            pagada.setDatePayment(LocalDate.now());
            pagada.setPaid(true);

            Fee impaga = new Fee();
            impaga.setId(2L);
            impaga.setSale(sale);
            impaga.setNumberFee(2);
            impaga.setAmount(new BigDecimal("100"));
            impaga.setExpirationDate(LocalDate.of(2026, 9, 6));
            impaga.setPaid(false);

            sale.getFees().clear();
            sale.getFees().add(pagada);
            sale.getFees().add(impaga);
            sale.setRemainingAmount(new BigDecimal("100"));

            when(feeRepository.findById(2L)).thenReturn(java.util.Optional.of(impaga));

            feeServices.deleteFee(2L);

            // La deuda se mantiene en 100 y no se regenera la cuota eliminada.
            assertEquals(0, new BigDecimal("100").compareTo(sale.getRemainingAmount()));
            assertEquals(SaleStatus.ACTIVE, sale.getStatus());
            assertEquals(1, sale.getFees().size());
            verify(feeRepository).delete(impaga);
            verify(feeRepository, never()).save(any(Fee.class));
        }

        @Test
        void eliminarCuotaImpagaConMasImpagasNoAlteraLaDeuda() {
            // Dos cuotas impagas de 100 cada una y deuda de 200; eliminar una
            // no cambia la deuda (no hubo cobro).
            Fee impaga1 = new Fee();
            impaga1.setId(1L);
            impaga1.setSale(sale);
            impaga1.setNumberFee(1);
            impaga1.setAmount(new BigDecimal("100"));
            impaga1.setExpirationDate(LocalDate.of(2026, 8, 6));
            impaga1.setPaid(false);

            Fee impaga2 = new Fee();
            impaga2.setId(2L);
            impaga2.setSale(sale);
            impaga2.setNumberFee(2);
            impaga2.setAmount(new BigDecimal("100"));
            impaga2.setExpirationDate(LocalDate.of(2026, 9, 6));
            impaga2.setPaid(false);

            sale.getFees().clear();
            sale.getFees().add(impaga1);
            sale.getFees().add(impaga2);
            sale.setRemainingAmount(new BigDecimal("200"));

            when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(impaga1));

            feeServices.deleteFee(1L);

            assertEquals(0, new BigDecimal("200").compareTo(sale.getRemainingAmount()));
            assertEquals(SaleStatus.ACTIVE, sale.getStatus());
            assertEquals(1, sale.getFees().size());
            verify(feeRepository).delete(impaga1);
        }

        @Test
        void eliminarUltimaCuotaImpagaNoRegeneraYDejaDeudaConsistente() {
            Fee pagada = new Fee();
            pagada.setId(1L);
            pagada.setSale(sale);
            pagada.setNumberFee(1);
            pagada.setAmount(new BigDecimal("100"));
            pagada.setExpirationDate(LocalDate.of(2026, 8, 6));
            pagada.setPaid(true);
            pagada.setDatePayment(LocalDate.now());

            Fee impaga = new Fee();
            impaga.setId(2L);
            impaga.setSale(sale);
            impaga.setNumberFee(2);
            impaga.setAmount(new BigDecimal("100"));
            impaga.setExpirationDate(LocalDate.of(2026, 9, 6));
            impaga.setPaid(false);

            sale.getFees().clear();
            sale.getFees().add(pagada);
            sale.getFees().add(impaga);
            sale.setRemainingAmount(new BigDecimal("100"));

            when(feeRepository.findById(2L)).thenReturn(java.util.Optional.of(impaga));

            feeServices.deleteFee(2L);

            assertEquals(0, new BigDecimal("100").compareTo(sale.getRemainingAmount()));
            assertEquals(SaleStatus.ACTIVE, sale.getStatus());
            assertEquals(1, sale.getFees().size());
            assertEquals(Boolean.TRUE, sale.getFees().get(0).getPaid());
            verify(feeRepository).delete(impaga);
            verify(feeRepository, never()).save(any(Fee.class));
        }
    }

    @Nested
    class RegisterPaymentSobrepago {

        @Test
        void rechazaMontoMayorAlSaldoRestante() {
            // La cuota vale 200 pero a la venta solo le quedan 100 por cobrar
            sale.setRemainingAmount(new BigDecimal("100"));
            fee.setAmount(new BigDecimal("200"));
            when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

            assertThrows(IllegalArgumentException.class,
                    () -> feeServices.registerPayment(1L, new BigDecimal("200"), LocalDate.now(), "EFECTIVO"));
            // Sin mutación del saldo ni cobro
            assertEquals(0, new BigDecimal("100").compareTo(sale.getRemainingAmount()));
            assertEquals(Boolean.FALSE, fee.getPaid());
        }
    }
}
