package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.app.digital.payments.digital_pyments.models.Product;
import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.ProductDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

class ProductServicesTest {

    private static final Long NEGOCIO_ID = 1L;

    @Mock
    private IProductRepository productRepository;

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
        productServices = new ProductServices(productRepository, productTypeRepository, saleRepository,
                businessRepository);
        TenantContext.setBusinessId(NEGOCIO_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createProductConservaNombreYLoGuarda() {
        ProductType categoria = new ProductType();
        categoria.setId(1L);
        when(productRepository.existsByNameAndBusinessId("Heladera Samsung", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria));

        boolean creado = productServices.createProduct("  Heladera Samsung  ", new BigDecimal("500"), null, 1L);

        assertTrue(creado);
        org.mockito.ArgumentCaptor<Product> captor = org.mockito.ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals("Heladera Samsung", captor.getValue().getName());
        assertEquals(0, new BigDecimal("500").compareTo(captor.getValue().getPrice()));
        assertEquals(1L, captor.getValue().getProductType().getId());
    }

    @Test
    void createProductRechazaVacioYDuplicado() {
        assertFalse(productServices.createProduct("   ", null, null, null));

        ProductType categoria = new ProductType();
        categoria.setId(1L);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(true);
        assertFalse(productServices.createProduct("TV", null, null, 1L));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProductRechazaSinCategoriaDevuelveFalse() {
        when(productRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(false);

        boolean creado = productServices.createProduct("TV", new BigDecimal("500"), null, null);

        assertFalse(creado);
        verify(productRepository, never()).save(any(Product.class));
        verify(productTypeRepository, never()).findById(any());
    }

    @Test
    void createProductConCategoriaInexistenteDevuelveFalse() {
        when(productTypeRepository.findById(99L)).thenReturn(Optional.empty());

        boolean creado = productServices.createProduct("TV", new BigDecimal("500"), null, 99L);

        assertFalse(creado);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProductRechazaCategoriaDeOtroNegocio() {
        ProductType categoria = new ProductType();
        categoria.setId(1L);
        com.app.digital.payments.digital_pyments.models.Business otroNegocio =
                new com.app.digital.payments.digital_pyments.models.Business();
        otroNegocio.setId(999L);
        categoria.setBusiness(otroNegocio);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> productServices.createProduct("TV", null, null, 1L));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProductRechazaPrecioCeroONegativo() {
        when(productRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(false);

        assertFalse(productServices.createProduct("TV", BigDecimal.ZERO, null, 1L));
        assertFalse(productServices.createProduct("TV", new BigDecimal("-5"), null, 1L));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProductGuardaStock() {
        ProductType categoria = new ProductType();
        categoria.setId(1L);
        when(productRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria));

        boolean creado = productServices.createProduct("TV", new BigDecimal("500"), 5, 1L);

        assertTrue(creado);
        org.mockito.ArgumentCaptor<Product> captor = org.mockito.ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getStock());
    }

    @Test
    void createProductRechazaStockNegativo() {
        when(productRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(false);

        assertFalse(productServices.createProduct("TV", null, -1, 1L));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProductConStockNullEsSinControlDeInventario() {
        ProductType categoria = new ProductType();
        categoria.setId(1L);
        when(productRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(false);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria));

        boolean creado = productServices.createProduct("TV", null, null, 1L);

        assertTrue(creado);
        org.mockito.ArgumentCaptor<Product> captor = org.mockito.ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals(null, captor.getValue().getStock());
    }

    @Test
    void deleteProductPermiteSiNoEstaEnUso() {
        Product product = new Product();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(saleRepository.findByProductIdAndStatusNot(eq(1L), eq(SaleStatus.CANCELED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        boolean eliminado = productServices.deleteProduct(1L);

        assertTrue(eliminado);
        verify(productRepository).delete(product);
    }

    @Test
    void deleteProductProtegeProductoEnUso() {
        Product product = new Product();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(saleRepository.findByProductIdAndStatusNot(eq(1L), eq(SaleStatus.CANCELED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(new Sale())));

        boolean eliminado = productServices.deleteProduct(1L);

        assertFalse(eliminado);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void deleteProductInexistenteDevuelveFalse() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertFalse(productServices.deleteProduct(99L));
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void updateProductCambiaNombrePrecioYCategoria() {
        Product product = new Product();
        product.setId(1L);
        ProductType viejaCategoria = new ProductType();
        viejaCategoria.setId(2L);
        product.setProductType(viejaCategoria);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndBusinessIdAndIdNot("Heladera Samsung", NEGOCIO_ID, 1L))
                .thenReturn(false);
        ProductType nuevaCategoria = new ProductType();
        nuevaCategoria.setId(3L);
        when(productTypeRepository.findById(3L)).thenReturn(Optional.of(nuevaCategoria));

        boolean actualizado = productServices.updateProduct(1L, "  Heladera Samsung  ", new BigDecimal("600"), null, 3L);

        assertTrue(actualizado);
        assertEquals("Heladera Samsung", product.getName());
        assertEquals(0, new BigDecimal("600").compareTo(product.getPrice()));
        assertEquals(3L, product.getProductType().getId());
    }

    @Test
    void updateProductRechazaSinCategoria() {
        Product product = new Product();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndBusinessIdAndIdNot("TV", NEGOCIO_ID, 1L)).thenReturn(false);

        boolean actualizado = productServices.updateProduct(1L, "TV", null, null, null);

        assertFalse(actualizado);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProductRechazaNombreDuplicado() {
        Product product = new Product();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndBusinessIdAndIdNot("TV", NEGOCIO_ID, 1L)).thenReturn(true);

        boolean actualizado = productServices.updateProduct(1L, "TV", null, null, 1L);

        assertFalse(actualizado);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProductRechazaPrecioCeroONegativo() {
        Product product = new Product();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndBusinessIdAndIdNot("TV", NEGOCIO_ID, 1L)).thenReturn(false);

        assertFalse(productServices.updateProduct(1L, "TV", BigDecimal.ZERO, null, 1L));
        assertFalse(productServices.updateProduct(1L, "TV", new BigDecimal("-5"), null, 1L));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProductGuardaStock() {
        Product product = new Product();
        product.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndBusinessIdAndIdNot("TV", NEGOCIO_ID, 1L)).thenReturn(false);
        ProductType nuevaCategoria = new ProductType();
        nuevaCategoria.setId(3L);
        when(productTypeRepository.findById(3L)).thenReturn(Optional.of(nuevaCategoria));

        boolean actualizado = productServices.updateProduct(1L, "TV", null, 7, 3L);

        assertTrue(actualizado);
        assertEquals(7, product.getStock());
    }

    @Test
    void updateProductRechazaStockNegativo() {
        Product product = new Product();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndBusinessIdAndIdNot("TV", NEGOCIO_ID, 1L)).thenReturn(false);

        assertFalse(productServices.updateProduct(1L, "TV", null, -1, 1L));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProductInexistenteDevuelveFalse() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertFalse(productServices.updateProduct(99L, "TV", null, null, 1L));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProductRechazaCategoriaDeOtroNegocio() {
        Product product = new Product();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndBusinessIdAndIdNot("TV", NEGOCIO_ID, 1L)).thenReturn(false);
        ProductType categoria = new ProductType();
        categoria.setId(1L);
        com.app.digital.payments.digital_pyments.models.Business otroNegocio =
                new com.app.digital.payments.digital_pyments.models.Business();
        otroNegocio.setId(999L);
        categoria.setBusiness(otroNegocio);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> productServices.updateProduct(1L, "TV", null, null, 1L));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getAllProductsDevuelveDtos() {
        Product product = new Product();
        product.setId(1L);
        product.setName("TV");
        product.setPrice(new BigDecimal("500"));
        product.setStock(4);
        ProductType categoria = new ProductType();
        categoria.setId(1L);
        categoria.setName("ELECTRODOMESTICO");
        product.setProductType(categoria);
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductDto> result = productServices.getAllProducts();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("TV", result.get(0).getName());
        assertEquals("ELECTRODOMESTICO", result.get(0).getProductTypeName());
        assertEquals(0, new BigDecimal("500").compareTo(result.get(0).getPrice()));
        assertEquals(4, result.get(0).getStock());
    }
}