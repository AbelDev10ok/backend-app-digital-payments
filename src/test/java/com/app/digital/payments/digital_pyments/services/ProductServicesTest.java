package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.models.Attribute;
import com.app.digital.payments.digital_pyments.models.AttributeValue;
import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Product;
import com.app.digital.payments.digital_pyments.models.ProductAttribute;
import com.app.digital.payments.digital_pyments.models.ProductVariant;
import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.VariantAttribute;
import com.app.digital.payments.digital_pyments.models.dtos.ProductDto;
import com.app.digital.payments.digital_pyments.models.dtos.ProductRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.ProductVariantRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IAttributeRepository;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductVariantRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

class ProductServicesTest {

    private static final Long NEGOCIO_ID = 1L;

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

    private ProductServices productServices;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productServices = new ProductServices(productRepository, variantRepository, attributeRepository,
                productTypeRepository, saleRepository, businessRepository);
        TenantContext.setBusinessId(NEGOCIO_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ProductRequestDto pedidoSimple(String nombre, String costo, String precio, Integer stock) {
        ProductRequestDto request = new ProductRequestDto();
        request.setName(nombre);
        request.setProductTypeId(1L);

        ProductVariantRequestDto variante = new ProductVariantRequestDto();
        variante.setCost(costo != null ? new BigDecimal(costo) : null);
        variante.setPrice(precio != null ? new BigDecimal(precio) : null);
        variante.setStock(stock);
        request.setVariants(List.of(variante));
        return request;
    }

    private ProductType categoria(Long id) {
        ProductType categoria = new ProductType();
        categoria.setId(id);
        categoria.setName("TV");
        return categoria;
    }

    private Attribute atributo(Long id, String nombre, Long... valoresIds) {
        Attribute atributo = new Attribute();
        atributo.setId(id);
        atributo.setName(nombre);
        List<AttributeValue> valores = new ArrayList<>();
        for (Long valorId : valoresIds) {
            AttributeValue valor = new AttributeValue();
            valor.setId(valorId);
            valor.setAttribute(atributo);
            valor.setValue("Valor " + valorId);
            valores.add(valor);
        }
        atributo.setValues(valores);
        return atributo;
    }

    private Product productoConVariante(Long id, String nombre, ProductVariant variant) {
        Product product = new Product();
        product.setId(id);
        product.setName(nombre);
        product.setActive(true);
        product.setCurrency("ARS");
        product.setProductType(categoria(1L));
        variant.setProduct(product);
        variant.setBusiness(new Business());
        product.setVariants(new ArrayList<>(List.of(variant)));
        return product;
    }

    private ProductVariant variante(Long id, String sku, String costo, String precio, Integer stock) {
        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setSku(sku);
        variant.setCost(new BigDecimal(costo));
        variant.setPrice(new BigDecimal(precio));
        variant.setStock(stock);
        variant.setActive(true);
        variant.setAttributes(new ArrayList<>());
        return variant;
    }

    // ------------------------------------------------------------------
    // Alta
    // ------------------------------------------------------------------

    @Test
    void createProductGuardaProductoYVariante() {
        when(productRepository.existsByNameAndBusinessId("Heladera Samsung", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(businessRepository.getReferenceById(NEGOCIO_ID)).thenReturn(new Business("Mi negocio", "ars"));

        List<ProductDto> creados = productServices.createProduct(
                pedidoSimple("  Heladera Samsung  ", "200", "500", 4));

        assertEquals(1, creados.size());
        assertEquals("Heladera Samsung", creados.get(0).getName());
        assertEquals("", creados.get(0).getVariantLabel());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product guardado = captor.getValue();
        assertEquals("Heladera Samsung", guardado.getName());
        assertEquals(1L, guardado.getProductType().getId());

        ProductVariant variante = guardado.getVariants().get(0);
        assertEquals(0, new BigDecimal("200").compareTo(variante.getCost()));
        assertEquals(0, new BigDecimal("500").compareTo(variante.getPrice()));
        assertEquals(4, variante.getStock());
        // El SKU se genera del nombre cuando no se informa
        assertEquals("HELADERA-SAMSUNG-1", variante.getSku());
        assertEquals(true, variante.getActive());
    }

    @Test
    void createProductCongelaLaMonedaDelNegocio() {
        when(productRepository.existsByNameAndBusinessId("Notebook", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(businessRepository.getReferenceById(NEGOCIO_ID)).thenReturn(new Business("Mi negocio", "usd"));

        productServices.createProduct(pedidoSimple("Notebook", "500", "900", 1));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        // Snapshot: la moneda del negocio queda congelada en el producto
        assertEquals("USD", captor.getValue().getCurrency());
    }

    @Test
    void createProductUsaLaMonedaElegidaEnElAlta() {
        when(productRepository.existsByNameAndBusinessId("Notebook", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));

        ProductRequestDto request = pedidoSimple("Notebook", "500", "900", 1);
        request.setCurrency("usd");

        productServices.createProduct(request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals("USD", captor.getValue().getCurrency());
    }

    @Test
    void createProductRechazaUnaMonedaNoSoportada() {
        ProductRequestDto request = pedidoSimple("TV", "500", "900", 1);
        request.setCurrency("PYG");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.createProduct(request));
        assertTrue(error.getMessage().contains("ARS"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProductRechazaVacioYDuplicado() {
        assertThrows(IllegalArgumentException.class,
                () -> productServices.createProduct(pedidoSimple("   ", "1", "2", 1)));

        when(productRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(true);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.createProduct(pedidoSimple("TV", "1", "2", 1)));
        assertTrue(error.getMessage().contains("Ya existe"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProductRechazaSinCategoria() {
        ProductRequestDto request = pedidoSimple("TV", "500", "900", 1);
        request.setProductTypeId(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.createProduct(request));
        assertTrue(error.getMessage().contains("categoría"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProductRechazaUnaCategoriaQueAgrupaSubcategorias() {
        when(productRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(productTypeRepository.existsByParentId(1L)).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.createProduct(pedidoSimple("TV", "500", "900", 1)));
        assertTrue(error.getMessage().contains("subcategoría"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProductRechazaSinVariantes() {
        when(productRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));

        ProductRequestDto request = new ProductRequestDto();
        request.setName("TV");
        request.setProductTypeId(1L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.createProduct(request));
        assertTrue(error.getMessage().contains("al menos una variante"));
    }

    @Test
    void createProductRechazaPrecioMenorOIgualAlCosto() {
        when(productRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.createProduct(pedidoSimple("TV", "500", "500", 1)));
        assertTrue(error.getMessage().contains("mayor al costo"));
    }

    @Test
    void createProductRechazaStockNegativoONulo() {
        when(productRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));

        IllegalArgumentException negativo = assertThrows(IllegalArgumentException.class,
                () -> productServices.createProduct(pedidoSimple("TV", "100", "200", -1)));
        assertTrue(negativo.getMessage().contains("stock"));

        IllegalArgumentException nulo = assertThrows(IllegalArgumentException.class,
                () -> productServices.createProduct(pedidoSimple("TV", "100", "200", null)));
        assertTrue(nulo.getMessage().contains("stock"));
    }

    @Test
    void createProductRechazaSkuDuplicado() {
        when(productRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(variantRepository.existsByBusinessIdAndSku(NEGOCIO_ID, "TV-1")).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.createProduct(pedidoSimple("TV", "100", "200", 1)));
        assertTrue(error.getMessage().contains("SKU"));
    }

    @Test
    void createProductRechazaCodigoDeBarrasDuplicado() {
        when(productRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(variantRepository.existsByBusinessIdAndBarcode(NEGOCIO_ID, "779123")).thenReturn(true);

        ProductRequestDto request = pedidoSimple("TV", "100", "200", 1);
        request.getVariants().get(0).setSku("TV-1");
        request.getVariants().get(0).setBarcode("779123");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.createProduct(request));
        assertTrue(error.getMessage().contains("código de barras"));
    }

    @Test
    void createProductRechazaUnSoloValorDeAtributoParaDosVariantes() {
        when(productRepository.existsByNameAndBusinessId("Remera", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(atributo(1L, "Color", 10L, 11L)));

        ProductRequestDto request = new ProductRequestDto();
        request.setName("Remera");
        request.setProductTypeId(1L);
        request.setAttributeIds(List.of(1L));

        ProductVariantRequestDto rojo = new ProductVariantRequestDto();
        rojo.setCost(new BigDecimal("100"));
        rojo.setPrice(new BigDecimal("200"));
        rojo.setStock(2);
        rojo.setSku("REM-ROJO");
        rojo.setAttributeValueIds(List.of(10L));

        ProductVariantRequestDto rojoRepetido = new ProductVariantRequestDto();
        rojoRepetido.setCost(new BigDecimal("100"));
        rojoRepetido.setPrice(new BigDecimal("200"));
        rojoRepetido.setStock(2);
        rojoRepetido.setSku("REM-ROJO-2");
        rojoRepetido.setAttributeValueIds(List.of(10L));

        request.setVariants(List.of(rojo, rojoRepetido));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.createProduct(request));
        assertTrue(error.getMessage().contains("misma combinación"));
    }

    @Test
    void createProductRechazaVarianteSinValorDeAtributo() {
        when(productRepository.existsByNameAndBusinessId("Remera", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(atributo(1L, "Color", 10L, 11L)));

        ProductRequestDto request = new ProductRequestDto();
        request.setName("Remera");
        request.setProductTypeId(1L);
        request.setAttributeIds(List.of(1L));

        ProductVariantRequestDto sinValor = new ProductVariantRequestDto();
        sinValor.setCost(new BigDecimal("100"));
        sinValor.setPrice(new BigDecimal("200"));
        sinValor.setStock(2);
        sinValor.setSku("REM");
        request.setVariants(List.of(sinValor));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.createProduct(request));
        assertTrue(error.getMessage().contains("Color"));
    }

    @Test
    void createProductConAtributosGuardaElValorDeCadaVariante() {
        when(productRepository.existsByNameAndBusinessId("Remera", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(atributo(1L, "Color", 10L, 11L)));

        ProductRequestDto request = new ProductRequestDto();
        request.setName("Remera");
        request.setProductTypeId(1L);
        request.setAttributeIds(List.of(1L));

        ProductVariantRequestDto rojo = new ProductVariantRequestDto();
        rojo.setCost(new BigDecimal("100"));
        rojo.setPrice(new BigDecimal("200"));
        rojo.setStock(2);
        rojo.setSku("REM-ROJO");
        rojo.setAttributeValueIds(List.of(10L));

        ProductVariantRequestDto azul = new ProductVariantRequestDto();
        azul.setCost(new BigDecimal("100"));
        azul.setPrice(new BigDecimal("200"));
        azul.setStock(3);
        azul.setSku("REM-AZUL");
        azul.setAttributeValueIds(List.of(11L));

        request.setVariants(List.of(rojo, azul));

        List<ProductDto> creados = productServices.createProduct(request);

        assertEquals(2, creados.size());
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product guardado = captor.getValue();
        assertEquals(2, guardado.getVariants().size());
        assertEquals(1, guardado.getAttributes().size());
        assertEquals(10L, guardado.getVariants().get(0).getAttributes().get(0).getAttributeValue().getId());
        assertEquals(11L, guardado.getVariants().get(1).getAttributes().get(0).getAttributeValue().getId());
    }

    @Test
    void createProductSinAtributosRechazaMasDeUnaVariante() {
        when(productRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));

        ProductRequestDto request = pedidoSimple("TV", "100", "200", 1);
        ProductVariantRequestDto segunda = new ProductVariantRequestDto();
        segunda.setCost(new BigDecimal("100"));
        segunda.setPrice(new BigDecimal("200"));
        segunda.setStock(1);
        segunda.setSku("TV-2");
        request.setVariants(List.of(request.getVariants().get(0), segunda));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.createProduct(request));
        assertTrue(error.getMessage().contains("sin atributos"));
    }

    // ------------------------------------------------------------------
    // Edición
    // ------------------------------------------------------------------

    @Test
    void updateProductActualizaDatosYVariante() {
        ProductVariant variant = variante(5L, "TV-1", "100", "200", 4);
        Product product = productoConVariante(5L, "TV", variant);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndBusinessIdAndIdNot("TV LED", NEGOCIO_ID, 5L)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(businessRepository.getReferenceById(NEGOCIO_ID)).thenReturn(new Business("Mi negocio", "ars"));

        ProductRequestDto request = pedidoSimple("TV LED", "120", "250", 9);
        request.getVariants().get(0).setId(5L);

        assertTrue(productServices.updateProduct(5L, request));

        assertEquals("TV LED", product.getName());
        assertEquals(9, variant.getStock());
        assertEquals(0, new BigDecimal("250").compareTo(variant.getPrice()));
    }

    @Test
    void updateProductRechazaCambiarAUnaCategoriaQueAgrupaSubcategorias() {
        ProductVariant variant = variante(5L, "TV-1", "100", "200", 4);
        Product product = productoConVariante(5L, "TV", variant);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndBusinessIdAndIdNot("TV", NEGOCIO_ID, 5L)).thenReturn(false);
        when(productTypeRepository.findById(2L)).thenReturn(Optional.of(categoria(2L)));
        when(productTypeRepository.existsByParentId(2L)).thenReturn(true);

        ProductRequestDto request = pedidoSimple("TV", "100", "200", 4);
        request.setProductTypeId(2L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.updateProduct(5L, request));
        assertTrue(error.getMessage().contains("subcategoría"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProductPermiteMantenerLaCategoriaPadreYaAsignada() {
        ProductVariant variant = variante(5L, "TV-1", "100", "200", 4);
        Product product = productoConVariante(5L, "TV", variant);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndBusinessIdAndIdNot("TV LED", NEGOCIO_ID, 5L)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(productTypeRepository.existsByParentId(1L)).thenReturn(true);
        when(businessRepository.getReferenceById(NEGOCIO_ID)).thenReturn(new Business("Mi negocio", "ars"));

        ProductRequestDto request = pedidoSimple("TV LED", "120", "250", 9);
        request.getVariants().get(0).setId(5L);

        assertTrue(productServices.updateProduct(5L, request));
        verify(productRepository).save(product);
    }

    @Test
    void updateProductRechazaCambiarLaMoneda() {
        ProductVariant variant = variante(5L, "TV-1", "100", "200", 4);
        Product product = productoConVariante(5L, "TV", variant);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        ProductRequestDto request = pedidoSimple("TV", "100", "200", 4);
        request.setCurrency("USD");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.updateProduct(5L, request));
        assertTrue(error.getMessage().contains("ARS"));
    }

    @Test
    void updateProductRechazaOtroNombreDelNegocio() {
        ProductVariant variant = variante(5L, "TV-1", "100", "200", 4);
        Product product = productoConVariante(5L, "TV", variant);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndBusinessIdAndIdNot("TV LED", NEGOCIO_ID, 5L)).thenReturn(true);

        ProductRequestDto request = pedidoSimple("TV LED", "100", "200", 4);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.updateProduct(5L, request));
        assertTrue(error.getMessage().contains("nombre"));
    }

    @Test
    void updateProductRechazaQuitarUnaVarianteConVentas() {
        ProductVariant variant = variante(5L, "TV-1", "100", "200", 4);
        Product product = productoConVariante(5L, "TV", variant);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndBusinessIdAndIdNot("TV", NEGOCIO_ID, 5L)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(saleRepository.existsByVariantId(5L)).thenReturn(true);

        ProductRequestDto request = pedidoSimple("TV", "100", "200", 4);
        ProductVariantRequestDto nueva = new ProductVariantRequestDto();
        nueva.setCost(new BigDecimal("100"));
        nueva.setPrice(new BigDecimal("200"));
        nueva.setStock(1);
        nueva.setSku("TV-2");
        request.setVariants(List.of(nueva));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.updateProduct(5L, request));
        assertTrue(error.getMessage().contains("ventas"));
    }

    @Test
    void updateProductRechazaCambiarLosAtributos() {
        ProductVariant variant = variante(5L, "TV-1", "100", "200", 4);
        Product product = productoConVariante(5L, "TV", variant);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndBusinessIdAndIdNot("TV", NEGOCIO_ID, 5L)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));

        ProductRequestDto request = pedidoSimple("TV", "100", "200", 4);
        request.setAttributeIds(List.of(1L));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.updateProduct(5L, request));
        assertTrue(error.getMessage().contains("atributos"));
    }

    @Test
    void updateProductReutilizaLosVinculosDeAtributosExistentes() {
        Attribute atributo = atributo(1L, "Ram", 1L, 2L);
        ProductVariant variant = variante(5L, "TV-1", "100", "200", 4);
        Product product = productoConVariante(5L, "TV", variant);
        product.getAttributes().add(vinculoProductoAtributo(product, atributo));
        VariantAttribute vinculo = vinculoVariante(variant, atributo.getValues().get(0));
        variant.getAttributes().add(vinculo);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndBusinessIdAndIdNot("TV", NEGOCIO_ID, 5L)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(atributo));

        ProductRequestDto request = pedidoSimple("TV", "100", "200", 4);
        request.getVariants().get(0).setId(5L);
        request.getVariants().get(0).setAttributeValueIds(List.of(1L));
        request.setAttributeIds(List.of(1L));
        request.setActive(false);

        assertTrue(productServices.updateProduct(5L, request));

        assertFalse(product.getActive());
        assertEquals(1, variant.getAttributes().size());
        // Se conserva el vínculo guardado: recrearlo rompía la clave única
        assertSame(vinculo, variant.getAttributes().get(0));
    }

    @Test
    void updateProductCambiaUnValorDeAtributoSoloEnElVinculoAfectado() {
        Attribute atributo = atributo(1L, "Ram", 1L, 2L);
        ProductVariant variant = variante(5L, "TV-1", "100", "200", 4);
        Product product = productoConVariante(5L, "TV", variant);
        product.getAttributes().add(vinculoProductoAtributo(product, atributo));
        VariantAttribute viejo = vinculoVariante(variant, atributo.getValues().get(0));
        variant.getAttributes().add(viejo);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndBusinessIdAndIdNot("TV", NEGOCIO_ID, 5L)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria(1L)));
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(atributo));

        ProductRequestDto request = pedidoSimple("TV", "100", "200", 4);
        request.getVariants().get(0).setId(5L);
        request.getVariants().get(0).setAttributeValueIds(List.of(2L));
        request.setAttributeIds(List.of(1L));

        assertTrue(productServices.updateProduct(5L, request));

        assertEquals(1, variant.getAttributes().size());
        assertNotSame(viejo, variant.getAttributes().get(0));
        assertEquals(2L, variant.getAttributes().get(0).getAttributeValue().getId());
    }

    private ProductAttribute vinculoProductoAtributo(Product product, Attribute atributo) {
        ProductAttribute pa = new ProductAttribute();
        pa.setProduct(product);
        pa.setAttribute(atributo);
        return pa;
    }

    private VariantAttribute vinculoVariante(ProductVariant variant, AttributeValue valor) {
        VariantAttribute va = new VariantAttribute();
        va.setVariant(variant);
        va.setAttributeValue(valor);
        return va;
    }

    @Test
    void updateProductDeOtroNegocioEsRechazado() {
        ProductVariant variant = variante(5L, "TV-1", "100", "200", 4);
        Product product = productoConVariante(5L, "TV", variant);
        Business ajeno = new Business();
        ajeno.setId(999L);
        product.setBusiness(ajeno);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class,
                () -> productServices.updateProduct(5L, pedidoSimple("TV", "100", "200", 4)));
    }

    // ------------------------------------------------------------------
    // Borrado y listado
    // ------------------------------------------------------------------

    @Test
    void deleteProductEliminaSiNoTieneVentas() {
        ProductVariant variant = variante(5L, "TV-1", "100", "200", 4);
        Product product = productoConVariante(5L, "TV", variant);
        product.setBusiness(new Business());
        product.getBusiness().setId(NEGOCIO_ID);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(saleRepository.existsByVariantId(5L)).thenReturn(false);

        assertTrue(productServices.deleteProduct(5L));
        verify(productRepository).delete(product);
    }

    @Test
    void deleteProductRechazaSiTieneVentas() {
        ProductVariant variant = variante(5L, "TV-1", "100", "200", 4);
        Product product = productoConVariante(5L, "TV", variant);
        product.setBusiness(new Business());
        product.getBusiness().setId(NEGOCIO_ID);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(saleRepository.existsByVariantId(5L)).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productServices.deleteProduct(5L));
        assertTrue(error.getMessage().contains("desactivalo"));
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void deleteProductDeOtroNegocioEsRechazado() {
        ProductVariant variant = variante(5L, "TV-1", "100", "200", 4);
        Product product = productoConVariante(5L, "TV", variant);
        Business ajeno = new Business();
        ajeno.setId(999L);
        product.setBusiness(ajeno);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () -> productServices.deleteProduct(5L));
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void getAllProductsDevuelveUnaFilaPorVariante() {
        ProductVariant rojo = variante(1L, "REM-ROJO", "100", "200", 2);
        ProductVariant azul = variante(2L, "REM-AZUL", "100", "200", 0);
        Product product = productoConVariante(5L, "Remera", rojo);
        product.setVariants(new ArrayList<>(List.of(rojo, azul)));
        azul.setProduct(product);
        azul.setBusiness(product.getBusiness());
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductDto> productos = productServices.getAllProducts();

        assertEquals(2, productos.size());
        assertEquals(5L, productos.get(0).getId());
        assertEquals(1L, productos.get(0).getVariantId());
        assertEquals("ARS", productos.get(0).getCurrency());
        assertEquals(1L, productos.get(0).getProductTypeId());
        assertEquals("TV", productos.get(0).getProductTypeName());
    }

    @Test
    void getAllProductsDistingueElEstadoDelProductoYDeLaVariante() {
        ProductVariant activa = variante(1L, "REM-ROJO", "100", "200", 2);
        Product inactivo = productoConVariante(5L, "Remera", activa);
        inactivo.setActive(false);
        when(productRepository.findAll()).thenReturn(List.of(inactivo));

        // Producto inactivo con la variante activa: la fila no se vende, pero el
        // estado propio de la variante se conserva para el formulario de edicion.
        ProductDto fila = productServices.getAllProducts().get(0);
        assertFalse(fila.getActive());
        assertFalse(fila.getProductActive());
        assertTrue(fila.getVariantActive());

        // Caso inverso: producto activo con la variante desactivada.
        inactivo.setActive(true);
        activa.setActive(false);

        ProductDto otra = productServices.getAllProducts().get(0);
        assertFalse(otra.getActive());
        assertTrue(otra.getProductActive());
        assertFalse(otra.getVariantActive());
    }

    @Test
    void deleteProductInexistenteDevuelveFalse() {
        when(productRepository.findById(5L)).thenReturn(Optional.empty());
        assertFalse(productServices.deleteProduct(5L));
    }
}
