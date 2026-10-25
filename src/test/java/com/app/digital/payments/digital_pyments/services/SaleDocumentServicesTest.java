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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.app.digital.payments.digital_pyments.models.Attribute;
import com.app.digital.payments.digital_pyments.models.AttributeValue;
import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.Product;
import com.app.digital.payments.digital_pyments.models.ProductVariant;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.SaleDocument;
import com.app.digital.payments.digital_pyments.models.VariantAttribute;
import com.app.digital.payments.digital_pyments.models.dtos.SaleDocumentDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleDocumentRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.DocumentType;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

class SaleDocumentServicesTest {

    @Mock
    private ISaleRepository saleRepository;

    @Mock
    private ISaleDocumentRepository documentRepository;

    @Mock
    private IBusinessRepository businessRepository;

    private SaleDocumentServices services;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        services = new SaleDocumentServices(saleRepository, documentRepository, businessRepository);
        TenantContext.setBusinessId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Business negocio() {
        Business business = new Business();
        business.setId(1L);
        business.setName("Cobros del Sur");
        business.setCuit("20123456789");
        business.setAddress("Av. Siempre Viva 123");
        business.setPhone("1144556677");
        business.setCurrency("ARS");
        return business;
    }

    private Client cliente() {
        Client client = new Client();
        client.setId(5L);
        client.setName("Juana Pérez");
        client.setDni("30111222");
        client.setDireccion("Calle Falsa 742");
        client.setTelefono("1144445555");
        return client;
    }

    private ProductVariant variante() {
        Product product = new Product();
        product.setId(3L);
        product.setName("Televisor 32 pulgadas");

        Attribute attribute = new Attribute();
        attribute.setName("Color");
        AttributeValue value = new AttributeValue();
        value.setValue("Negro");
        value.setAttribute(attribute);
        VariantAttribute variantAttribute = new VariantAttribute();
        variantAttribute.setAttributeValue(value);

        ProductVariant variant = new ProductVariant();
        variant.setId(8L);
        variant.setProduct(product);
        variant.setSku("TV-32-NEGRO");
        variant.setPrice(new BigDecimal("50000"));
        variant.setAttributes(new ArrayList<>(List.of(variantAttribute)));
        return variant;
    }

    private Sale venta(SaleKind kind, SaleStatus status) {
        Sale sale = new Sale();
        sale.setId(10L);
        sale.setBusiness(negocio());
        sale.setClient(cliente());
        sale.setVariant(variante());
        sale.setKind(kind);
        sale.setStatus(status);
        sale.setDescriptionProduct("Televisor 32 pulgadas");
        sale.setQuantity(2);
        sale.setUnitPrice(new BigDecimal("50000"));
        sale.setPriceTotal(new BigDecimal("100000"));
        sale.setCurrency("ARS");
        sale.setDateCreation(LocalDate.of(2026, 6, 1));
        sale.setFees(new ArrayList<>());
        return sale;
    }

    private void prepararEmision(Sale sale, Long ultimoNumero) {
        when(businessRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sale.getBusiness()));
        when(saleRepository.findById(10L)).thenReturn(Optional.of(sale));
        when(documentRepository.findBySaleIdAndType(10L, DocumentType.REMITO)).thenReturn(Optional.empty());
        when(documentRepository.findMaxNumber(1L, DocumentType.REMITO)).thenReturn(ultimoNumero);
        when(documentRepository.save(any(SaleDocument.class))).thenAnswer(invocation -> {
            SaleDocument document = invocation.getArgument(0);
            document.setId(99L);
            return document;
        });
    }

    @Test
    void emiteRemitoDeVentaConSnapshotCompleto() {
        Sale sale = venta(SaleKind.VENTA, SaleStatus.ACTIVE);
        prepararEmision(sale, 0L);

        SaleDocumentDto dto = services.emitirRemito(10L, "  Entregar por la tarde  ");

        assertEquals(DocumentType.REMITO, dto.getType());
        assertEquals(1L, dto.getNumber());
        assertEquals("000001", dto.getNumberLabel());
        assertNotNull(dto.getIssuedAt());
        assertEquals(10L, dto.getSaleId());
        assertEquals(LocalDate.of(2026, 6, 1), dto.getSaleDate());

        assertEquals("Cobros del Sur", dto.getBusinessName());
        assertEquals("20123456789", dto.getBusinessTaxId());
        assertEquals("Av. Siempre Viva 123", dto.getBusinessAddress());
        assertEquals("1144556677", dto.getBusinessPhone());

        assertEquals("Juana Pérez", dto.getClientName());
        assertEquals("30111222", dto.getClientDoc());
        assertEquals("Calle Falsa 742", dto.getClientAddress());
        assertEquals("1144445555", dto.getClientPhone());

        assertEquals("Televisor 32 pulgadas", dto.getProductName());
        assertEquals("Negro", dto.getVariantLabel());
        assertEquals("TV-32-NEGRO", dto.getSku());
        assertEquals(2, dto.getQuantity());
        assertEquals(0, new BigDecimal("50000").compareTo(dto.getUnitPrice()));
        assertEquals(0, new BigDecimal("100000").compareTo(dto.getSubtotal()));
        assertEquals(0, new BigDecimal("100000").compareTo(dto.getTotal()));
        assertEquals("ARS", dto.getCurrency());
        assertEquals("Entregar por la tarde", dto.getNotes());

        ArgumentCaptor<SaleDocument> captor = ArgumentCaptor.forClass(SaleDocument.class);
        verify(documentRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getNumber());
        assertEquals(DocumentType.REMITO, captor.getValue().getType());
    }

    @Test
    void laNumeracionSigueAlUltimoRemitoDelNegocio() {
        Sale sale = venta(SaleKind.VENTA, SaleStatus.ACTIVE);
        prepararEmision(sale, 41L);

        SaleDocumentDto dto = services.emitirRemito(10L, null);

        assertEquals(42L, dto.getNumber());
        assertEquals("000042", dto.getNumberLabel());
    }

    @Test
    void reimprimirDevuelveElSnapshotSinCrearOtroRemito() {
        Sale sale = venta(SaleKind.VENTA, SaleStatus.COMPLETED);
        SaleDocument existente = new SaleDocument();
        existente.setId(77L);
        existente.setSale(sale);
        existente.setBusiness(sale.getBusiness());
        existente.setType(DocumentType.REMITO);
        existente.setNumber(5L);
        existente.setBusinessName("Nombre viejo");
        existente.setClientName("Cliente viejo");
        existente.setProductName("Producto viejo");
        existente.setQuantity(1);
        existente.setUnitPrice(new BigDecimal("10"));
        existente.setTotal(new BigDecimal("10"));
        existente.setCurrency("ARS");
        existente.setSaleDate(sale.getDateCreation());
        existente.setIssuedAt(java.time.LocalDateTime.of(2026, 6, 2, 10, 30));

        when(businessRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sale.getBusiness()));
        when(saleRepository.findById(10L)).thenReturn(Optional.of(sale));
        when(documentRepository.findBySaleIdAndType(10L, DocumentType.REMITO))
                .thenReturn(Optional.of(existente));

        SaleDocumentDto dto = services.emitirRemito(10L, "otra observación");

        assertEquals(77L, dto.getId());
        assertEquals("000005", dto.getNumberLabel());
        assertEquals("Nombre viejo", dto.getBusinessName());
        assertNull(dto.getNotes());
        verify(documentRepository, never()).save(any(SaleDocument.class));
        verify(documentRepository, never()).findMaxNumber(any(), any());
    }

    @Test
    void noEmiteRemitoDeUnPrestamo() {
        Sale sale = venta(SaleKind.PRESTAMO, SaleStatus.ACTIVE);
        prepararEmision(sale, 0L);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> services.emitirRemito(10L, null));

        assertTrue(error.getMessage().contains("venta"));
        verify(documentRepository, never()).save(any(SaleDocument.class));
    }

    @Test
    void noEmiteRemitoDeUnaVentaAnulada() {
        Sale sale = venta(SaleKind.VENTA, SaleStatus.CANCELED);
        prepararEmision(sale, 0L);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> services.emitirRemito(10L, null));

        assertTrue(error.getMessage().contains("anulada"));
        verify(documentRepository, never()).save(any(SaleDocument.class));
    }

    @Test
    void obtenerRemitoDevuelveVacioSiNoSeEmitio() {
        Sale sale = venta(SaleKind.VENTA, SaleStatus.ACTIVE);
        when(saleRepository.findById(10L)).thenReturn(Optional.of(sale));
        when(documentRepository.findBySaleIdAndType(10L, DocumentType.REMITO)).thenReturn(Optional.empty());

        assertTrue(services.obtenerRemito(10L).isEmpty());
    }

    @Test
    void obtenerRemitoDeUnaVentaInexistenteDevuelve404() {
        when(saleRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> services.obtenerRemito(10L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void noEmiteRemitoDeUnaVentaInexistente() {
        when(businessRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(negocio()));
        when(saleRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> services.emitirRemito(10L, null));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(documentRepository, never()).save(any(SaleDocument.class));
    }
}
