package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Product;
import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.ProductVariant;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.SaleDocument;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.models.dtos.SaleRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleUpdateRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductVariantRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleDocumentRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.DocumentType;
import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

class VentaServicesTest {

    @Mock
    private ISaleRepository salesRepository;

    @Mock
    private IClientRepository clientRepository;

    @Mock
    private IFeeRepository feeRepository;

    @Mock
    private IProductTypeRepository productTypeRepository;

    @Mock
    private IProductVariantRepository variantRepository;

    @Mock
    private IBusinessRepository businessRepository;

    @Mock
    private ISaleDocumentRepository saleDocumentRepository;

    private VentaServices ventaServices;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ventaServices = new VentaServices(salesRepository, clientRepository, feeRepository, productTypeRepository,
                variantRepository, businessRepository, saleDocumentRepository);
        TenantContext.setBusinessId(1L);
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
        dto.setDateSale(LocalDate.now());
        dto.setFirstFeeDate(LocalDate.now());
        dto.setPayments("MENSUAL");
        dto.setPayFirstFee(false);
        return dto;
    }

    /**
     * Prepara una variante vendible. El precio de venta unitario es igual al
     * costo recibido: con eso alcanza para que las ventas en cuotas de los tests
     * cumplan el piso del precio de lista y la regla costo &lt; total.
     * Para precios de venta con margen está {@link #prepararProductoConPrecio}.
     */
    private ProductVariant prepararProducto(Long id, String nombre, BigDecimal costo) {
        return prepararProductoConPrecio(id, nombre, costo, costo, true, 10, null);
    }

    private ProductVariant prepararProducto(Long id, String nombre, BigDecimal costo, boolean conCategoria) {
        return prepararProductoConPrecio(id, nombre, costo, costo, conCategoria, 10, null);
    }

    private ProductVariant prepararProductoConMoneda(Long id, String nombre, BigDecimal costo, String moneda) {
        return prepararProductoConPrecio(id, nombre, costo, costo, true, 10, moneda);
    }

    private ProductVariant prepararProductoConStock(Long id, String nombre, BigDecimal costo, Integer stock) {
        return prepararProductoConPrecio(id, nombre, costo, costo, true, stock, null);
    }

    /** Variante con precio de venta unitario explícito (distinto del costo). */
    private ProductVariant prepararProductoConPrecio(Long id, String nombre, BigDecimal costo, BigDecimal precio,
            Integer stock) {
        return prepararProductoConPrecio(id, nombre, costo, precio, true, stock, null);
    }

    private ProductVariant prepararProductoConPrecio(Long id, String nombre, BigDecimal costo, BigDecimal precio,
            boolean conCategoria, Integer stock, String moneda) {
        Business business = new Business();
        business.setId(1L);

        Product product = new Product();
        product.setId(id);
        product.setName(nombre);
        product.setActive(true);
        product.setBusiness(business);
        if (conCategoria) {
            ProductType pt = new ProductType();
            pt.setId(1L);
            pt.setName("TV");
            pt.setBusiness(business);
            product.setProductType(pt);
        }
        if (moneda != null) {
            product.setCurrency(moneda);
        }

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

    @Test
    void createVentaCalculaTotalesYEstadoActivo() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProducto(1L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(1L);

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(new BigDecimal("200"), result.getPriceTotal());
        assertEquals(SaleStatus.ACTIVE.name(), result.getStatus());

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(0, saved.getRemainingAmount().compareTo(new BigDecimal("200")));
        assertEquals(1, saved.getFees().size());
        assertEquals("Televisor", saved.getDescriptionProduct());
        // Snapshot del precio unitario del catálogo (referencia en cuotas)
        assertEquals(0, new BigDecimal("150").compareTo(saved.getUnitPrice()));
    }

    @Test
    void createVentaDeProductoSinMonedaUsaLaDelNegocio() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        when(businessRepository.getReferenceById(1L)).thenReturn(new Business("Mi negocio", "usd"));

        prepararProducto(1L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(1L);

        SaleResponseDto result = ventaServices.createVenta(dto);

        // El producto es legado (sin moneda propia): la venta cae en la del negocio
        assertEquals("USD", result.getCurrency());
        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        assertEquals("USD", captor.getValue().getCurrency());
    }

    @Test
    void createVentaHeredaLaMonedaDelProductoYNoLaDelNegocio() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        // El negocio está configurado en pesos, pero el producto se cargó en dólares
        when(businessRepository.getReferenceById(1L)).thenReturn(new Business("Mi negocio", "ARS"));

        prepararProductoConMoneda(1L, "Televisor", new BigDecimal("150"), "USD");
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(1L);

        SaleResponseDto result = ventaServices.createVenta(dto);

        // El importe del producto está en USD: la venta se guarda en USD, nunca en
        // la moneda que el negocio tenga configurada hoy (eso cambiaría la unidad
        // del importe sin convertirlo)
        assertEquals("USD", result.getCurrency());
        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        assertEquals("USD", captor.getValue().getCurrency());
    }

    @Test
    void createVentaRechazaUnaMonedaQueContradiceAlProducto() {
        when(businessRepository.getReferenceById(1L)).thenReturn(new Business("Mi negocio", "ARS"));
        prepararProductoConMoneda(1L, "Televisor", new BigDecimal("150"), "USD");

        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(1L);
        dto.setCurrency("ARS"); // contradice la moneda del producto

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));
        assertTrue(error.getMessage().contains("USD"));
        verify(salesRepository, never()).save(any(Sale.class));
    }

    @Test
    void createPrestamoUsaLaMonedaElegida() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        when(businessRepository.getReferenceById(1L)).thenReturn(new Business("Mi negocio", "ARS"));

        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("200"));
        dto.setCurrency("USD"); // el préstamo no tiene producto: la moneda la elige quien lo carga

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals("USD", result.getCurrency());
    }

    @Test
    void createPrestamoRechazaUnaMonedaNoSoportada() {
        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("200"));
        dto.setCurrency("PYG");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));
        assertTrue(error.getMessage().contains("ARS"));
        verify(salesRepository, never()).save(any(Sale.class));
    }

    @Test
    void createVentaDeTipoPrestamoFijaDescripcion() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("200"));

        ventaServices.createVenta(dto);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        assertEquals("Préstamo personal", captor.getValue().getDescriptionProduct());
    }

    @Test
    void createPrestamoConInteresCalculaTotalYCouta() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("1000"));
        dto.setQuantityFees(4);
        dto.setInterestRate(new BigDecimal("10"));

        SaleResponseDto result = ventaServices.createVenta(dto);

        // total = 1000 * 1.10 = 1100, cuota = 1100 / 4 = 275
        assertEquals(0, new BigDecimal("1100").compareTo(result.getPriceTotal()));
        assertEquals(0, new BigDecimal("275").compareTo(result.getAmountFee()));

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(0, new BigDecimal("1100").compareTo(saved.getRemainingAmount()));
        assertEquals(0, new BigDecimal("10").compareTo(saved.getInterestRate()));
    }

    @Test
    void createPrestamoConInteresNoExigeCostIgualATotal() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("1000"));
        dto.setQuantityFees(4);
        dto.setAmountFee(new BigDecimal("999"));
        dto.setInterestRate(new BigDecimal("10"));

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(0, new BigDecimal("1100").compareTo(result.getPriceTotal()));
    }

    @Test
    void createPrestamoConInteresNegativoEsRechazado() {
        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("1000"));
        dto.setInterestRate(new BigDecimal("-5"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));
    }

    @Test
    void createVentaRechazaCantidadDeCuotasInvalida() {
        prepararProducto(1L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(1L);
        dto.setQuantityFees(0);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));
    }

    @Test
    void createVentaRechazaVentaSinProducto() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dtoVenta("VENTA", "Televisor")));
    }

    @Test
    void createVentaUsaCostoDelProductoIgnorandoElEnviado() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProducto(1L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(1L);
        dto.setCost(new BigDecimal("1"));

        ventaServices.createVenta(dto);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("150").compareTo(captor.getValue().getCost()));
        assertEquals("Televisor", captor.getValue().getDescriptionProduct());
    }

    @Test
    void createVentaRechazaProductoSinPrecioValido() {
        prepararProducto(1L, "Televisor", null);
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(1L);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));
    }

    @Test
    void createVentaUsaLaCategoriaDelProducto() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProducto(1L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(1L);
        dto.setProductType(99L);

        ventaServices.createVenta(dto);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getProductType().getId());
    }

    @Test
    void createVentaRechazaProductoSinCategoria() {
        prepararProducto(1L, "Televisor", new BigDecimal("150"), false);
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(1L);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));
    }

    @Test
    void updateSaleUsaLaCategoriaDelProducto() throws Exception {
        Business business = new Business();
        business.setId(1L);

        ProductType pt = new ProductType();
        pt.setId(1L);
        pt.setName("TV");
        pt.setBusiness(business);

        Product product = new Product();
        product.setId(5L);
        product.setName("TV");
        product.setBusiness(business);
        product.setProductType(pt);

        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setProduct(product);
        variant.setBusiness(business);
        variant.setCost(new BigDecimal("100"));
        variant.setPrice(new BigDecimal("200"));
        variant.setStock(5);
        variant.setActive(true);
        when(variantRepository.findById(5L)).thenReturn(Optional.of(variant));

        ProductType ptRequest = new ProductType();
        ptRequest.setId(99L);
        ptRequest.setName("Mueble");
        ptRequest.setBusiness(business);
        when(productTypeRepository.findById(99L)).thenReturn(Optional.of(ptRequest));

        Sale existingSale = new Sale();
        existingSale.setId(1L);
        existingSale.setBusiness(business);
        when(salesRepository.findById(1L)).thenReturn(Optional.of(existingSale));

        SaleUpdateRequestDto update = new SaleUpdateRequestDto();
        update.setKind("VENTA");
        update.setVariant(5L);
        update.setProductType(99L);
        update.setDescriptionProduct("Televisor actualizado");

        ventaServices.updateSale(update, 1L);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getProductType().getId());
        assertEquals(5L, captor.getValue().getVariant().getId());
    }

    @Test
    void updateSaleRechazaUnProductoDeOtraMoneda() {
        Business business = new Business();
        business.setId(1L);

        Product product = new Product();
        product.setId(5L);
        product.setName("TV");
        product.setBusiness(business);
        product.setCurrency("ARS");

        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setProduct(product);
        variant.setBusiness(business);
        variant.setCost(new BigDecimal("100"));
        variant.setPrice(new BigDecimal("200"));
        variant.setStock(5);
        when(variantRepository.findById(5L)).thenReturn(Optional.of(variant));

        Sale existingSale = new Sale();
        existingSale.setId(1L);
        existingSale.setBusiness(business);
        existingSale.setCurrency("USD"); // la operación ya se pactó en dólares
        when(salesRepository.findById(1L)).thenReturn(Optional.of(existingSale));

        SaleUpdateRequestDto update = new SaleUpdateRequestDto();
        update.setKind("VENTA");
        update.setVariant(5L);
        update.setDescriptionProduct("Televisor");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ventaServices.updateSale(update, 1L));
        // El detalle mostraría importes en dólares junto a un producto en pesos
        assertTrue(error.getMessage().contains("ARS"));
        verify(salesRepository, never()).save(any(Sale.class));
    }

    @Test
    void updateSaleDevuelveElStockDeLaVarianteAnteriorYDescuentaElDeLaNueva() throws Exception {
        Business business = new Business();
        business.setId(1L);

        ProductType pt = new ProductType();
        pt.setId(1L);
        pt.setName("TV");
        pt.setBusiness(business);

        Product producto = new Product();
        producto.setId(5L);
        producto.setName("TV");
        producto.setBusiness(business);
        producto.setProductType(pt);
        producto.setActive(true);

        ProductVariant anterior = new ProductVariant();
        anterior.setId(5L);
        anterior.setProduct(producto);
        anterior.setBusiness(business);
        anterior.setSku("A");
        anterior.setCost(new BigDecimal("100"));
        anterior.setPrice(new BigDecimal("200"));
        anterior.setStock(7);
        anterior.setActive(true);

        ProductVariant nueva = new ProductVariant();
        nueva.setId(6L);
        nueva.setProduct(producto);
        nueva.setBusiness(business);
        nueva.setSku("B");
        nueva.setCost(new BigDecimal("100"));
        nueva.setPrice(new BigDecimal("200"));
        nueva.setStock(4);
        nueva.setActive(true);
        when(variantRepository.findById(6L)).thenReturn(Optional.of(nueva));

        Sale existingSale = new Sale();
        existingSale.setId(1L);
        existingSale.setBusiness(business);
        existingSale.setKind(SaleKind.VENTA);
        existingSale.setVariant(anterior);
        existingSale.setQuantity(3);
        existingSale.setCurrency("ARS");
        when(salesRepository.findById(1L)).thenReturn(Optional.of(existingSale));

        SaleUpdateRequestDto update = new SaleUpdateRequestDto();
        update.setKind("VENTA");
        update.setVariant(6L);
        update.setDescriptionProduct("Cambio de variante");

        ventaServices.updateSale(update, 1L);

        // La anterior recupera sus 3 unidades y la nueva las entrega
        assertEquals(10, anterior.getStock());
        assertEquals(1, nueva.getStock());
    }

    @Test
    void createVentaConProductoAsociaVarianteALaVenta() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProducto(5L, "TV", new BigDecimal("150"));

        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(5L);

        ventaServices.createVenta(dto);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        assertEquals(5L, captor.getValue().getVariant().getId());
    }

    @Test
    void createVentaSinFechaDePrimeraCuotaLaAgendaEnLaFechaDeLaVenta() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProducto(5L, "TV", new BigDecimal("150"));

        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(5L);
        dto.setDateSale(LocalDate.of(2026, 9, 19));
        dto.setFirstFeeDate(null);

        ventaServices.createVenta(dto);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale guardada = captor.getValue();

        // El plan cuelga de la operación, no del día en que se cargó: con la fecha
        // de venta vieja, la cuota y la fecha final deben ser las de esa fecha
        assertEquals(LocalDate.of(2026, 9, 19), guardada.getFees().get(0).getExpirationDate());
        assertEquals(LocalDate.of(2026, 10, 18), guardada.getFinalPaymentDate());
    }

    @Test
    void createVentaRechazaProductoDeOtroNegocio() {
        Business business = new Business();
        business.setId(999L);
        Product product = new Product();
        product.setId(5L);
        product.setName("TV");
        product.setBusiness(business);
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setProduct(product);
        variant.setBusiness(business);
        variant.setCost(new BigDecimal("100"));
        variant.setPrice(new BigDecimal("200"));
        variant.setStock(5);
        when(variantRepository.findById(5L)).thenReturn(Optional.of(variant));

        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(5L);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> ventaServices.createVenta(dto));
    }

    @Test
    void createVentaConProductoInexistenteLanzaExcepcion() {
        when(variantRepository.findById(99L)).thenReturn(Optional.empty());

        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(99L);

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> ventaServices.createVenta(dto));
    }

    @Test
    void createVentaConCantidadMultiplicaCostoYDescripcion() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProducto(5L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(5L);
        dto.setQuantity(3);
        dto.setAmountFee(new BigDecimal("250"));
        dto.setQuantityFees(3);

        ventaServices.createVenta(dto);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(3, saved.getQuantity());
        assertEquals(0, new BigDecimal("450").compareTo(saved.getCost()));
        assertEquals("3 x Televisor", saved.getDescriptionProduct());
    }

    @Test
    void createVentaConCantidadPorDefectoEsUno() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProducto(5L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(5L);

        ventaServices.createVenta(dto);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(1, saved.getQuantity());
        assertEquals(0, new BigDecimal("150").compareTo(saved.getCost()));
        assertEquals("Televisor", saved.getDescriptionProduct());
    }

    @Test
    void createVentaRechazaCantidadCeroONegativa() {
        prepararProducto(5L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(5L);
        dto.setQuantity(0);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));

        dto.setQuantity(-2);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));
        verify(salesRepository, never()).save(any(Sale.class));
    }

    @Test
    void createVentaDescuentaStockDeLaVariante() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProductoConStock(5L, "Televisor", new BigDecimal("150"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(5L);
        dto.setQuantity(3);
        dto.setAmountFee(new BigDecimal("250"));
        dto.setQuantityFees(3);

        ventaServices.createVenta(dto);

        ArgumentCaptor<ProductVariant> captor = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository).save(captor.capture());
        assertEquals(7, captor.getValue().getStock());
    }

    @Test
    void createVentaConStockInsuficienteEsRechazada() {
        prepararProductoConStock(5L, "Televisor", new BigDecimal("150"), 2);
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(5L);
        dto.setQuantity(3); // mayor al stock

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));

        assertTrue(error.getMessage().contains("Stock insuficiente"));
        verify(salesRepository, never()).save(any(Sale.class));
        verify(variantRepository, never()).save(any(ProductVariant.class));
    }

    @Test
    void createVentaSinStockEsRechazada() {
        prepararProductoConStock(5L, "Televisor", new BigDecimal("150"), 0);
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(5L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));

        assertTrue(error.getMessage().contains("No hay stock"));
        verify(salesRepository, never()).save(any(Sale.class));
    }

    @Test
    void createVentaRechazaVarianteDesactivada() {
        ProductVariant variant = prepararProductoConStock(5L, "Televisor", new BigDecimal("150"), 10);
        variant.setActive(false);
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(5L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));

        assertTrue(error.getMessage().contains("desactivado"));
        verify(salesRepository, never()).save(any(Sale.class));
    }

    @Test
    void createVentaContadoUsaElPrecioDeVentaPorCantidad() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        // Costo 30 y precio de venta 60 en el catálogo
        prepararProductoConPrecio(1L, "Producto", new BigDecimal("30"), new BigDecimal("60"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayments("CONTADO");
        dto.setQuantity(2);
        dto.setQuantityFees(4); // debe forzarse a 1

        SaleResponseDto result = ventaServices.createVenta(dto);

        // total = precio unitario × cantidad = 60 × 2
        assertEquals(new BigDecimal("120"), result.getPriceTotal());

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(Payments.CONTADO, saved.getTypePayments());
        assertEquals(Integer.valueOf(1), saved.getQuiantityFees());
        assertEquals(1, saved.getFees().size());
        assertEquals(0, new BigDecimal("60").compareTo(saved.getAmountFee()));
        // Costo del negocio = costo unitario × cantidad = 30 × 2
        assertEquals(0, new BigDecimal("60").compareTo(saved.getCost()));
    }

    @Test
    void createVentaContadoIgnoraElMontoEnviado() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProductoConPrecio(1L, "Producto", new BigDecimal("30"), new BigDecimal("60"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayments("CONTADO");
        dto.setAmountFee(new BigDecimal("999")); // no debe mandar: al contado manda el catálogo

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(new BigDecimal("60"), result.getPriceTotal());
    }

    @Test
    void createVentaRechazaUnTotalMenorAlPrecioDeVenta() {
        // Catálogo con margen: costo 100, precio de venta 300
        prepararProductoConPrecio(1L, "Producto", new BigDecimal("100"), new BigDecimal("300"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayments("MENSUAL");
        dto.setQuantityFees(1);
        dto.setAmountFee(new BigDecimal("250")); // 250 < 300: por debajo del precio de lista

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));

        assertTrue(error.getMessage().contains("precio de venta"));
        verify(salesRepository, never()).save(any(Sale.class));
    }

    @Test
    void createVentaEnCuotasNoPuedeBajarDeLaSumaDeLosPreciosUnitarios() {
        prepararProductoConPrecio(1L, "Producto", new BigDecimal("100"), new BigDecimal("300"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayments("MENSUAL");
        dto.setQuantity(2);
        dto.setQuantityFees(2);
        dto.setAmountFee(new BigDecimal("250")); // 500 < 600 (300 × 2)

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));

        assertTrue(error.getMessage().contains("600"));
        verify(salesRepository, never()).save(any(Sale.class));
    }

    @Test
    void createVentaEnCuotasAceptaJustoElPrecioDeLista() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProductoConPrecio(1L, "Producto", new BigDecimal("100"), new BigDecimal("300"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayments("MENSUAL");
        dto.setQuantityFees(2);
        dto.setAmountFee(new BigDecimal("150")); // 300 = precio de lista

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(new BigDecimal("300"), result.getPriceTotal());
    }

    @Test
    void createVentaEnCuotasNoUsaElPrecioDeListaComoTotal() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProductoConStock(1L, "Producto", new BigDecimal("30"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayments("MENSUAL");
        dto.setQuantityFees(3);
        dto.setAmountFee(new BigDecimal("100"));

        SaleResponseDto result = ventaServices.createVenta(dto);

        // El precio de contado (30) es referencia; el total financiado es 3 × 100
        assertEquals(new BigDecimal("300"), result.getPriceTotal());

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("30").compareTo(captor.getValue().getUnitPrice()));
    }

    @Test
    void deleteVentaReponeStockDeLaVariante() throws Exception {
        Business business = new Business();
        business.setId(1L);
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setStock(7);
        Sale sale = new Sale();
        sale.setId(1L);
        sale.setBusiness(business);
        sale.setKind(SaleKind.VENTA);
        sale.setVariant(variant);
        sale.setQuantity(3);
        sale.setFees(new java.util.ArrayList<>());
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        ventaServices.deleteVenta(1L);

        assertEquals(10, variant.getStock());
        verify(variantRepository).save(variant);
        verify(saleDocumentRepository).deleteBySaleId(1L);
        verify(salesRepository).delete(sale);
    }

    @Test
    void deleteVentaEliminaSusRemitosAntesDeLaVenta() throws Exception {
        Business business = new Business();
        business.setId(1L);
        Sale sale = new Sale();
        sale.setId(7L);
        sale.setBusiness(business);
        sale.setKind(SaleKind.PRESTAMO);
        sale.setFees(new java.util.ArrayList<>());
        when(salesRepository.findById(7L)).thenReturn(Optional.of(sale));

        ventaServices.deleteVenta(7L);

        verify(saleDocumentRepository).deleteBySaleId(7L);
        verify(salesRepository).delete(sale);
    }

    @Test
    void deleteVentaDePrestamoNoTocaStock() throws Exception {
        Business business = new Business();
        business.setId(1L);
        Sale sale = new Sale();
        sale.setId(1L);
        sale.setBusiness(business);
        sale.setKind(SaleKind.PRESTAMO);
        sale.setQuantity(3);
        sale.setFees(new java.util.ArrayList<>());
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        ventaServices.deleteVenta(1L);

        verify(variantRepository, never()).save(any(ProductVariant.class));
        verify(salesRepository).delete(sale);
    }

    @Test
    void deleteVentaCanceladaNoReponeStockDosVeces() throws Exception {
        Business business = new Business();
        business.setId(1L);
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setStock(7);
        Sale sale = new Sale();
        sale.setId(1L);
        sale.setBusiness(business);
        sale.setKind(SaleKind.VENTA);
        sale.setVariant(variant);
        sale.setQuantity(3);
        sale.setStatus(SaleStatus.CANCELED);
        sale.setFees(new java.util.ArrayList<>());
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        ventaServices.deleteVenta(1L);

        // El stock ya se repuso al anular: no se suma de nuevo
        verify(variantRepository, never()).save(any(ProductVariant.class));
        assertEquals(7, variant.getStock());
        verify(salesRepository).delete(sale);
    }

    private Sale ventaConCuotaImpaga(BigDecimal remaining, BigDecimal amountCuota) {
        Sale sale = new Sale();
        sale.setId(1L);
        // El saldo se deriva del total y de los cobros: sin cobros, el total es la deuda
        sale.setPriceTotal(remaining);
        sale.setAmountFee(amountCuota);
        sale.setRemainingAmount(remaining);
        sale.setStatus(SaleStatus.ACTIVE);
        Business business = new Business();
        business.setId(1L);
        sale.setBusiness(business);
        Fee cuota = new Fee();
        cuota.setId(1L);
        cuota.setSale(sale);
        cuota.setNumberFee(1);
        cuota.setAmount(amountCuota);
        cuota.setPaid(false);
        sale.setFees(java.util.List.of(cuota));
        return sale;
    }

    @Test
    void markPrestamoAsCompletedPermiteMontoDistintoAlValorDeLaCuota() {
        Sale sale = ventaConCuotaImpaga(new BigDecimal("200"), new BigDecimal("100"));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        ventaServices.markPrestamoAsCompleted(1L, new BigDecimal("150"));

        assertEquals(0, new BigDecimal("50").compareTo(sale.getRemainingAmount()));
        assertEquals(SaleStatus.ACTIVE, sale.getStatus());
        assertEquals(0, new BigDecimal("150").compareTo(sale.getFees().get(0).getPaidAmount()));
        // La cuota se cierra por lo cobrado
        assertEquals(Boolean.TRUE, sale.getFees().get(0).getPaid());
        assertEquals(0, new BigDecimal("150").compareTo(sale.getFees().get(0).getAmount()));
    }

    @Test
    void markPrestamoAsCompletedRechazaMontoMayorAlSaldoRestante() {
        Sale sale = ventaConCuotaImpaga(new BigDecimal("100"), new BigDecimal("100"));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.markPrestamoAsCompleted(1L, new BigDecimal("150")));

        assertEquals(0, new BigDecimal("100").compareTo(sale.getRemainingAmount()));
        assertEquals(Boolean.FALSE, sale.getFees().get(0).getPaid());
    }

    private Fee cuotaPaga(BigDecimal monto) {
        Fee fee = new Fee();
        fee.setId(1L);
        fee.setNumberFee(1);
        fee.setAmount(monto);
        fee.setPaidAmount(monto);
        fee.setPaid(true);
        fee.setExpirationDate(LocalDate.now());
        fee.setLastPaymentAt(LocalDateTime.now());
        return fee;
    }

    private Fee cuotaImpaga(BigDecimal monto) {
        Fee fee = new Fee();
        fee.setId(2L);
        fee.setNumberFee(2);
        fee.setAmount(monto);
        fee.setPaid(false);
        fee.setExpirationDate(LocalDate.now().plusMonths(1));
        return fee;
    }

    private Sale ventaParaAnular(BigDecimal remaining, ProductVariant variant, List<Fee> cuotas) {
        Business business = new Business();
        business.setId(1L);
        Client client = new Client();
        client.setId(1L);
        client.setName("Juan");
        client.setTelefono("1144556677");
        Sale sale = new Sale();
        sale.setId(1L);
        sale.setBusiness(business);
        sale.setClient(client);
        sale.setKind(SaleKind.VENTA);
        sale.setDescriptionProduct("Televisor");
        sale.setPriceTotal(remaining);
        sale.setCost(new BigDecimal("100"));
        sale.setAmountFee(new BigDecimal("100"));
        sale.setQuiantityFees(2);
        sale.setTypePayments(Payments.MENSUAL);
        sale.setDateCreation(LocalDate.now());
        sale.setRemainingAmount(remaining);
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setVariant(variant);
        sale.setQuantity(3);
        java.util.ArrayList<Fee> lista = new java.util.ArrayList<>(cuotas);
        lista.forEach(fee -> fee.setSale(sale));
        sale.setFees(lista);
        return sale;
    }

    @Test
    void cancelVentaMarcaEstadoCanceladoYDevuelveDto() throws Exception {
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setStock(5);
        Sale sale = ventaParaAnular(new BigDecimal("200"), variant, List.of(cuotaImpaga(new BigDecimal("100"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        SaleResponseDto result = ventaServices.cancelVenta(1L, true, true, null);

        assertEquals(SaleStatus.CANCELED.name(), result.getStatus());
        verify(salesRepository).save(sale);
    }

    @Test
    void cancelVentaConRestockReponeStockDeLaVariante() throws Exception {
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("200"), variant, List.of(cuotaImpaga(new BigDecimal("100"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        ventaServices.cancelVenta(1L, true, true, null);

        assertEquals(10, variant.getStock());
        verify(variantRepository).save(variant);
    }

    @Test
    void cancelVentaSinRestockNoTocaElInventario() throws Exception {
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("150"), variant,
                List.of(cuotaPaga(new BigDecimal("100")), cuotaImpaga(new BigDecimal("50"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        // El producto no volvió: la anulación no toca el stock, pero la
        // devolución del monto cobrado se asienta igual.
        SaleResponseDto result = ventaServices.cancelVenta(1L, true, false, null);

        assertEquals(7, variant.getStock());
        verify(variantRepository, never()).save(any(ProductVariant.class));
        assertEquals(0, new BigDecimal("100").compareTo(result.getRefundAmount()));
    }

    @Test
    void cancelVentaConRestockEnPrestamoRechaza() {
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("200"), variant, List.of(cuotaImpaga(new BigDecimal("100"))));
        sale.setKind(SaleKind.PRESTAMO);
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        assertThrows(IllegalArgumentException.class,
                () -> ventaServices.cancelVenta(1L, false, true, null));

        verify(salesRepository, never()).save(any(Sale.class));
        verify(variantRepository, never()).save(any(ProductVariant.class));
    }

    @Test
    void cancelVentaConRestockEnVentaSinVarianteRechaza() {
        Sale sale = ventaParaAnular(new BigDecimal("200"), null, List.of(cuotaImpaga(new BigDecimal("100"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        assertThrows(IllegalArgumentException.class,
                () -> ventaServices.cancelVenta(1L, false, true, null));

        verify(salesRepository, never()).save(any(Sale.class));
    }

    @Test
    void cancelVentaCalculaRefundDeCuotasPagadasYDejaRemainingEnCero() throws Exception {
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("150"), variant,
                List.of(cuotaPaga(new BigDecimal("100")), cuotaImpaga(new BigDecimal("50"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        ventaServices.cancelVenta(1L, true, true, null);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(0, new BigDecimal("100").compareTo(saved.getRefundAmount()));
        assertEquals(0, saved.getRemainingAmount().signum());
        assertEquals(LocalDate.now(), saved.getRealFinalDate());
        assertEquals(SaleStatus.CANCELED, saved.getStatus());
    }

    @Test
    void cancelVentaConRefundTrueDevuelveMontoCobradoEnElDto() throws Exception {
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("150"), variant,
                List.of(cuotaPaga(new BigDecimal("100")), cuotaImpaga(new BigDecimal("50"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        SaleResponseDto result = ventaServices.cancelVenta(1L, true, true, null);

        assertEquals(0, new BigDecimal("100").compareTo(result.getRefundAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(result.getCollectedAmount()));
        assertEquals(0, new BigDecimal("0").compareTo(result.getRemainingAmount()));
        assertEquals(SaleStatus.CANCELED.name(), result.getStatus());
    }

    @Test
    void cancelVentaConRefundFalseRetieneElMontoCobrado() throws Exception {
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("150"), variant,
                List.of(cuotaPaga(new BigDecimal("100")), cuotaImpaga(new BigDecimal("50"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        SaleResponseDto result = ventaServices.cancelVenta(1L, false, true, null);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(0, saved.getRefundAmount().signum());
        assertEquals(0, saved.getRemainingAmount().signum());
        assertEquals(LocalDate.now(), saved.getRealFinalDate());
        assertEquals(SaleStatus.CANCELED, saved.getStatus());
        assertEquals(0, new BigDecimal("100").compareTo(result.getCollectedAmount()));
    }

    @Test
    void cancelVentaConRefundFalseNoRegistraDevolucionYDejaDeudaEnCero() throws Exception {
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("150"), variant,
                List.of(cuotaPaga(new BigDecimal("100")), cuotaImpaga(new BigDecimal("50"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        SaleResponseDto result = ventaServices.cancelVenta(1L, false, true, null);

        // Sin devolución y sin deuda pendiente: el registro anulado no aporta a la
        // deuda del cliente (remaining = 0) ni a los reembolsos (refundAmount = 0).
        assertEquals(0, result.getRefundAmount().signum());
        assertEquals(0, result.getRemainingAmount().signum());
        assertEquals(SaleStatus.CANCELED.name(), result.getStatus());
    }

    @Test
    void cancelVentaSinCuotasPagadasDevuelveRefundEnCero() throws Exception {
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setStock(5);
        Sale sale = ventaParaAnular(new BigDecimal("200"), variant, List.of(cuotaImpaga(new BigDecimal("100"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        SaleResponseDto result = ventaServices.cancelVenta(1L, true, true, null);

        assertEquals(0, result.getRefundAmount().signum());
        assertEquals(0, new BigDecimal("0").compareTo(result.getCollectedAmount()));
        assertEquals(0, result.getRemainingAmount().signum());
    }

    @Test
    void cancelVentaYaCanceladaEsIdempotente() throws Exception {
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("0"), variant, List.of(cuotaPaga(new BigDecimal("200"))));
        sale.setStatus(SaleStatus.CANCELED);
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        SaleResponseDto result = ventaServices.cancelVenta(1L, true, true, null);

        assertEquals(SaleStatus.CANCELED.name(), result.getStatus());
        verify(salesRepository, never()).save(any(Sale.class));
        verify(variantRepository, never()).save(any(ProductVariant.class));
        assertEquals(7, variant.getStock());
    }

    @Test
    void updateSaleRechazaVentaAnulada() {
        Business business = new Business();
        business.setId(1L);
        Sale sale = new Sale();
        sale.setId(1L);
        sale.setBusiness(business);
        sale.setStatus(SaleStatus.CANCELED);
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        assertThrows(IllegalStateException.class,
                () -> ventaServices.updateSale(new SaleUpdateRequestDto(), 1L));

        verify(salesRepository, never()).save(any(Sale.class));
    }

    @Test
    void markPrestamoAsCompletedRechazaVentaAnulada() {
        Sale sale = ventaConCuotaImpaga(new BigDecimal("100"), new BigDecimal("100"));
        sale.setStatus(SaleStatus.CANCELED);
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        assertThrows(IllegalStateException.class,
                () -> ventaServices.markPrestamoAsCompleted(1L, new BigDecimal("100")));
    }

    @Test
    void cancelVentaRegistraAuditoriaYFechaDeDevolucion() throws Exception {
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("150"), variant,
                List.of(cuotaPaga(new BigDecimal("100")), cuotaImpaga(new BigDecimal("50"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario usuario = new Usuario();
        usuario.setEmail("duena@negocio.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));

        ventaServices.cancelVenta(1L, true, true, "  El cliente devolvio el producto  ");

        // La auditoría guarda fecha, usuario y el motivo sin espacios al borde
        assertNotNull(sale.getCanceledAt());
        assertEquals("duena@negocio.com", sale.getCanceledBy());
        assertEquals("El cliente devolvio el producto", sale.getCancelReason());
        // Y la devolución queda fechada el día en que se anuló
        assertEquals(LocalDate.now(), sale.getRefundAt());
    }

    @Test
    void cancelVentaSinDevolucionNoFechaElReembolso() throws Exception {
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("150"), variant,
                List.of(cuotaPaga(new BigDecimal("100")), cuotaImpaga(new BigDecimal("50"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        SaleResponseDto result = ventaServices.cancelVenta(1L, false, false, null);

        assertEquals(0, result.getRefundAmount().signum());
        assertNull(sale.getRefundAt());
        assertNull(sale.getCancelReason());
    }

    @Test
    void deleteVentaRechazaSiTieneRemitoEmitido() {
        Sale sale = new Sale();
        sale.setId(1L);
        Business business = new Business();
        business.setId(1L);
        sale.setBusiness(business);
        sale.setKind(SaleKind.VENTA);
        sale.setFees(new java.util.ArrayList<>());
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(saleDocumentRepository.findBySaleIdAndType(1L, DocumentType.REMITO))
                .thenReturn(Optional.of(new SaleDocument()));

        assertThrows(IllegalArgumentException.class, () -> ventaServices.deleteVenta(1L));

        verify(salesRepository, never()).delete(any(Sale.class));
    }

    @Test
    void deleteVentaRechazaSiTieneCuotasPagadas() {
        ProductVariant variant = new ProductVariant();
        variant.setId(5L);
        variant.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("100"), variant, List.of(cuotaPaga(new BigDecimal("100"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.deleteVenta(1L));

        verify(salesRepository, never()).delete(any(Sale.class));
        verify(variantRepository, never()).save(any(ProductVariant.class));
    }

    // --- Tests de tipos de pago no MENSUAL ---

    @Test
    void createVentaSemanal() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        prepararProducto(1L, "Producto", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayments("SEMANAL");
        dto.setQuantityFees(4);

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(new BigDecimal("400"), result.getPriceTotal());
        assertEquals(SaleStatus.ACTIVE.name(), result.getStatus());

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(Payments.SEMANAL, saved.getTypePayments());
        assertEquals(Integer.valueOf(4), saved.getQuiantityFees());
    }

    @Test
    void createVentaQuincenal() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        prepararProducto(1L, "Producto", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayments("QUINCENAL");
        dto.setQuantityFees(6);

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(new BigDecimal("600"), result.getPriceTotal());
        assertEquals(SaleStatus.ACTIVE.name(), result.getStatus());

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(Payments.QUINCENAL, saved.getTypePayments());
        assertEquals(Integer.valueOf(6), saved.getQuiantityFees());
    }

    @Test
    void createPrestamoContadoRechazado() {
        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("200"));
        dto.setPayments("CONTADO");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> ventaServices.createVenta(dto));
    }

    @Test
    void createVentaConPayFirstFee() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        prepararProducto(1L, "Producto", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayFirstFee(true);
        dto.setQuantityFees(3);
        dto.setFirstFeeAmount(new BigDecimal("200"));

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(new BigDecimal("300"), result.getPriceTotal());
        assertEquals(SaleStatus.ACTIVE.name(), result.getStatus());

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        // 2 fees: [siguienteCuota (impaga), primeraCuota (pagada)]
        assertEquals(2, saved.getFees().size());
        // La primera en la lista es la siguiente (impaga, numero 2)
        Fee siguiente = saved.getFees().get(0);
        assertEquals(Boolean.FALSE, siguiente.getPaid());
        assertEquals(Integer.valueOf(2), siguiente.getNumberFee());
        // La cuota siguiente se genera por el monto estandar de la venta
        assertEquals(0, new BigDecimal("100").compareTo(siguiente.getAmount()));
        // La segunda es la primera cuota: queda cerrada por lo cobrado
        Fee primera = saved.getFees().get(1);
        assertEquals(Boolean.TRUE, primera.getPaid());
        assertEquals(Integer.valueOf(1), primera.getNumberFee());
        assertEquals(0, new BigDecimal("200").compareTo(primera.getAmount()));
        assertEquals(0, new BigDecimal("200").compareTo(primera.getPaidAmount()));
        // Saldo restante = 300 - 200 = 100
        assertEquals(0, new BigDecimal("100").compareTo(saved.getRemainingAmount()));
    }

    @Test
    void createVentaConPayFirstFeeCubreTodoElSaldo() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        prepararProducto(1L, "Producto", new BigDecimal("30"));
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayFirstFee(true);
        dto.setQuantityFees(1);
        dto.setAmountFee(new BigDecimal("400"));
        dto.setFirstFeeAmount(new BigDecimal("400")); // paga todo

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(SaleStatus.COMPLETED.name(), result.getStatus());

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        // Solo 1 fee (pagada) porque remaining = 0, no se genera segunda
        assertEquals(1, saved.getFees().size());
        assertEquals(Boolean.TRUE, saved.getFees().get(0).getPaid());
        assertEquals(Integer.valueOf(1), saved.getFees().get(0).getNumberFee());
    }

    @Test
    void createVentaConPrimeraCuotaParcialAcotaLaCuotaSiguienteAlSaldo() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        prepararProducto(1L, "Producto", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setVariant(1L);
        dto.setPayFirstFee(true);
        dto.setQuantityFees(3); // total 300, cuota de 100
        dto.setFirstFeeAmount(new BigDecimal("250")); // se cobra mas que la cuota

        ventaServices.createVenta(dto);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        // Queda debiendo 50: la cuota siguiente se agenda por esa deuda, no por 100
        assertEquals(0, new BigDecimal("50").compareTo(saved.getRemainingAmount()));
        Fee siguiente = saved.getFees().get(0);
        assertEquals(Boolean.FALSE, siguiente.getPaid());
        assertEquals(Integer.valueOf(2), siguiente.getNumberFee());
        assertEquals(0, new BigDecimal("50").compareTo(siguiente.getAmount()));
    }

    // --- Vendedor sellado de la operación ---

    @Test
    void createVentaSellaElVendedorDeLaOperacion() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        prepararProducto(1L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(1L);
        dto.setSellerId(2L);

        Client vendedor = new Client();
        vendedor.setId(2L);
        vendedor.setName("luis");
        vendedor.setSeller(true);
        when(clientRepository.findById(2L)).thenReturn(Optional.of(vendedor));

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(2L, result.getSellerId());
        assertEquals("luis", result.getSellerName());
    }

    @Test
    void createVentaRechazaAtribuirseAlMismoCliente() throws Exception {
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setVariant(1L);
        dto.setSellerId(1L); // la propia persona que compra

        Client cliente = new Client();
        cliente.setId(1L);
        cliente.setSeller(true);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));

        assertEquals("Una operación no puede atribuirse a la misma persona que la compra.", ex.getMessage());
    }

    @Test
    void updateSaleCorrigeElVendedorSellado() throws Exception {
        Client cliente = new Client();
        cliente.setId(1L);

        Sale sale = new Sale();
        sale.setId(10L);
        sale.setClient(cliente);
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setKind(SaleKind.PRESTAMO);
        when(salesRepository.findById(10L)).thenReturn(Optional.of(sale));

        Client vendedor = new Client();
        vendedor.setId(2L);
        vendedor.setName("luis");
        vendedor.setSeller(true);
        when(clientRepository.findById(2L)).thenReturn(Optional.of(vendedor));

        SaleUpdateRequestDto dto = new SaleUpdateRequestDto();
        dto.setKind("PRESTAMO");
        dto.setDescriptionProduct("Préstamo personal");
        dto.setSellerId(2L);

        ventaServices.updateSale(dto, 10L);

        assertEquals(2L, sale.getSeller().getId());
        verify(salesRepository).save(sale);
    }

    @Test
    void updateSaleSinVendedorDejaLaOperacionSinAtribuir() throws Exception {
        Client cliente = new Client();
        cliente.setId(1L);

        Client vendedor = new Client();
        vendedor.setId(2L);
        vendedor.setSeller(true);

        Sale sale = new Sale();
        sale.setId(10L);
        sale.setClient(cliente);
        sale.setSeller(vendedor);
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setKind(SaleKind.PRESTAMO);
        when(salesRepository.findById(10L)).thenReturn(Optional.of(sale));

        SaleUpdateRequestDto dto = new SaleUpdateRequestDto();
        dto.setKind("PRESTAMO");
        dto.setDescriptionProduct("Préstamo personal");

        ventaServices.updateSale(dto, 10L);

        assertNull(sale.getSeller());
    }
}
