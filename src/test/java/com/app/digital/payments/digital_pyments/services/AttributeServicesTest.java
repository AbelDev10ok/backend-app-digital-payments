package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.app.digital.payments.digital_pyments.models.dtos.AttributeDto;
import com.app.digital.payments.digital_pyments.models.dtos.AttributeValueDto;
import com.app.digital.payments.digital_pyments.repositories.IAttributeRepository;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductAttributeRepository;
import com.app.digital.payments.digital_pyments.repositories.IVariantAttributeRepository;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

class AttributeServicesTest {

    private static final Long NEGOCIO_ID = 1L;

    @Mock
    private IAttributeRepository attributeRepository;

    @Mock
    private IProductAttributeRepository productAttributeRepository;

    @Mock
    private IVariantAttributeRepository variantAttributeRepository;

    @Mock
    private IBusinessRepository businessRepository;

    private AttributeServices attributeServices;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        attributeServices = new AttributeServices(attributeRepository, productAttributeRepository,
                variantAttributeRepository, businessRepository);
        TenantContext.setBusinessId(NEGOCIO_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private AttributeDto pedido(String nombre, String... valores) {
        AttributeDto dto = new AttributeDto();
        dto.setName(nombre);
        List<AttributeValueDto> lista = new ArrayList<>();
        for (String valor : valores) {
            lista.add(new AttributeValueDto(null, valor));
        }
        dto.setValues(lista);
        return dto;
    }

    @Test
    void createGuardaAtributoConSusValores() {
        when(attributeRepository.existsByNameAndBusinessId("Color", NEGOCIO_ID)).thenReturn(false);
        when(businessRepository.getReferenceById(NEGOCIO_ID)).thenReturn(new Business());

        AttributeDto creado = attributeServices.create(pedido("  Color  ", "Rojo", "Azul", "rojo"));

        assertEquals("Color", creado.getName());
        // "rojo" es duplicado de "Rojo" (case-insensitive): no se guarda dos veces
        assertEquals(2, creado.getValues().size());

        ArgumentCaptor<Attribute> captor = ArgumentCaptor.forClass(Attribute.class);
        verify(attributeRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getValues().size());
        assertEquals("Rojo", captor.getValue().getValues().get(0).getValue());
    }

    @Test
    void createRechazaNombreVacioODuplicado() {
        assertThrows(IllegalArgumentException.class, () -> attributeServices.create(pedido("   ", "Rojo")));

        when(attributeRepository.existsByNameAndBusinessId("Color", NEGOCIO_ID)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> attributeServices.create(pedido("Color", "Rojo")));
        verify(attributeRepository, never()).save(any(Attribute.class));
    }

    @Test
    void createRechazaAtributoSinValores() {
        when(attributeRepository.existsByNameAndBusinessId("Color", NEGOCIO_ID)).thenReturn(false);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> attributeServices.create(pedido("Color")));
        assertTrue(error.getMessage().contains("al menos un valor"));
        verify(attributeRepository, never()).save(any(Attribute.class));
    }

    @Test
    void addValueAgregaUnValorNuevoYRechazaRepetido() {
        Attribute atributo = new Attribute();
        atributo.setId(1L);
        atributo.setName("Color");
        Business business = new Business();
        business.setId(NEGOCIO_ID);
        atributo.setBusiness(business);
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(atributo));

        AttributeDto actualizado = attributeServices.addValue(1L, "  Verde  ");

        assertEquals(1, actualizado.getValues().size());
        assertEquals("Verde", actualizado.getValues().get(0).getValue());

        assertThrows(IllegalArgumentException.class, () -> attributeServices.addValue(1L, "verde"));
    }

    @Test
    void deleteEliminaSiNadieLoUsa() {
        Attribute atributo = new Attribute();
        atributo.setId(1L);
        Business business = new Business();
        business.setId(NEGOCIO_ID);
        atributo.setBusiness(business);
        AttributeValue valor = new AttributeValue();
        valor.setId(10L);
        valor.setAttribute(atributo);
        atributo.setValues(new ArrayList<>(List.of(valor)));
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(atributo));

        assertTrue(attributeServices.delete(1L));
        verify(attributeRepository).delete(atributo);
    }

    @Test
    void deleteRechazaSiUnProductoLoUsa() {
        Attribute atributo = new Attribute();
        atributo.setId(1L);
        Business business = new Business();
        business.setId(NEGOCIO_ID);
        atributo.setBusiness(business);
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(atributo));
        when(productAttributeRepository.existsByAttributeId(1L)).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> attributeServices.delete(1L));
        assertTrue(error.getMessage().contains("producto"));
        verify(attributeRepository, never()).delete(any(Attribute.class));
    }

    @Test
    void deleteRechazaSiUnaVarianteUsaUnValor() {
        Attribute atributo = new Attribute();
        atributo.setId(1L);
        Business business = new Business();
        business.setId(NEGOCIO_ID);
        atributo.setBusiness(business);
        AttributeValue valor = new AttributeValue();
        valor.setId(10L);
        valor.setAttribute(atributo);
        atributo.setValues(new ArrayList<>(List.of(valor)));
        when(attributeRepository.findById(1L)).thenReturn(Optional.of(atributo));
        when(variantAttributeRepository.existsByAttributeValueId(10L)).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> attributeServices.delete(1L));
        assertTrue(error.getMessage().contains("variante"));
        verify(attributeRepository, never()).delete(any(Attribute.class));
    }

    @Test
    void getAllDevuelveLosAtributosDelNegocioConSusValores() {
        Attribute atributo = new Attribute();
        atributo.setId(1L);
        atributo.setName("Talle");
        AttributeValue valor = new AttributeValue();
        valor.setId(10L);
        valor.setAttribute(atributo);
        valor.setValue("M");
        atributo.setValues(new ArrayList<>(List.of(valor)));
        when(attributeRepository.findAll()).thenReturn(List.of(atributo));

        List<AttributeDto> resultado = attributeServices.getAll();

        assertEquals(1, resultado.size());
        assertEquals("Talle", resultado.get(0).getName());
        assertEquals("M", resultado.get(0).getValues().get(0).getValue());
    }
}
