package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.PriceChange;
import com.app.digital.payments.digital_pyments.models.Product;
import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.ProductVariant;
import com.app.digital.payments.digital_pyments.models.dtos.BulkPriceRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.BulkPriceResultDto;
import com.app.digital.payments.digital_pyments.models.dtos.BulkPriceRowDto;
import com.app.digital.payments.digital_pyments.models.dtos.PriceChangeDto;
import com.app.digital.payments.digital_pyments.repositories.IAttributeRepository;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IPriceChangeRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductVariantRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

/**
 * Ajuste masivo de precios: cálculo, invariantes del catálogo e historial.
 */
class ProductBulkPriceTest {

    private static final Long NEGOCIO_ID = 7L;

    @Mock
    private IProductRepository productRepository;
    @Mock
    private IProductVariantRepository variantRepository;
    @Mock
    private IAttributeRepository attributeRepository;
    @Mock
    private IProductTypeRepository productTypeRepository;
    @Mock
    private ISaleRepository saleRepository;
    @Mock
    private IBusinessRepository businessRepository;
    @Mock
    private IPriceChangeRepository priceChangeRepository;

    private ProductServices productServices;
    private Business negocio;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productServices = new ProductServices(productRepository, variantRepository, attributeRepository,
                productTypeRepository, saleRepository, businessRepository, priceChangeRepository);
        TenantContext.setBusinessId(NEGOCIO_ID);
        negocio = new Business();
        negocio.setId(NEGOCIO_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ProductType categoria(Long id, ProductType padre) {
        ProductType tipo = new ProductType();
        tipo.setId(id);
        tipo.setName("Categoría " + id);
        tipo.setBusiness(negocio);
        tipo.setParent(padre);
        return tipo;
    }

    private ProductVariant variante(Long id, String nombre, String sku, String precio, String costo,
            ProductType categoria) {
        Product producto = new Product();
        producto.setId(id);
        producto.setName(nombre);
        producto.setCurrency("ARS");
        producto.setActive(true);
        producto.setBusiness(negocio);
        producto.setProductType(categoria);

        ProductVariant variante = new ProductVariant();
        variante.setId(id);
        variante.setProduct(producto);
        variante.setBusiness(negocio);
        variante.setSku(sku);
        variante.setPrice(new BigDecimal(precio));
        variante.setCost(new BigDecimal(costo));
        variante.setStock(3);
        variante.setActive(true);
        return variante;
    }

    private BulkPriceRequestDto pedido(String alcance, String modo, String valor) {
        BulkPriceRequestDto request = new BulkPriceRequestDto();
        request.setScope(alcance);
        request.setMode(modo);
        request.setValue(new BigDecimal(valor));
        request.setAdjustCost(false);
        return request;
    }

    // ------------------------------------------------------------------
    // Preview
    // ------------------------------------------------------------------

    @Test
    void previewPorcentajeCalculaElPrecioNuevoYNoEscribeNada() {
        ProductVariant tv = variante(1L, "TV 32", "TV-1", "1000.00", "600.00", categoria(10L, null));
        when(variantRepository.findAllByBusinessId(NEGOCIO_ID)).thenReturn(List.of(tv));

        BulkPriceResultDto resultado = productServices.previewBulkPrice(pedido("ALL", "PERCENT", "10"));

        assertEquals(1, resultado.getTotalCount());
        assertEquals(1, resultado.getChangedCount());
        assertNull(resultado.getBatchId());
        BulkPriceRowDto fila = resultado.getRows().get(0);
        assertEquals(new BigDecimal("1100.00"), fila.getNewPrice());
        assertEquals(new BigDecimal("1000.00"), fila.getOldPrice());
        // Sin adjustCost el costo no se toca.
        assertEquals(new BigDecimal("600.00"), fila.getNewCost());
        assertEquals("TV-1", fila.getSku());
        assertEquals("TV 32", fila.getProductName());
        assertEquals("ARS", fila.getCurrency());
        assertTrue(fila.getChanged());

        verify(variantRepository, never()).saveAll(any());
        verifyNoInteractions(priceChangeRepository);
    }

    // ------------------------------------------------------------------
    // Aplicación
    // ------------------------------------------------------------------

    @Test
    void aplicaPorcentajeAlPrecioYAlCostoYCierraElHistorial() {
        ProductVariant tv = variante(1L, "TV 32", "TV-1", "1000.00", "600.00", categoria(10L, null));
        when(variantRepository.findAllByBusinessId(NEGOCIO_ID)).thenReturn(List.of(tv));

        BulkPriceRequestDto request = pedido("ALL", "PERCENT", "10");
        request.setAdjustCost(true);

        BulkPriceResultDto resultado = productServices.applyBulkPrice(request);

        assertEquals(new BigDecimal("1100.00"), tv.getPrice());
        assertEquals(new BigDecimal("660.00"), tv.getCost());
        assertNotNull(resultado.getBatchId());
        assertEquals(1, resultado.getChangedCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PriceChange>> captor = ArgumentCaptor.forClass(List.class);
        verify(priceChangeRepository).saveAll(captor.capture());
        List<PriceChange> historial = captor.getValue();
        assertEquals(1, historial.size());
        PriceChange cambio = historial.get(0);
        assertEquals("TV-1", cambio.getSku());
        assertEquals("TV 32", cambio.getProductName());
        assertEquals(new BigDecimal("1000.00"), cambio.getOldPrice());
        assertEquals(new BigDecimal("1100.00"), cambio.getNewPrice());
        assertEquals(new BigDecimal("600.00"), cambio.getOldCost());
        assertEquals(new BigDecimal("660.00"), cambio.getNewCost());
        assertEquals("PERCENT", cambio.getAdjustmentMode());
        assertEquals(new BigDecimal("10"), cambio.getAdjustmentValue());
        assertEquals(resultado.getBatchId(), cambio.getBatchId());
        assertNotNull(cambio.getCreatedAt());
        assertTrue(cambio.getAdjustCost());
    }

    @Test
    void aplicaMontoFijoYRedondeaElPrecio() {
        ProductVariant tv = variante(1L, "TV 32", "TV-1", "1234.56", "500.00", categoria(10L, null));
        when(variantRepository.findAllByBusinessId(NEGOCIO_ID)).thenReturn(List.of(tv));

        BulkPriceRequestDto request = pedido("ALL", "AMOUNT", "100");
        request.setRoundTo(new BigDecimal("100"));

        BulkPriceResultDto resultado = productServices.applyBulkPrice(request);

        // 1234.56 + 100 = 1334.56, redondeado a la centena más cercana.
        assertEquals(new BigDecimal("1300.00"), tv.getPrice());
        assertEquals(new BigDecimal("500.00"), tv.getCost());
        assertEquals(new BigDecimal("1300.00"), resultado.getRows().get(0).getNewPrice());
    }

    @Test
    void rechazaElLoteEnteroCuandoElPrecioQuedaBajoElCosto() {
        ProductVariant tv = variante(1L, "TV 32", "TV-1", "1000.00", "900.00", categoria(10L, null));
        when(variantRepository.findAllByBusinessId(NEGOCIO_ID)).thenReturn(List.of(tv));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.applyBulkPrice(pedido("ALL", "PERCENT", "-20")));

        assertTrue(error.getMessage().contains("TV-1"));
        assertTrue(error.getMessage().contains("costo"));
        // Sin mutación parcial: el precio sigue como estaba.
        assertEquals(new BigDecimal("1000.00"), tv.getPrice());
        verify(variantRepository, never()).saveAll(any());
        verifyNoInteractions(priceChangeRepository);
    }

    @Test
    void rechazaElAjusteQueNoCambiaNingunPrecio() {
        ProductVariant tv = variante(1L, "TV 32", "TV-1", "1000.00", "500.00", categoria(10L, null));
        when(variantRepository.findAllByBusinessId(NEGOCIO_ID)).thenReturn(List.of(tv));

        BulkPriceRequestDto request = pedido("ALL", "AMOUNT", "10");
        request.setRoundTo(new BigDecimal("100"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.applyBulkPrice(request));

        assertTrue(error.getMessage().contains("no cambia ningún precio"));
        verifyNoInteractions(priceChangeRepository);
    }

    @Test
    void rechazaValorCeroYAlcanceDesconocido() {
        assertThrows(IllegalArgumentException.class,
                () -> productServices.previewBulkPrice(pedido("ALL", "PERCENT", "0")));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.previewBulkPrice(pedido("SUCURSAL", "PERCENT", "10")));
        assertTrue(error.getMessage().contains("alcance"));
    }

    // ------------------------------------------------------------------
    // Alcance
    // ------------------------------------------------------------------

    @Test
    void elAlcancePorCategoriaIncluyeLasSubcategorias() {
        ProductType padre = categoria(10L, null);
        ProductType hija = categoria(11L, padre);
        ProductVariant tv = variante(1L, "TV 32", "TV-1", "1000.00", "500.00", hija);

        when(productTypeRepository.findById(10L)).thenReturn(Optional.of(padre));
        when(productTypeRepository.findAllByBusinessId(NEGOCIO_ID)).thenReturn(List.of(padre, hija));
        when(variantRepository.findAllByBusinessIdAndProductProductTypeIdIn(any(), any())).thenReturn(List.of(tv));

        BulkPriceRequestDto request = pedido("CATEGORY", "PERCENT", "10");
        request.setCategoryId(10L);
        BulkPriceResultDto resultado = productServices.previewBulkPrice(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(variantRepository).findAllByBusinessIdAndProductProductTypeIdIn(any(), captor.capture());
        assertTrue(captor.getValue().containsAll(List.of(10L, 11L)));
        assertEquals(1, resultado.getTotalCount());
    }

    @Test
    void rechazaSkusQueNoPertenecenAlNegocio() {
        ProductVariant propio = variante(1L, "TV 32", "TV-1", "1000.00", "500.00", categoria(10L, null));
        when(variantRepository.findAllByBusinessIdAndIdIn(any(), any())).thenReturn(List.of(propio));

        BulkPriceRequestDto request = pedido("VARIANTS", "PERCENT", "10");
        request.setVariantIds(List.of(1L, 2L));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.previewBulkPrice(request));
        assertTrue(error.getMessage().contains("no pertenecen al negocio"));
    }

    // ------------------------------------------------------------------
    // Historial
    // ------------------------------------------------------------------

    @Test
    void devuelveElHistorialDelProductoComoDto() {
        Product producto = new Product();
        producto.setId(5L);
        producto.setBusiness(negocio);
        when(productRepository.findById(5L)).thenReturn(Optional.of(producto));

        PriceChange cambio = new PriceChange();
        cambio.setId(99L);
        cambio.setVariantId(1L);
        cambio.setProductId(5L);
        cambio.setProductName("TV 32");
        cambio.setSku("TV-1");
        cambio.setCurrency("ARS");
        cambio.setOldPrice(new BigDecimal("1000.00"));
        cambio.setNewPrice(new BigDecimal("1100.00"));
        cambio.setOldCost(new BigDecimal("600.00"));
        cambio.setNewCost(new BigDecimal("660.00"));
        cambio.setAdjustmentMode("PERCENT");
        cambio.setAdjustmentValue(new BigDecimal("10"));
        cambio.setAdjustCost(true);
        cambio.setBatchId("lote-1");
        cambio.setCreatedAt(LocalDateTime.of(2026, 11, 11, 15, 30));
        cambio.setCreatedBy("duenio@negocio.com");
        when(priceChangeRepository.findTop100ByBusinessIdAndProductIdOrderByCreatedAtDescIdDesc(NEGOCIO_ID, 5L))
                .thenReturn(new ArrayList<>(List.of(cambio)));

        List<PriceChangeDto> historial = productServices.priceHistory(5L);

        assertEquals(1, historial.size());
        PriceChangeDto dto = historial.get(0);
        assertEquals(99L, dto.getId());
        assertEquals("TV-1", dto.getSku());
        assertEquals(new BigDecimal("1100.00"), dto.getNewPrice());
        assertEquals("duenio@negocio.com", dto.getCreatedBy());
        assertFalse(dto.getOldPrice().compareTo(dto.getNewPrice()) > 0);
    }
}