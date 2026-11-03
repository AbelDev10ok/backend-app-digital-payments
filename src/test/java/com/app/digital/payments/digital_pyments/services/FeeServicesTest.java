package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Payment;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;
import com.app.digital.payments.digital_pyments.models.dtos.PaymentDto;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.IPaymentRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.PaymentMethod;
import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;

class FeeServicesTest {

    @Mock
    private IFeeRepository feeRepository;

    @Mock
    private ISaleRepository saleRepository;

    @Mock
    private IPaymentRepository paymentRepository;

    private FeeServices feeServices;

    private Sale sale;

    private Fee fee;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        feeServices = new FeeServices(feeRepository, saleRepository, paymentRepository);

        sale = spy(new Sale());
        sale.setId(1L);
        sale.setDateCreation(LocalDate.now().minusDays(1));
        sale.setPriceTotal(new BigDecimal("100"));
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
    void registerPaymentParcialDejaLaCuotaAbiertaSinCrearCuotas() {
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        FeeDto dto = feeServices.registerPayment(1L, new BigDecimal("50"), LocalDate.now(), "EFECTIVO");

        // Un pago parcial no cierra la cuota ni crea cuotas nuevas: la cuota
        // sigue abierta por su saldo y su monto programado no se reescribe
        assertEquals(Boolean.FALSE, dto.getPaid());
        assertEquals(0, new BigDecimal("100").compareTo(dto.getAmount()));
        assertEquals(0, new BigDecimal("50").compareTo(dto.getPaidAmount()));
        assertEquals(0, new BigDecimal("50").compareTo(sale.getRemainingAmount()));
        assertEquals(SaleStatus.ACTIVE, sale.getStatus());
        assertEquals(1, fee.getPayments().size());
        // El cronograma no crece: la única cuota del plan sigue siendo esta
        assertEquals(1, sale.getFees().size());
    }

    @Test
    void registerPaymentGuardaElHorarioDelCobro() {
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));
        LocalDate hoy = LocalDate.now();

        FeeDto dto = feeServices.registerPayment(1L, new BigDecimal("100"), hoy, "EFECTIVO");

        assertEquals(Boolean.TRUE, dto.getPaid());
        assertEquals(1, fee.getPayments().size());
        // El backend sella la hora: el historial siempre guarda fecha y hora
        assertNotNull(dto.getLastPaymentAt());
        assertEquals(hoy, dto.getLastPaymentAt().toLocalDate());
    }

    @Test
    void registerPaymentRechazaMontoMayorAlSaldoRestanteDeLaVenta() {
        sale.setPriceTotal(new BigDecimal("80"));
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        assertThrows(IllegalArgumentException.class,
                () -> feeServices.registerPayment(1L, new BigDecimal("100"), LocalDate.now(), "EFECTIVO"));

        assertEquals(0, new BigDecimal("80").compareTo(sale.getRemainingAmount()));
        assertEquals(Boolean.FALSE, fee.getPaid());
        assertEquals(0, fee.getPayments().size());
    }

    @Test
    void registerPaymentPermiteCobrarMasQueElMontoDeLaCuota() {
        // El tope es la deuda de la venta, no el monto de la cuota: un cobro de
        // 150 sobre una cuota de 100 la cierra y el excedente cascada a la siguiente
        sale.setPriceTotal(new BigDecimal("300"));
        Fee segunda = nuevaCuota(2L, 2, "100");
        Fee tercera = nuevaCuota(3L, 3, "100");
        sale.getFees().add(segunda);
        sale.getFees().add(tercera);
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        FeeDto dto = feeServices.registerPayment(1L, new BigDecimal("150"), LocalDate.now(), "EFECTIVO");

        // La cuota 1 se cierra por su monto programado, sin reescribirlo
        assertEquals(Boolean.TRUE, dto.getPaid());
        assertEquals(0, new BigDecimal("100").compareTo(dto.getAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(fee.getPaidAmount()));
        // El excedente de 50 entra en la cuota 2, que sigue abierta
        assertEquals(Boolean.FALSE, segunda.getPaid());
        assertEquals(0, new BigDecimal("100").compareTo(segunda.getAmount()));
        assertEquals(0, new BigDecimal("50").compareTo(segunda.getPaidAmount()));
        // La 3 no se toca
        assertEquals(0, tercera.getPaidAmount().signum());
        assertEquals(0, new BigDecimal("150").compareTo(sale.getRemainingAmount()));
        assertEquals(SaleStatus.ACTIVE, sale.getStatus());
    }

    @Test
    void registerPaymentNoCreaCuotasNuevas() {
        // El plan es fijo: cobrar no agenda cuotas nuevas ni siquiera dejando
        // deuda pendiente (la agenda rodante se eliminó en esta etapa)
        sale.setAmountFee(new BigDecimal("100"));
        sale.setPriceTotal(new BigDecimal("300"));
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        feeServices.registerPayment(1L, new BigDecimal("100"), LocalDate.now(), "EFECTIVO");

        assertEquals(Boolean.TRUE, fee.getPaid());
        assertEquals(1, sale.getFees().size());
        assertEquals(0, new BigDecimal("200").compareTo(sale.getRemainingAmount()));
    }

    @Test
    void cobrarNoAcotaElMontoDeLasCuotasImpagas() {
        // Venta de 150 con cuotas de 100: al cobrar la primera, la cuota 2 sigue
        // valiendo su monto programado (no se re-deriva del saldo)
        sale.setAmountFee(new BigDecimal("100"));
        sale.setPriceTotal(new BigDecimal("150"));
        Fee segunda = nuevaCuota(2L, 2, "100");
        sale.getFees().add(segunda);
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        feeServices.registerPayment(1L, new BigDecimal("100"), LocalDate.now(), "EFECTIVO");

        assertEquals(Boolean.TRUE, fee.getPaid());
        assertEquals(0, new BigDecimal("100").compareTo(segunda.getAmount()));
        assertEquals(Boolean.FALSE, segunda.getPaid());
        assertEquals(0, new BigDecimal("50").compareTo(sale.getRemainingAmount()));
    }

    @Test
    void registerPaymentDeUnaCuotaPosteriorCobraPrimeroLaMasAntigua() {
        // Se pide cobrar la cuota 2 pero la 1 sigue impaga: la deuda vieja se
        // cubre primero y no es un error (cascada FIFO)
        sale.setPriceTotal(new BigDecimal("300"));
        Fee segunda = nuevaCuota(2L, 2, "100");
        Fee tercera = nuevaCuota(3L, 3, "100");
        sale.getFees().add(segunda);
        sale.getFees().add(tercera);
        when(feeRepository.findById(2L)).thenReturn(java.util.Optional.of(segunda));

        FeeDto dto = feeServices.registerPayment(2L, new BigDecimal("100"), LocalDate.now(), "EFECTIVO");

        // El cobro entró en la cuota 1 (la más antigua), no en la nombrada
        assertEquals(Integer.valueOf(1), dto.getNumberFee());
        assertEquals(Boolean.TRUE, fee.getPaid());
        assertEquals(Boolean.FALSE, segunda.getPaid());
        assertEquals(0, segunda.getPaidAmount().signum());
        assertEquals(0, new BigDecimal("200").compareTo(sale.getRemainingAmount()));
    }

    @Test
    void cobrarLaDeudaCompletaEliminaLasCuotasImpagasSobrantes() {
        // Cobrar el total de la venta cierra la operación y las cuotas impagas
        // que ya no representan deuda desaparecen del cronograma
        Fee sobrante = nuevaCuota(2L, 2, "100");
        sale.getFees().add(sobrante);
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        feeServices.registerPayment(1L, new BigDecimal("100"), LocalDate.now(), "EFECTIVO");

        assertEquals(SaleStatus.COMPLETED, sale.getStatus());
        assertEquals(LocalDate.now(), sale.getRealFinalDate());
        assertEquals(0, sale.getRemainingAmount().signum());
        assertEquals(1, sale.getFees().size());
        assertEquals(Boolean.TRUE, fee.getPaid());
        verify(feeRepository).delete(sobrante);
    }

    @Test
    void registerPaymentRechazaCuandoElPlanNoCubreElMontoSinAplicarNada() {
        // Venta de 200 con una sola cuota impaga de 100 (el plan no cubre toda
        // la deuda): un cobro de 150 no se puede asignar entero, así que se
        // rechaza con error claro y sin dejar pagos a medias
        sale.setPriceTotal(new BigDecimal("200"));
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> feeServices.registerPayment(1L, new BigDecimal("150"), LocalDate.now(), "EFECTIVO"));

        assertEquals("El plan de cuotas no cubre la deuda restante; revisá las cuotas de la operación",
                error.getMessage());
        assertEquals(0, new BigDecimal("200").compareTo(sale.getRemainingAmount()));
        assertEquals(Boolean.FALSE, fee.getPaid());
        assertEquals(0, fee.getPaidAmount().signum());
        assertEquals(0, fee.getPayments().size());
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    private Fee nuevaCuota(Long id, int numero, String monto) {
        Fee cuota = new Fee();
        cuota.setId(id);
        cuota.setSale(sale);
        cuota.setNumberFee(numero);
        cuota.setAmount(new BigDecimal(monto));
        cuota.setExpirationDate(LocalDate.of(2026, 8, 6).plusMonths(numero - 1L));
        cuota.setPaid(false);
        return cuota;
    }

    @Test
    void registerPaymentRechazaCuotaYaPagada() {
        fee.setPaid(true);
        fee.setPaidAmount(new BigDecimal("100"));
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        assertThrows(IllegalStateException.class,
                () -> feeServices.registerPayment(1L, new BigDecimal("100"), LocalDate.now(), "EFECTIVO"));
    }

    @Test
    void registerPaymentRechazaCuotaDeVentaAnulada() {
        sale.setStatus(SaleStatus.CANCELED);
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        assertThrows(IllegalStateException.class,
                () -> feeServices.registerPayment(1L, new BigDecimal("50"), LocalDate.now(), "EFECTIVO"));

        verify(saleRepository, never()).save(any(Sale.class));
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
    void registerPaymentPermiteCobrarDosCuotasDistintasElMismoDia() {
        // La cuota 1 ya está cobrada el mismo día; cobrar la cuota 2 no se bloquea
        Fee fee2 = new Fee();
        fee2.setId(2L);
        fee2.setSale(sale);
        fee2.setNumberFee(2);
        fee2.setAmount(new BigDecimal("50"));
        fee2.setExpirationDate(LocalDate.now());
        fee2.setPaid(false);

        // La cuota 1 ya entró: la deuda que queda es sólo la cuota 2
        fee.setPaid(true);
        fee.setPaidAmount(new BigDecimal("100"));
        sale.getFees().add(fee2);
        sale.setPriceTotal(new BigDecimal("150"));

        when(feeRepository.findById(2L)).thenReturn(java.util.Optional.of(fee2));

        feeServices.registerPayment(2L, new BigDecimal("50"), LocalDate.now(), "EFECTIVO");

        assertEquals(Boolean.TRUE, fee2.getPaid());
        assertEquals(SaleStatus.COMPLETED, sale.getStatus());
    }

    @Test
    void getPaymentsByFeeIdDevuelveElHistorialConHorario() {
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));
        Payment p1 = new Payment(fee, new BigDecimal("40"), LocalDateTime.now().minusHours(3), PaymentMethod.EFECTIVO);
        Payment p2 = new Payment(fee, new BigDecimal("60"), LocalDateTime.now(), PaymentMethod.TRANSFERENCIA);
        when(paymentRepository.findByFeeIdOrderByPaidAtAscIdAsc(1L)).thenReturn(List.of(p1, p2));

        List<PaymentDto> pagos = feeServices.getPaymentsByFeeId(1L);

        assertEquals(2, pagos.size());
        assertEquals("EFECTIVO", pagos.get(0).getPaymentMethod());
        assertEquals("TRANSFERENCIA", pagos.get(1).getPaymentMethod());
        assertNotNull(pagos.get(0).getPaidAt());
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

    @Test
    void postponeFeeRechazaCuotaDeVentaAnulada() {
        sale.setStatus(SaleStatus.CANCELED);
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        assertThrows(IllegalStateException.class,
                () -> feeServices.postponeFee(1L, LocalDate.now().plusDays(5), null, null));

        verify(feeRepository, never()).save(any(Fee.class));
    }

    @Nested
    class PostponeFeeEdicionMonto {

        @BeforeEach
        void setUp() {
            // Cuota ya cobrada con su monto aplicado al saldo de la venta
            fee.setPaid(true);
            fee.setPaidAmount(new BigDecimal("100"));
            fee.setAmount(new BigDecimal("100"));
            fee.setLastPaymentAt(LocalDateTime.now());
            when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));
        }

        @Test
        void alReducirElMontoDevuelveElSaldoAlRemaining() {
            sale.setRemainingAmount(new BigDecimal("0")); // toda la venta ya estaba cubierta

            feeServices.postponeFee(1L, null, null, new BigDecimal("40"));

            // La cuota "devuelve" 60 hacia el saldo: 0 + (100 - 40) = 60
            assertEquals(0, new BigDecimal("60").compareTo(sale.getRemainingAmount()));
            assertEquals(0, new BigDecimal("40").compareTo(fee.getAmount()));
            // El cobro registrado se ajusta al nuevo monto de la cuota
            assertEquals(0, new BigDecimal("40").compareTo(fee.getPaidAmount()));
            assertEquals(Boolean.TRUE, fee.getPaid());
        }

        @Test
        void subirElMontoNoDejaElSaldoNegativo() {
            // La venta ya estaba saldada y se sube el monto de la cuota de 100 a 200:
            // el saldo se acota en 0 y el cobro registrado se ajusta al nuevo monto
            sale.setRemainingAmount(new BigDecimal("0"));

            feeServices.postponeFee(1L, null, null, new BigDecimal("200"));

            assertEquals(0, BigDecimal.ZERO.compareTo(sale.getRemainingAmount()));
            assertEquals(0, new BigDecimal("200").compareTo(fee.getPaidAmount()));
            assertEquals(Boolean.TRUE, fee.getPaid());
        }

        @Test
        void cambiarLaFechaDePagoMueveElUltimoCobroAunqueHayaVarios() {
            // Dos cobros parciales: la "fecha de pago" editable es la del último
            Payment primerPago = new Payment(fee, new BigDecimal("40"), LocalDateTime.of(2026, 8, 1, 9, 0),
                    PaymentMethod.EFECTIVO);
            primerPago.setId(10L);
            Payment segundoPago = new Payment(fee, new BigDecimal("60"), LocalDateTime.of(2026, 8, 5, 15, 30),
                    PaymentMethod.EFECTIVO);
            segundoPago.setId(11L);
            fee.applyPayment(primerPago);
            fee.applyPayment(segundoPago);

            LocalDate nuevaFecha = LocalDate.now().plusDays(3);

            feeServices.postponeFee(1L, null, nuevaFecha, null);

            // El último cobro se mueve a la fecha nueva conservando su hora
            assertEquals(nuevaFecha.atTime(15, 30), segundoPago.getPaidAt());
            assertEquals(nuevaFecha.atTime(15, 30), fee.getLastPaymentAt());
            // El cobro anterior queda intacto
            assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), primerPago.getPaidAt());
            verify(paymentRepository).save(segundoPago);
        }

        @Test
        void montoCeroDesmarcaElPago() {
            sale.setRemainingAmount(new BigDecimal("0"));

            feeServices.postponeFee(1L, null, null, BigDecimal.ZERO);

            assertEquals(Boolean.FALSE, fee.getPaid());
            assertNull(fee.getLastPaymentAt());
            assertEquals(0, new BigDecimal("100").compareTo(sale.getRemainingAmount()));
        }
    }

    @Test
    void editarElMontoDeUnaCuotaImpagaNoMueveLaDeuda() {
        // Cuota impaga de 100 y deuda de 100: editarla cambia el valor de esa cuota
        // (lo que se va a cobrar), pero la deuda no se mueve porque no entró plata
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        feeServices.postponeFee(1L, null, null, new BigDecimal("40"));

        assertEquals(0, new BigDecimal("100").compareTo(sale.getRemainingAmount()));
        assertEquals(0, new BigDecimal("40").compareTo(fee.getAmount()));
        assertEquals(Boolean.FALSE, fee.getPaid());

        // Y subirla tampoco la mueve
        feeServices.postponeFee(1L, null, null, new BigDecimal("150"));

        assertEquals(0, new BigDecimal("100").compareTo(sale.getRemainingAmount()));
        assertEquals(Boolean.FALSE, fee.getPaid());
    }

    @Test
    void alBajarElMontoSeRecortaElCobroRegistradoConservandoElHorario() {
        // Cuota cobrada con dos cobros parciales (60 + 40 = 100)
        Payment p1 = new Payment(fee, new BigDecimal("60"), LocalDateTime.of(2026, 8, 1, 9, 0),
                PaymentMethod.EFECTIVO);
        Payment p2 = new Payment(fee, new BigDecimal("40"), LocalDateTime.of(2026, 8, 5, 15, 30),
                PaymentMethod.EFECTIVO);
        fee.applyPayment(p1);
        fee.applyPayment(p2);
        sale.setPriceTotal(new BigDecimal("100"));
        sale.setRemainingAmount(BigDecimal.ZERO);
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        feeServices.postponeFee(1L, null, null, new BigDecimal("50"));

        // El cobro registrado queda en 50 (se recorta el sobrante) con el horario intacto
        assertEquals(0, new BigDecimal("50").compareTo(fee.getAmount()));
        assertEquals(0, new BigDecimal("50").compareTo(fee.getPaidAmount()));
        assertEquals(Boolean.TRUE, fee.getPaid());
        assertEquals(1, fee.getPayments().size());
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), fee.getPayments().get(0).getPaidAt());
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), fee.getLastPaymentAt());
        // Y la diferencia vuelve al saldo
        assertEquals(0, new BigDecimal("50").compareTo(sale.getRemainingAmount()));
    }

    @Test
    void alSubirElMontoDeUnaCuotaCobradaQuedaUnCobroDeAjusteYElSaldoBaja() {
        Payment pago = new Payment(fee, new BigDecimal("100"), LocalDateTime.of(2026, 8, 1, 9, 0),
                PaymentMethod.EFECTIVO);
        fee.applyPayment(pago);
        sale.setPriceTotal(new BigDecimal("300"));
        sale.setRemainingAmount(new BigDecimal("200"));
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        feeServices.postponeFee(1L, null, null, new BigDecimal("150"));

        // El saldo baja por la diferencia (200 - 50) y el ajuste conserva el horario
        assertEquals(0, new BigDecimal("150").compareTo(sale.getRemainingAmount()));
        assertEquals(0, new BigDecimal("150").compareTo(fee.getPaidAmount()));
        assertEquals(Boolean.TRUE, fee.getPaid());
        assertEquals(2, fee.getPayments().size());
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), fee.getPayments().get(1).getPaidAt());
    }

    @Test
    void elSaldoNuncaSuperaElTotalAlCobrarYEditarCuotas() {
        // Repro del bug: venta de 300 en 3 cuotas de 100
        sale.setPriceTotal(new BigDecimal("300"));
        sale.setRemainingAmount(new BigDecimal("300"));
        sale.setAmountFee(new BigDecimal("100"));
        fee.setAmount(new BigDecimal("100"));
        Fee cuota2 = nuevaCuota(2L, 2, "100");
        Fee cuota3 = nuevaCuota(3L, 3, "100");
        sale.getFees().add(cuota2);
        sale.getFees().add(cuota3);
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        // 1) Cobro completo de la cuota 1: el saldo baja a 200, sin cuotas nuevas
        feeServices.registerPayment(1L, new BigDecimal("100"), LocalDate.now(), "EFECTIVO");
        assertEquals(0, new BigDecimal("200").compareTo(sale.getRemainingAmount()));
        assertEquals(3, sale.getFees().size());

        // 2) Editar el monto de la cuota cobrada (100 → 50): la diferencia vuelve al saldo
        feeServices.postponeFee(1L, null, null, new BigDecimal("50"));
        assertEquals(0, new BigDecimal("250").compareTo(sale.getRemainingAmount()));
        assertEquals(0, new BigDecimal("50").compareTo(fee.getPaidAmount()));
        assertTrue(sale.getRemainingAmount().compareTo(sale.getPriceTotal()) <= 0);
    }

    @Test
    void editarElMontoDeUnaCuotaViejaCobradaCuentaSuMontoComoCobrado() {
        // Cuota cobrada antes del log de pagos: paid=true, sin paidAmount y sin
        // filas en payment. El monto de la cuota era lo cobrado, así que bajarlo
        // de 160.000 a 150.000 tiene que dejar 10.000 de deuda y reabrir la venta.
        sale.setPriceTotal(new BigDecimal("160000"));
        sale.setAmountFee(new BigDecimal("10000"));
        sale.setRemainingAmount(BigDecimal.ZERO);
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setRealFinalDate(LocalDate.now());
        fee.setAmount(new BigDecimal("160000"));
        fee.setPaid(true);
        fee.setPaidAmount(BigDecimal.ZERO);
        fee.setLastPaymentAt(LocalDateTime.now());
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(fee));

        feeServices.postponeFee(1L, null, null, new BigDecimal("150000"));

        assertEquals(0, new BigDecimal("10000").compareTo(sale.getRemainingAmount()));
        assertEquals(SaleStatus.ACTIVE, sale.getStatus());
        assertNull(sale.getRealFinalDate());
        // La cuota sigue pagada y su cobro registrado queda en el nuevo monto
        assertEquals(Boolean.TRUE, fee.getPaid());
        assertEquals(0, new BigDecimal("150000").compareTo(fee.getPaidAmount()));
        // Y se agenda una cuota impaga por la deuda
        Fee agendada = sale.getFees().stream()
                .filter(f -> f.getNumberFee() == 2)
                .findFirst()
                .orElseThrow();
        assertEquals(0, new BigDecimal("10000").compareTo(agendada.getAmount()));
        assertEquals(Boolean.FALSE, agendada.getPaid());
    }

    @Test
    void editarLaCuotaCobradaHastaElTotalEliminaLaCuotaSobranteYCompletaLaVenta() {
        // Venta de 170: se cobro 160 en la primera cuota y quedo agendada una cuota
        // por la deuda (10). Al re-declarar la cuota cobrada en 170 la venta queda
        // saldada, asi que la cuota por cobrar sobra y tiene que desaparecer.
        Fee cobrada = new Fee();
        cobrada.setId(1L);
        cobrada.setSale(sale);
        cobrada.setNumberFee(1);
        cobrada.setAmount(new BigDecimal("160"));
        cobrada.setExpirationDate(LocalDate.of(2026, 8, 6));
        cobrada.setPaid(true);
        cobrada.setPaidAmount(new BigDecimal("160"));
        cobrada.setLastPaymentAt(LocalDateTime.of(2026, 8, 6, 10, 0));
        cobrada.setPaymentMethod(PaymentMethod.EFECTIVO);
        cobrada.getPayments().add(new Payment(cobrada, new BigDecimal("160"),
                LocalDateTime.of(2026, 8, 6, 10, 0), PaymentMethod.EFECTIVO));

        Fee sobrante = new Fee();
        sobrante.setId(2L);
        sobrante.setSale(sale);
        sobrante.setNumberFee(2);
        sobrante.setAmount(new BigDecimal("10"));
        sobrante.setExpirationDate(LocalDate.of(2026, 9, 6));
        sobrante.setPaid(false);

        sale.getFees().clear();
        sale.getFees().add(cobrada);
        sale.getFees().add(sobrante);
        sale.setPriceTotal(new BigDecimal("170"));
        sale.setRemainingAmount(new BigDecimal("10"));
        sale.setStatus(SaleStatus.ACTIVE);
        when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(cobrada));

        feeServices.postponeFee(1L, null, null, new BigDecimal("170"));

        assertEquals(0, sale.getRemainingAmount().signum());
        assertEquals(SaleStatus.COMPLETED, sale.getStatus());
        assertNotNull(sale.getRealFinalDate());
        // El cobro de ajuste deja la cuota cobrada en su nuevo monto
        assertEquals(0, new BigDecimal("170").compareTo(cobrada.getPaidAmount()));
        assertEquals(Boolean.TRUE, cobrada.getPaid());
        // La cuota que iba a cubrir la deuda se elimina
        assertFalse(sale.getFees().contains(sobrante));
        verify(feeRepository).delete(sobrante);
    }

    @Test
    void cobrarElUltimoTramoDeLaDeudaEliminaLaCuotaSobrante() {
        // Misma venta de 170 pero saldada por el cobro de la deuda (10): la cuota
        // sobrante que habia quedado agendada ya no se cobra.
        Fee cobrada = new Fee();
        cobrada.setId(1L);
        cobrada.setSale(sale);
        cobrada.setNumberFee(1);
        cobrada.setAmount(new BigDecimal("160"));
        cobrada.setExpirationDate(LocalDate.of(2026, 8, 6));
        cobrada.setPaid(true);
        cobrada.setPaidAmount(new BigDecimal("160"));
        cobrada.setLastPaymentAt(LocalDateTime.of(2026, 8, 6, 10, 0));

        Fee porCobrar = new Fee();
        porCobrar.setId(2L);
        porCobrar.setSale(sale);
        porCobrar.setNumberFee(2);
        porCobrar.setAmount(new BigDecimal("10"));
        porCobrar.setExpirationDate(LocalDate.of(2026, 9, 6));
        porCobrar.setPaid(false);

        Fee sobrante = new Fee();
        sobrante.setId(3L);
        sobrante.setSale(sale);
        sobrante.setNumberFee(3);
        sobrante.setAmount(new BigDecimal("170"));
        sobrante.setExpirationDate(LocalDate.of(2026, 10, 6));
        sobrante.setPaid(false);

        sale.getFees().clear();
        sale.getFees().add(cobrada);
        sale.getFees().add(porCobrar);
        sale.getFees().add(sobrante);
        sale.setPriceTotal(new BigDecimal("170"));
        sale.setRemainingAmount(new BigDecimal("10"));
        sale.setStatus(SaleStatus.ACTIVE);
        when(feeRepository.findById(2L)).thenReturn(java.util.Optional.of(porCobrar));

        feeServices.registerPayment(2L, new BigDecimal("10"), null, "EFECTIVO");

        assertEquals(0, sale.getRemainingAmount().signum());
        assertEquals(SaleStatus.COMPLETED, sale.getStatus());
        assertEquals(Boolean.TRUE, porCobrar.getPaid());
        assertFalse(sale.getFees().contains(sobrante));
        verify(feeRepository).delete(sobrante);
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
            pagada.setPaidAmount(new BigDecimal("100"));
            pagada.setExpirationDate(LocalDate.of(2026, 8, 6));
            pagada.setLastPaymentAt(LocalDateTime.of(2026, 8, 6, 10, 0));
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
            // Total 200: 100 cobrados en la cuota 1 y 100 pendientes en la 2
            sale.setPriceTotal(new BigDecimal("200"));

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

    // El plan de cuotas es fijo: editar una cuota nunca re-deriva el monto
    // programado de las impagas (el tope de lo que se cobra sigue siendo la deuda).
    @Nested
    class PostponeFeeNoRecalculaMontosDelPlan {

        private Fee pagada;
        private Fee pendiente;

        @BeforeEach
        void setUp() {
            // Venta de 200 en cuotas de 100: la cuota 1 cobrada y la 2 por cobrar
            pagada = new Fee();
            pagada.setId(1L);
            pagada.setSale(sale);
            pagada.setNumberFee(1);
            pagada.setAmount(new BigDecimal("100"));
            pagada.setPaidAmount(new BigDecimal("100"));
            pagada.setExpirationDate(LocalDate.of(2026, 8, 6));
            pagada.setLastPaymentAt(LocalDateTime.of(2026, 8, 6, 10, 0));
            pagada.setPaid(true);

            pendiente = new Fee();
            pendiente.setId(2L);
            pendiente.setSale(sale);
            pendiente.setNumberFee(2);
            pendiente.setAmount(new BigDecimal("100"));
            pendiente.setExpirationDate(LocalDate.of(2026, 9, 6));
            pendiente.setPaid(false);

            sale.getFees().clear();
            sale.getFees().add(pagada);
            sale.getFees().add(pendiente);
            sale.setPriceTotal(new BigDecimal("200"));
            sale.setAmountFee(new BigDecimal("100"));
            sale.setRemainingAmount(new BigDecimal("100"));

            when(feeRepository.findById(1L)).thenReturn(java.util.Optional.of(pagada));
            when(feeRepository.findById(2L)).thenReturn(java.util.Optional.of(pendiente));
        }

        @Test
        void alSubirElMontoDeUnaCuotaCobradaLaImpagaConservaSuMontoProgramado() {
            // Se re-declara que en la cuota 1 se cobraron 180 en vez de 100: la deuda
            // baja a 20, pero la cuota impaga sigue agendada por su monto del plan
            feeServices.postponeFee(1L, null, null, new BigDecimal("180"));

            assertEquals(0, new BigDecimal("20").compareTo(sale.getRemainingAmount()));
            assertEquals(0, new BigDecimal("100").compareTo(pendiente.getAmount()));
        }

        @Test
        void alVolverABajarLaCuotaCobradaLaImpagaSigueEnSuMonto() {
            feeServices.postponeFee(1L, null, null, new BigDecimal("180"));
            feeServices.postponeFee(1L, null, null, new BigDecimal("100"));

            // Deuda 100 otra vez y la cuota por cobrar sigue valiendo 100
            assertEquals(0, new BigDecimal("100").compareTo(sale.getRemainingAmount()));
            assertEquals(0, new BigDecimal("100").compareTo(pendiente.getAmount()));
        }

        @Test
        void editarElMontoDeUnaImpagaSeRespetaSiLaDeudaNoCambia() {
            // Editar una cuota impaga no mueve la deuda (todavía no entró plata), así
            // que el monto que se le puso a mano se mantiene
            feeServices.postponeFee(2L, null, null, new BigDecimal("40"));

            assertEquals(0, new BigDecimal("100").compareTo(sale.getRemainingAmount()));
            assertEquals(0, new BigDecimal("40").compareTo(pendiente.getAmount()));
        }

        @Test
        void unaCuotaImpagaPorEncimaDeLaDeudaConservaSuMontoProgramado() {
            // Dato viejo: la cuota 1 quedó cobrada por 150 (deuda 50) y la cuota
            // impaga sigue valiendo 100. El plan no se re-acota al saldo.
            pagada.setAmount(new BigDecimal("150"));
            pagada.setPaidAmount(new BigDecimal("150"));

            feeServices.postponeFee(2L, null, null, new BigDecimal("100"));

            assertEquals(0, new BigDecimal("50").compareTo(sale.getRemainingAmount()));
            assertEquals(0, new BigDecimal("100").compareTo(pendiente.getAmount()));
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

