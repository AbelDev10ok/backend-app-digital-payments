package com.app.digital.payments.digital_pyments.services;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleRequestDto;
import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Product;
import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.ProductVariant;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.IPaymentRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductVariantRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleDocumentRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.FeeStatus;
import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerifE234FeeServicesTest {
    @Mock private IFeeRepository feeRepository;
    @Mock private ISaleRepository saleRepository;
    @Mock private IPaymentRepository paymentRepository;

    private FeeServices feeServices;
    private Sale sale;

    private static final LocalDate D1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate D2 = D1.plusWeeks(1);
    private static final LocalDate D3 = D1.plusWeeks(2);
    private static final LocalDate D4 = D1.plusWeeks(3);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        feeServices = new FeeServices(feeRepository, saleRepository, paymentRepository);
        sale = new Sale();
        sale.setId(1L);
        sale.setDateCreation(LocalDate.of(2026, 8, 1));
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setTypePayments(Payments.SEMANAL);
        sale.setPriceTotal(new BigDecimal("400"));
        sale.setRemainingAmount(new BigDecimal("400"));
        sale.setAmountFee(new BigDecimal("100"));
        sale.setDescriptionProduct("Producto");
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
            cuota.setLastPaymentAt(vencimiento.atTime(10, 0));
        }
        sale.getFees().add(cuota);
        when(feeRepository.findById(id)).thenReturn(Optional.of(cuota));
        return cuota;
    }

    private String snapshot() {
        StringBuilder sb = new StringBuilder();
        for (Fee f : sale.getFees()) {
            sb.append(f.getId()).append('|').append(f.getNumberFee()).append('|')
              .append(f.getExpirationDate()).append('|').append(f.getAmount()).append('|')
              .append(f.getPaid()).append('|').append(f.getPaidAmount()).append(';');
        }
        sb.append("total=").append(sale.getPriceTotal())
          .append(";remaining=").append(sale.getRemainingAmount())
          .append(";audit=").append(sale.getReprogrammedAt()).append('/').append(sale.getReprogramCount())
          .append(";final=").append(sale.getFinalPaymentDate());
        return sb.toString();
    }

    private List<Long> ids(List<FeeDto> cuotas) {
        return cuotas.stream().map(FeeDto::getId).toList();
    }

    // ----- S10: validaciones del delta -----

    @Test
    void deltasInvalidosRechazanSinTocarNadaNiCargarLaCuota() {
        cuota(1L, 1, D1, false);
        cuota(2L, 2, D2, false);
        String antes = snapshot();

        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> feeServices.reprogramFee(1L, 2, 3));
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> feeServices.reprogramFee(1L, null, null));
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> feeServices.reprogramFee(1L, 0, null));
        IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class, () -> feeServices.reprogramFee(1L, null, 0));
        IllegalArgumentException e5 = assertThrows(IllegalArgumentException.class, () -> feeServices.reprogramFee(1L, -1, null));
        IllegalArgumentException e6 = assertThrows(IllegalArgumentException.class, () -> feeServices.reprogramFee(1L, null, -5));

        for (IllegalArgumentException e : List.of(e1, e2, e3, e4, e5, e6)) {
            assertEquals("Indic\u00e1 periods o days (exactamente uno, mayor a 0)", e.getMessage());
        }
        assertEquals(antes, snapshot());
        verify(feeRepository, never()).findById(any());
        verify(feeRepository, never()).save(any(Fee.class));
        verify(feeRepository, never()).delete(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void reprogramVentaAnuladaCompletadaOCobradaRechazaSinMutar() {
        Fee c1 = cuota(1L, 1, D1, false);
        cuota(2L, 2, D2, false);
        Fee c3 = cuota(3L, 3, D3, false);
        sale.setFinalPaymentDate(D3);

        sale.setStatus(SaleStatus.CANCELED);
        String antes = snapshot();
        IllegalStateException e1 = assertThrows(IllegalStateException.class, () -> feeServices.reprogramFee(1L, 2, null));
        assertEquals("No se puede reprogramar una cuota de una venta anulada", e1.getMessage());
        assertEquals(antes, snapshot());

        sale.setStatus(SaleStatus.COMPLETED);
        IllegalStateException e2 = assertThrows(IllegalStateException.class, () -> feeServices.reprogramFee(1L, 2, null));
        assertEquals("No se puede reprogramar una cuota de una venta completada", e2.getMessage());
        assertEquals(antes, snapshot());

        sale.setStatus(SaleStatus.ACTIVE);
        c1.setPaid(true);
        c1.setPaidAmount(new BigDecimal("100"));
        String antes3 = snapshot();
        IllegalStateException e3 = assertThrows(IllegalStateException.class, () -> feeServices.reprogramFee(1L, 2, null));
        assertEquals("La cuota ya est\u00e1 pagada", e3.getMessage());
        assertEquals(antes3, snapshot());

        verify(feeRepository, never()).save(any(Fee.class));
        verify(feeRepository, never()).delete(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
        assertNull(sale.getReprogrammedAt());
        assertEquals(Integer.valueOf(0), sale.getReprogramCount());
    }

    // ----- S10: movimiento del tramo -----

    @Test
    void reprogramCorreLaNombradaYLasSiguientesImpagasSalteandoLasCobradas() {
        Fee c1 = cuota(1L, 1, D1, true);
        Fee c2 = cuota(2L, 2, D2, false);
        Fee c3 = cuota(3L, 3, D3, false);
        Fee c4 = cuota(4L, 4, D4, false);
        sale.setRemainingAmount(new BigDecimal("300"));
        sale.setFinalPaymentDate(D4);

        List<FeeDto> movidas = feeServices.reprogramFee(2L, null, 5);

        assertEquals(List.of(2L, 3L, 4L), ids(movidas));
        assertEquals(D1, c1.getExpirationDate());
        assertEquals(D2.plusDays(5), c2.getExpirationDate());
        assertEquals(D3.plusDays(5), c3.getExpirationDate());
        assertEquals(D4.plusDays(5), c4.getExpirationDate());
        for (Fee f : List.of(c1, c2, c3, c4)) {
            assertEquals(0, new BigDecimal("100").compareTo(f.getAmount()));
        }
        assertEquals(4, sale.getFees().size());
        assertEquals(0, new BigDecimal("300").compareTo(sale.getRemainingAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(c1.getPaidAmount()));
        verify(feeRepository, never()).save(c1);
        verify(feeRepository).save(c2);
        verify(feeRepository).save(c3);
        verify(feeRepository).save(c4);
        verify(feeRepository, never()).delete(any(Fee.class));
        verify(saleRepository, times(1)).save(sale);
        assertEquals(Integer.valueOf(1), sale.getReprogramCount());
        assertNotNull(sale.getReprogrammedAt());
        assertEquals(D4.plusDays(5), sale.getFinalPaymentDate());
    }

    @Test
    void reprogramPorPeriodosUsaLaFrecuenciaDeLaVenta() {
        cuota(1L, 1, D1, false);
        Fee c2 = cuota(2L, 2, D2, false);
        Fee c3 = cuota(3L, 3, D3, false);

        feeServices.reprogramFee(1L, 2, null);

        assertEquals(D2.plusWeeks(2), c2.getExpirationDate());
        assertEquals(D3.plusWeeks(2), c3.getExpirationDate());
    }

    @Test
    void reprogramEnPeriodosMensualesEncadenaMeses() {
        sale.setTypePayments(Payments.MENSUAL);
        cuota(1L, 1, D1, false);
        Fee c2 = cuota(2L, 2, D1.plusMonths(1), false);

        feeServices.reprogramFee(1L, 1, null);

        assertEquals(D1.plusMonths(2), c2.getExpirationDate());
    }

    @Test
    void reprogramSellaUnEventoPorLlamadaYAcumula() {
        cuota(1L, 1, D1, false);
        cuota(2L, 2, D2, false);
        cuota(3L, 3, D3, false);

        feeServices.reprogramFee(1L, null, 3);
        assertEquals(Integer.valueOf(1), sale.getReprogramCount());

        feeServices.reprogramFee(1L, null, 2);
        assertEquals(Integer.valueOf(2), sale.getReprogramCount());
        assertNotNull(sale.getReprogrammedAt());
        assertEquals(D1.plusDays(5), sale.getFees().get(0).getExpirationDate());
        verify(saleRepository, times(2)).save(sale);
    }

    // ----- S11 as-built: fecha final por numberFee -----

    @Test
    void reprogramFechaFinalEsElVencimientoDeLaCuotaDeMayorNumberFee() {
        cuota(1L, 1, D1, false);
        cuota(2L, 2, D2, false);
        Fee c3 = cuota(3L, 3, D3, false);
        sale.setFinalPaymentDate(D3);

        feeServices.reprogramFee(2L, null, 30);

        assertEquals(D3.plusDays(30), c3.getExpirationDate());
        assertEquals(D3.plusDays(30), sale.getFinalPaymentDate());
    }

    @Test
    void posponerUnaCuotaIntermediaMasAllaDeLaUltimaNoMueveLaFechaFinalAsBuilt() {
        cuota(1L, 1, D1, false);
        Fee c2 = cuota(2L, 2, D2, false);
        cuota(3L, 3, D3, false);
        sale.setFinalPaymentDate(D3);

        feeServices.postponeFee(2L, D3.plusDays(10), null, null);

        assertEquals(D3.plusDays(10), c2.getExpirationDate());
        assertEquals(D3, sale.getFinalPaymentDate());
        assertEquals(Integer.valueOf(1), sale.getReprogramCount());
        assertNotNull(sale.getReprogrammedAt());
        verify(feeRepository).save(c2);
        verify(saleRepository, times(1)).save(sale);
    }

    @Test
    void posponerLaCuotaDeMayorNumberFeeMueveLaFechaFinal() {
        cuota(1L, 1, D1, false);
        cuota(2L, 2, D2, false);
        Fee c3 = cuota(3L, 3, D3, false);
        sale.setFinalPaymentDate(D3);

        feeServices.postponeFee(3L, D3.plusDays(10), null, null);

        assertEquals(D3.plusDays(10), c3.getExpirationDate());
        assertEquals(D3.plusDays(10), sale.getFinalPaymentDate());
        assertEquals(Integer.valueOf(1), sale.getReprogramCount());
        assertNotNull(sale.getReprogrammedAt());
    }

    // ----- S13d: rechazos de posponer sin auditar -----

    @Test
    void fechaDuplicadaRechazaSinAuditarNiGuardar() {
        Fee c1 = cuota(1L, 1, D1, false);
        cuota(2L, 2, D2, false);
        cuota(3L, 3, D3, false);
        String antes = snapshot();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> feeServices.postponeFee(1L, D3, null, null));

        assertEquals("Ya existe una cuota con esa fecha de expiraci\u00f3n en esta venta", e.getMessage());
        assertEquals(D1, c1.getExpirationDate());
        assertEquals(antes, snapshot());
        assertNull(sale.getReprogrammedAt());
        assertEquals(Integer.valueOf(0), sale.getReprogramCount());
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void fechaAnteriorALaCreacionRechazaSinAuditar() {
        Fee c1 = cuota(1L, 1, D1, false);
        String antes = snapshot();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> feeServices.postponeFee(1L, LocalDate.of(2026, 7, 1), null, null));

        assertEquals("La fecha no puede ser anterior a la creaci\u00f3n", e.getMessage());
        assertEquals(antes, snapshot());
        assertNull(sale.getReprogrammedAt());
        assertEquals(Integer.valueOf(0), sale.getReprogramCount());
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    // ----- S13c: rebalanceo de montos -----

    @Test
    void editarImpagaNoUltimaRebalanceaLaUltimaImpagaAlResiduo() {
        cuota(1L, 1, D1, true);
        Fee c2 = cuota(2L, 2, D2, false);
        Fee c3 = cuota(3L, 3, D3, false);
        Fee c4 = cuota(4L, 4, D4, false);
        sale.setRemainingAmount(new BigDecimal("300"));

        FeeDto dto = feeServices.postponeFee(2L, null, null, new BigDecimal("50"));

        assertEquals(0, new BigDecimal("50").compareTo(c2.getAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(c3.getAmount()));
        assertEquals(0, new BigDecimal("150").compareTo(c4.getAmount()));
        assertEquals(0, new BigDecimal("50").compareTo(dto.getAmount()));
        BigDecimal suma = sale.getFees().stream().map(Fee::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sale.getPriceTotal().compareTo(suma));
        assertEquals(0, new BigDecimal("300").compareTo(sale.getRemainingAmount()));
        assertEquals(Boolean.FALSE, c2.getPaid());
        verify(feeRepository).save(c4);
        verify(saleRepository, times(1)).save(sale);
    }

    @Test
    void residuoCeroONegativoRechazaSinMutarNiGuardar() {
        cuota(1L, 1, D1, true);
        Fee c2 = cuota(2L, 2, D2, false);
        Fee c3 = cuota(3L, 3, D3, false);
        Fee c4 = cuota(4L, 4, D4, false);
        sale.setRemainingAmount(new BigDecimal("300"));
        String antes = snapshot();

        IllegalArgumentException e0 = assertThrows(IllegalArgumentException.class,
                () -> feeServices.postponeFee(2L, null, null, new BigDecimal("200")));
        IllegalArgumentException eNeg = assertThrows(IllegalArgumentException.class,
                () -> feeServices.postponeFee(2L, null, null, new BigDecimal("250")));

        assertEquals("El monto deja cuotas por debajo del total de la operaci\u00f3n", e0.getMessage());
        assertEquals("El monto deja cuotas por debajo del total de la operaci\u00f3n", eNeg.getMessage());
        assertEquals(antes, snapshot());
        assertEquals(0, new BigDecimal("100").compareTo(c2.getAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(c3.getAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(c4.getAmount()));
        assertEquals(0, new BigDecimal("300").compareTo(sale.getRemainingAmount()));
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void editarLaUltimaImpagaSeAceptaTalCualSinTocarElResto() {
        cuota(1L, 1, D1, true);
        Fee c2 = cuota(2L, 2, D2, false);
        Fee c3 = cuota(3L, 3, D3, false);
        Fee c4 = cuota(4L, 4, D4, false);
        sale.setRemainingAmount(new BigDecimal("300"));

        feeServices.postponeFee(4L, null, null, new BigDecimal("40"));

        assertEquals(0, new BigDecimal("40").compareTo(c4.getAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(c2.getAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(c3.getAmount()));
        BigDecimal suma = sale.getFees().stream().map(Fee::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("340").compareTo(suma));
        assertEquals(0, new BigDecimal("300").compareTo(sale.getRemainingAmount()));
        assertEquals(Boolean.FALSE, c4.getPaid());
        verify(feeRepository, never()).save(any(Fee.class));
        verify(saleRepository, times(1)).save(sale);
    }
}

class VerifE234QueryTest {
    @Mock private IFeeRepository feeRepository;

    private FeeQueryServices service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new FeeQueryServices(feeRepository);

        Sale sale = new Sale();
        sale.setId(2L);
        sale.setDescriptionProduct("Producto");
        sale.setKind(SaleKind.VENTA);
        sale.setQuiantityFees(3);
        sale.setAmountFee(new BigDecimal("100"));
        sale.setRemainingAmount(new BigDecimal("300"));

        Fee vencida = new Fee(sale, 1, new BigDecimal("100"), LocalDate.now().minusDays(45));
        vencida.setId(10L);
        vencida.setPaid(false);

        when(feeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(vencida)));
    }

    @SuppressWarnings("rawtypes")
    private Specification<Fee> capturar(FeeStatus estado, LocalDate desde, LocalDate hasta, Long clientId) {
        service.getCuotas(estado, desde, hasta, clientId, PageRequest.of(0, 20));
        ArgumentCaptor<Specification<Fee>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(feeRepository).findAll(captor.capture(), any(Pageable.class));
        return captor.getValue();
    }

    private CriteriaBuilder evaluar(Specification<Fee> spec, boolean conCliente, boolean conRango) {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root root = mock(Root.class);
        Path impaga = mock(Path.class);
        Path vencimiento = mock(Path.class);
        Path salePath = mock(Path.class);
        Path status = mock(Path.class);
        when(root.get("sale")).thenReturn(salePath);
        when(root.get("paid")).thenReturn(impaga);
        when(root.get("expirationDate")).thenReturn(vencimiento);
        when(salePath.get("status")).thenReturn(status);
        if (conCliente) {
            Path clientPath = mock(Path.class);
            Path clientIdPath = mock(Path.class);
            when(salePath.get("client")).thenReturn(clientPath);
            when(clientPath.get("id")).thenReturn(clientIdPath);
        }
        spec.toPredicate(root, mock(CriteriaQuery.class), cb);
        return cb;
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void aCobrarEsImpagasHastaHoyMas30SinPisoYExcluyeAnuladas() {
        Specification<Fee> spec = capturar(FeeStatus.A_COBRAR, null, null, null);
        CriteriaBuilder cb = evaluar(spec, false, false);

        verify(cb).isFalse(any());
        verify(cb).lessThanOrEqualTo(any(Expression.class), any(Comparable.class));
        verify(cb).notEqual(any(Expression.class), (Object) org.mockito.ArgumentMatchers.eq(SaleStatus.CANCELED));
        verify(cb, never()).greaterThan(any(Expression.class), any(Comparable.class));
        verify(cb, never()).greaterThanOrEqualTo(any(Expression.class), any(Comparable.class));

        org.mockito.ArgumentCaptor<Comparable> corte = org.mockito.ArgumentCaptor.forClass(Comparable.class);
        verify(cb).lessThanOrEqualTo(any(Expression.class), corte.capture());
        assertEquals(LocalDate.now().plusDays(30), corte.getValue());
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void aCobrarConservaLosFiltrosDeRangoYCliente() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 2, 1);
        Specification<Fee> spec = capturar(FeeStatus.A_COBRAR, desde, hasta, 7L);
        CriteriaBuilder cb = evaluar(spec, true, true);

        verify(cb).greaterThanOrEqualTo(any(Expression.class), org.mockito.ArgumentMatchers.eq(desde));
        verify(cb).lessThanOrEqualTo(any(Expression.class), org.mockito.ArgumentMatchers.eq(hasta));
        verify(cb).equal(any(Expression.class), org.mockito.ArgumentMatchers.eq(7L));
    }

    @Test
    void postponedSigueRechazadoSinConsultarLaBase() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.getCuotas(FeeStatus.POSTPONED, null, null, null, PageRequest.of(0, 20)));

        assertEquals("El filtro de cuotas pospuestas no esta disponible", e.getMessage());
        verify(feeRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void pendingSigueListandoTodasLasImpagasSinCorte() {
        Specification<Fee> spec = capturar(FeeStatus.PENDING, null, null, null);
        CriteriaBuilder cb = evaluar(spec, false, false);

        verify(cb).isFalse(any());
        verify(cb, never()).lessThanOrEqualTo(any(Expression.class), any(Comparable.class));
        verify(cb, never()).greaterThan(any(Expression.class), any(Comparable.class));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void todaySigueComparandoIgualdadPorFecha() {
        Specification<Fee> spec = capturar(FeeStatus.TODAY, null, null, null);
        CriteriaBuilder cb = evaluar(spec, false, false);

        verify(cb).equal(any(Expression.class), org.mockito.ArgumentMatchers.eq(LocalDate.now()));
        verify(cb, never()).lessThanOrEqualTo(any(Expression.class), any(Comparable.class));
    }
}

class VerifE234AltaTest {
    @Mock private ISaleRepository salesRepository;
    @Mock private IClientRepository clientRepository;
    @Mock private IFeeRepository feeRepository;
    @Mock private IProductTypeRepository productTypeRepository;
    @Mock private IProductVariantRepository variantRepository;
    @Mock private IBusinessRepository businessRepository;
    @Mock private ISaleDocumentRepository saleDocumentRepository;

    private VentaServices ventaServices;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ventaServices = new VentaServices(salesRepository, clientRepository, feeRepository, productTypeRepository,
                variantRepository, businessRepository, saleDocumentRepository);
        TenantContext.setBusinessId(1L);
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private SaleRequestDto dtoVenta(String kindName, String descripcion) {
        ProductType productType = new ProductType();
        productType.setId(1L);
        productType.setName("TV");
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));

        Client client = new Client();
        client.setId(1L);
        client.setName("Juan");
        client.setTelefono("1144556677");
        client.setEmail("juan@example.com");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        SaleRequestDto dto = new SaleRequestDto();
        dto.setClientId(1L);
        dto.setKind(kindName);
        dto.setProductType(1L);
        dto.setAmountFee(new BigDecimal("100"));
        dto.setQuantityFees(2);
        dto.setDescriptionProduct(descripcion);
        dto.setCost(new BigDecimal("50"));
        dto.setDateSale(LocalDate.of(2026, 9, 1));
        dto.setFirstFeeDate(LocalDate.of(2026, 9, 1));
        dto.setPayments("MENSUAL");
        dto.setPayFirstFee(false);
        return dto;
    }

    private void prepararProductoConPrecio(Long id, BigDecimal costo, BigDecimal precio, int stock) {
        Business business = new Business();
        business.setId(1L);

        Product product = new Product();
        product.setId(id);
        product.setName("Producto");
        product.setActive(true);
        product.setBusiness(business);
        ProductType pt = new ProductType();
        pt.setId(1L);
        pt.setName("TV");
        pt.setBusiness(business);
        product.setProductType(pt);

        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setProduct(product);
        variant.setBusiness(business);
        variant.setSku("SKU-" + id);
        variant.setActive(true);
        variant.setCost(costo);
        variant.setPrice(precio);
        variant.setStock(stock);

        when(variantRepository.findById(id)).thenReturn(Optional.of(variant));
    }

    private Sale capturarSale() {
        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void altaMensualFechaFinalEsElVencimientoDeLaUltimaCuota() throws Exception {
        prepararProductoConPrecio(1L, new BigDecimal("50"), new BigDecimal("150"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayments("MENSUAL");
        dto.setQuantityFees(3);
        dto.setDateSale(LocalDate.of(2026, 9, 30));
        dto.setFirstFeeDate(LocalDate.of(2026, 9, 30));

        ventaServices.createVenta(dto);
        Sale saved = capturarSale();

        assertEquals(3, saved.getFees().size());
        assertEquals(LocalDate.of(2026, 11, 30), saved.getFees().get(2).getExpirationDate());
        assertEquals(LocalDate.of(2026, 11, 30), saved.getFinalPaymentDate());
    }

    @Test
    void altaSemanalFechaFinalEsElVencimientoDeLaUltimaCuota() throws Exception {
        prepararProductoConPrecio(1L, new BigDecimal("50"), new BigDecimal("150"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayments("SEMANAL");
        dto.setQuantityFees(3);

        ventaServices.createVenta(dto);
        Sale saved = capturarSale();

        assertEquals(LocalDate.of(2026, 9, 15), saved.getFees().get(2).getExpirationDate());
        assertEquals(LocalDate.of(2026, 9, 15), saved.getFinalPaymentDate());
    }

    @Test
    void altaContadoFechaFinalEsElVencimientoDeLaUnicaCuota() throws Exception {
        prepararProductoConPrecio(1L, new BigDecimal("80"), new BigDecimal("100"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayments("CONTADO");
        dto.setQuantity(2);
        dto.setFirstFeeDate(LocalDate.of(2026, 9, 15));

        ventaServices.createVenta(dto);
        Sale saved = capturarSale();

        assertEquals(1, saved.getFees().size());
        assertEquals(LocalDate.of(2026, 9, 15), saved.getFinalPaymentDate());
    }

    @Test
    void altaPrestamoConInteresFechaFinalEsElVencimientoDeLaUltimaCuota() throws Exception {
        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("1200"));
        dto.setInterestRate(new BigDecimal("25"));
        dto.setQuantityFees(4);
        dto.setPayments("MENSUAL");

        ventaServices.createVenta(dto);
        Sale saved = capturarSale();

        assertEquals(4, saved.getFees().size());
        assertEquals(saved.getFees().get(3).getExpirationDate(), saved.getFinalPaymentDate());
        assertEquals(LocalDate.of(2026, 12, 1), saved.getFinalPaymentDate());
    }

    @Test
    void payFirstFeeNoCambiaLaFechaFinalDelPlan() throws Exception {
        prepararProductoConPrecio(1L, new BigDecimal("50"), new BigDecimal("150"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayments("SEMANAL");
        dto.setQuantityFees(3);
        dto.setPayFirstFee(true);
        dto.setFirstFeeAmount(new BigDecimal("250"));

        ventaServices.createVenta(dto);
        Sale saved = capturarSale();

        assertEquals(Boolean.TRUE, saved.getFees().get(1).getPaid());
        assertEquals(LocalDate.of(2026, 9, 15), saved.getFinalPaymentDate());
    }
}
