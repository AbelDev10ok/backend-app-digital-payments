package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.ProductTypeDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

class ProductTypeServicesTest {

    private static final Long NEGOCIO_ID = 1L;

    @Mock
    private IProductTypeRepository productTypeRepository;

    @Mock
    private IProductRepository productRepository;

    @Mock
    private ISaleRepository saleRepository;

    @Mock
    private IBusinessRepository businessRepository;

    private ProductTypeServices productTypeServices;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productTypeServices = new ProductTypeServices(productTypeRepository, productRepository, saleRepository,
                businessRepository);
        TenantContext.setBusinessId(NEGOCIO_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createProductTypeNormalizaNombreYLoGuarda() {
        when(productTypeRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(false);

        boolean creado = productTypeServices.createProductType("  tv  ");

        assertTrue(creado);
        org.mockito.ArgumentCaptor<ProductType> captor = org.mockito.ArgumentCaptor.forClass(ProductType.class);
        verify(productTypeRepository).save(captor.capture());
        assertEquals("TV", captor.getValue().getName());
    }

    @Test
    void createProductTypeRechazaVacioYDuplicado() {
        assertFalse(productTypeServices.createProductType("   "));

        when(productTypeRepository.existsByNameAndBusinessId("TV", NEGOCIO_ID)).thenReturn(true);
        assertFalse(productTypeServices.createProductType("TV"));
        verify(productTypeRepository, never()).save(any(ProductType.class));
    }

    @Test
    void deleteProductTypePermiteSiNoEstaEnUso() {
        ProductType categoria = new ProductType();
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productRepository.existsByProductTypeId(1L)).thenReturn(false);
        when(saleRepository.findByProductTypeIdAndStatusNot(eq(1L), eq(SaleStatus.CANCELED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        boolean eliminado = productTypeServices.deleteProductType(1L);

        assertTrue(eliminado);
        verify(productTypeRepository).delete(categoria);
    }

    @Test
    void deleteProductTypeProtegeCategoriaConProductos() {
        ProductType categoria = new ProductType();
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productRepository.existsByProductTypeId(1L)).thenReturn(true);

        boolean eliminado = productTypeServices.deleteProductType(1L);

        assertFalse(eliminado);
        verify(productTypeRepository, never()).delete(any(ProductType.class));
        verify(saleRepository, never()).findByProductTypeIdAndStatusNot(any(), any(), any());
    }

    @Test
    void deleteProductTypeProtegeCategoriaEnUso() {
        ProductType categoria = new ProductType();
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productRepository.existsByProductTypeId(1L)).thenReturn(false);
        when(saleRepository.findByProductTypeIdAndStatusNot(eq(1L), eq(SaleStatus.CANCELED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(new Sale())));

        boolean eliminado = productTypeServices.deleteProductType(1L);

        assertFalse(eliminado);
        verify(productTypeRepository, never()).delete(any(ProductType.class));
    }

    @Test
    void deleteProductTypeInexistenteDevuelveFalse() {
        when(productTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertFalse(productTypeServices.deleteProductType(99L));
        verify(productTypeRepository, never()).delete(any(ProductType.class));
    }

    @Test
    void updateProductTypeRenombraYNormaliza() {
        ProductType productType = new ProductType();
        productType.setId(1L);
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));
        when(productTypeRepository.existsByNameAndBusinessIdAndIdNot("TELEVISORES", NEGOCIO_ID, 1L))
                .thenReturn(false);

        boolean actualizado = productTypeServices.updateProductType(1L, "  televisores  ");

        assertTrue(actualizado);
        org.mockito.ArgumentCaptor<ProductType> captor = org.mockito.ArgumentCaptor.forClass(ProductType.class);
        verify(productTypeRepository).save(captor.capture());
        assertEquals("TELEVISORES", captor.getValue().getName());
    }

    @Test
    void updateProductTypeRechazaVacioYDuplicado() {
        ProductType productType = new ProductType();
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));

        assertFalse(productTypeServices.updateProductType(1L, "   "));

        when(productTypeRepository.existsByNameAndBusinessIdAndIdNot("TV", NEGOCIO_ID, 1L)).thenReturn(true);
        assertFalse(productTypeServices.updateProductType(1L, "TV"));
        verify(productTypeRepository, never()).save(any(ProductType.class));
    }

    @Test
    void updateProductTypeInexistenteDevuelveFalse() {
        when(productTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertFalse(productTypeServices.updateProductType(99L, "TV"));
        verify(productTypeRepository, never()).save(any(ProductType.class));
    }

    @Test
    void getAllProductTypesDevuelveDtos() {
        ProductType tv = new ProductType();
        tv.setId(1L);
        tv.setName("TV");
        tv.setIcon("tv-icon");
        when(productTypeRepository.findAll()).thenReturn(List.of(tv));

        List<ProductTypeDto> result = productTypeServices.getAllProductTypes();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("TV", result.get(0).getName());
        assertEquals("tv-icon", result.get(0).getIcon());
    }
}
