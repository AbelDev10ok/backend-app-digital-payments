package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;

import com.app.digital.payments.digital_pyments.utils.TenantContext;
import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Product;
import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.ProductVariant;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.SaleRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductVariantRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleDocumentRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;

/**
 * Verificacion independiente de E1 (cronograma fijo): fija la conducta FIFO, las cuotas del plan y los rechazos sin mutacion.
 */
class VentaCronogramaFijoTest {

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

    private ProductVariant prepararProductoConPrecio(Long id, BigDecimal costo, BigDecimal precio, int stock) {
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
        return variant;
    }

    private Sale capturarSale() {
        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        return captor.getValue();
    }

    private BigDecimal sumaProgramada(Sale saved) {
        return saved.getFees().stream().map(Fee::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Invariante 3 + S1: venta semanal de 3 cuotas materializa el plan completo
    @Test
    void ventaSemanalDe3CuotasSumaElTotalYEncadenaVencimientos() throws Exception {
        prepararProductoConPrecio(1L, new BigDecimal("150"), new BigDecimal("150"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayments("SEMANAL");
        dto.setQuantityFees(3);

        SaleResponseDto result = ventaServices.createVenta(dto);
        Sale saved = capturarSale();

        assertEquals(new BigDecimal("300"), result.getPriceTotal());
        assertEquals(3, saved.getFees().size());
        assertEquals(0, sumaProgramada(saved).compareTo(saved.getPriceTotal()));
        for (int i = 0; i < 3; i++) {
            Fee f = saved.getFees().get(i);
            assertEquals(Integer.valueOf(i + 1), f.getNumberFee());
            assertEquals(LocalDate.of(2026, 9, 1).plusWeeks(i), f.getExpirationDate());
            assertEquals(0, new BigDecimal("100").compareTo(f.getAmount()));
            assertEquals(Boolean.FALSE, f.getPaid());
            assertEquals(0, f.getPaidAmount().signum());
        }
        assertEquals(0, new BigDecimal("300").compareTo(saved.getRemainingAmount()));
        assertEquals(SaleStatus.ACTIVE, saved.getStatus());
    }

    // S6: contado con cantidad 2 tiene UNA cuota que cubre el total
    @Test
    void contadoConCantidad2TieneUnaCuotaPorElTotal() throws Exception {
        prepararProductoConPrecio(1L, new BigDecimal("80"), new BigDecimal("100"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayments("CONTADO");
        dto.setQuantity(2);

        SaleResponseDto result = ventaServices.createVenta(dto);
        Sale saved = capturarSale();

        assertEquals(new BigDecimal("200"), result.getPriceTotal());
        assertEquals(Integer.valueOf(1), saved.getQuiantityFees());
        assertEquals(1, saved.getFees().size());
        assertEquals(0, new BigDecimal("200").compareTo(saved.getFees().get(0).getAmount()));
        assertEquals(0, sumaProgramada(saved).compareTo(saved.getPriceTotal()));
        assertEquals(LocalDate.of(2026, 9, 1), saved.getFees().get(0).getExpirationDate());
        assertEquals(0, new BigDecimal("200").compareTo(saved.getRemainingAmount()));
    }

    // Invariante 3: prestamo con interes
    @Test
    void prestamoConInteresSumaElTotalYDivideEnCuotas() throws Exception {
        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("1200"));
        dto.setInterestRate(new BigDecimal("25"));
        dto.setAmountFee(new BigDecimal("999"));
        dto.setQuantityFees(4);
        dto.setPayments("MENSUAL");

        SaleResponseDto result = ventaServices.createVenta(dto);
        Sale saved = capturarSale();

        assertEquals(0, new BigDecimal("1500").compareTo(result.getPriceTotal()));
        assertEquals(4, saved.getFees().size());
        assertEquals(0, sumaProgramada(saved).compareTo(saved.getPriceTotal()));
        for (int i = 0; i < 4; i++) {
            assertEquals(0, new BigDecimal("375").compareTo(saved.getFees().get(i).getAmount()));
            assertEquals(LocalDate.of(2026, 9, 1).plusMonths(i), saved.getFees().get(i).getExpirationDate());
        }
    }

    @Test
    void prestamoConInteresConRedondeoLaSumaDelPlanDaElTotal() throws Exception {
        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("1000"));
        dto.setInterestRate(new BigDecimal("10"));
        dto.setQuantityFees(3);
        dto.setPayments("MENSUAL");

        SaleResponseDto result = ventaServices.createVenta(dto);
        Sale saved = capturarSale();

        assertEquals(new BigDecimal("1100.01"), result.getPriceTotal());
        assertEquals(0, sumaProgramada(saved).compareTo(saved.getPriceTotal()));
        assertEquals(0, new BigDecimal("366.67").compareTo(saved.getFees().get(0).getAmount()));
    }

    // Invariante 3: prestamo sin interes
    @Test
    void prestamoSinInteresSumaElTotal() throws Exception {
        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("300"));
        dto.setAmountFee(new BigDecimal("100"));
        dto.setQuantityFees(3);
        dto.setPayments("MENSUAL");

        ventaServices.createVenta(dto);
        Sale saved = capturarSale();

        assertEquals(0, new BigDecimal("300").compareTo(saved.getPriceTotal()));
        assertEquals(0, sumaProgramada(saved).compareTo(saved.getPriceTotal()));
    }

    // S4: pago inicial en cascada
    @Test
    void payFirstFeeCascadaSobreElPlanCompleto() throws Exception {
        prepararProductoConPrecio(1L, new BigDecimal("150"), new BigDecimal("150"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayFirstFee(true);
        dto.setQuantityFees(3);
        dto.setFirstFeeAmount(new BigDecimal("250"));

        ventaServices.createVenta(dto);
        Sale saved = capturarSale();

        assertEquals(3, saved.getFees().size());
        assertEquals(0, sumaProgramada(saved).compareTo(saved.getPriceTotal()));
        assertEquals(Boolean.TRUE, saved.getFees().get(0).getPaid());
        assertEquals(0, new BigDecimal("100").compareTo(saved.getFees().get(0).getAmount()));
        assertEquals(Boolean.TRUE, saved.getFees().get(1).getPaid());
        assertEquals(0, new BigDecimal("100").compareTo(saved.getFees().get(1).getAmount()));
        assertEquals(Boolean.FALSE, saved.getFees().get(2).getPaid());
        assertEquals(0, new BigDecimal("100").compareTo(saved.getFees().get(2).getAmount()));
        assertEquals(0, new BigDecimal("50").compareTo(saved.getRemainingAmount()));
        assertEquals(SaleStatus.ACTIVE, saved.getStatus());
    }

    // S2: el pago inicial que cubre todo deja la operacion COMPLETED con fecha real
    @Test
    void payFirstFeeCubriendoTodoElPlanCierraLaOperacion() throws Exception {
        prepararProductoConPrecio(1L, new BigDecimal("150"), new BigDecimal("150"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayFirstFee(true);
        dto.setQuantityFees(3);
        dto.setFirstFeeAmount(new BigDecimal("300"));

        ventaServices.createVenta(dto);
        Sale saved = capturarSale();

        assertEquals(0, saved.getRemainingAmount().signum());
        assertEquals(SaleStatus.COMPLETED, saved.getStatus());
        assertNotNull(saved.getRealFinalDate());
        assertTrue(saved.getFees().stream().allMatch(f -> Boolean.TRUE.equals(f.getPaid())));
    }

    // S4: validaciones existentes del pago inicial; rechazo sin persistir
    @Test
    void payFirstFeeMayorAlSaldoSeRechazaSinGuardar() {
        prepararProductoConPrecio(1L, new BigDecimal("150"), new BigDecimal("150"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayFirstFee(true);
        dto.setQuantityFees(3);
        dto.setFirstFeeAmount(new BigDecimal("301"));

        assertThrows(IllegalArgumentException.class, () -> ventaServices.createVenta(dto));

        verify(salesRepository, never()).save(any(Sale.class));
        verify(feeRepository, never()).save(any(Fee.class));
        verify(variantRepository, never()).save(any(com.app.digital.payments.digital_pyments.models.ProductVariant.class));
    }

    @Test
    void payFirstFeeCeroSeRechazaSinGuardar() {
        prepararProductoConPrecio(1L, new BigDecimal("150"), new BigDecimal("150"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayFirstFee(true);
        dto.setQuantityFees(3);
        dto.setFirstFeeAmount(BigDecimal.ZERO);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));

        assertEquals("El monto de la primera cuota debe ser mayor a 0", e.getMessage());
        verify(salesRepository, never()).save(any(Sale.class));
    }

    // Invariante 2: markPrestamoAsCompleted conserva el plan (S3/S5 deliberados)
    @Test
    void markPrestamoAsCompletedAplicaCascadaSinReescribirMontos() {
        Sale sale = new Sale();
        sale.setId(1L);
        sale.setPriceTotal(new BigDecimal("300"));
        sale.setRemainingAmount(new BigDecimal("300"));
        sale.setStatus(SaleStatus.ACTIVE);
        Business business = new Business();
        business.setId(1L);
        sale.setBusiness(business);
        List<Fee> fees = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Fee f = new Fee();
            f.setId((long) i);
            f.setSale(sale);
            f.setNumberFee(i);
            f.setAmount(new BigDecimal("100"));
            f.setExpirationDate(LocalDate.of(2026, 8, 1).plusMonths(i - 1L));
            f.setPaid(false);
            fees.add(f);
        }
        sale.setFees(fees);
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        ventaServices.markPrestamoAsCompleted(1L, new BigDecimal("250"));

        assertEquals(0, new BigDecimal("50").compareTo(sale.getRemainingAmount()));
        assertEquals(SaleStatus.ACTIVE, sale.getStatus());
        assertEquals(0, new BigDecimal("100").compareTo(fees.get(0).getAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(fees.get(0).getPaidAmount()));
        assertEquals(Boolean.TRUE, fees.get(0).getPaid());
        assertEquals(0, new BigDecimal("100").compareTo(fees.get(1).getPaidAmount()));
        assertEquals(Boolean.TRUE, fees.get(1).getPaid());
        assertEquals(0, new BigDecimal("50").compareTo(fees.get(2).getPaidAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(fees.get(2).getAmount()));
        assertEquals(Boolean.FALSE, fees.get(2).getPaid());
        assertEquals(0, new BigDecimal("300").compareTo(sumaProgramada(sale)));
    }

    @Test
    void markPrestamoAsCompletedSinMontoUsaLaCuotaPendiente() {
        Sale sale = new Sale();
        sale.setId(1L);
        sale.setPriceTotal(new BigDecimal("300"));
        sale.setRemainingAmount(new BigDecimal("300"));
        sale.setStatus(SaleStatus.ACTIVE);
        Business business = new Business();
        business.setId(1L);
        sale.setBusiness(business);
        List<Fee> fees = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Fee f = new Fee();
            f.setId((long) i);
            f.setSale(sale);
            f.setNumberFee(i);
            f.setAmount(new BigDecimal("100"));
            f.setExpirationDate(LocalDate.of(2026, 8, 1).plusMonths(i - 1L));
            f.setPaid(false);
            fees.add(f);
        }
        sale.setFees(fees);
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        ventaServices.markPrestamoAsCompleted(1L, null);

        assertEquals(0, new BigDecimal("100").compareTo(fees.get(0).getPaidAmount()));
        assertEquals(0, new BigDecimal("200").compareTo(sale.getRemainingAmount()));
        assertEquals(SaleStatus.ACTIVE, sale.getStatus());
    }

    // Comportamiento preexistente que sigue igual
    @Test
    void ventaEnCuotasSigueExigiendoAlMenosDos() {
        prepararProductoConPrecio(1L, new BigDecimal("150"), new BigDecimal("150"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setQuantityFees(1);

        assertThrows(IllegalArgumentException.class, () -> ventaServices.createVenta(dto));
        verify(salesRepository, never()).save(any(Sale.class));
    }
}
