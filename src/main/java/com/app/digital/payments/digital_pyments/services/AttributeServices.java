package com.app.digital.payments.digital_pyments.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Attribute;
import com.app.digital.payments.digital_pyments.models.AttributeValue;
import com.app.digital.payments.digital_pyments.models.dtos.AttributeDto;
import com.app.digital.payments.digital_pyments.models.dtos.AttributeValueDto;
import com.app.digital.payments.digital_pyments.repositories.IAttributeRepository;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductAttributeRepository;
import com.app.digital.payments.digital_pyments.repositories.IVariantAttributeRepository;
import com.app.digital.payments.digital_pyments.utils.TenantContext;
import com.app.digital.payments.digital_pyments.utils.TenantGuard;

/**
 * Atributos del catálogo por negocio ("Color", "Talle") con sus valores.
 * Se usan para generar variantes; no se borran si un producto los usa.
 */
@Service
public class AttributeServices {

    private final IAttributeRepository attributeRepository;
    private final IProductAttributeRepository productAttributeRepository;
    private final IVariantAttributeRepository variantAttributeRepository;
    private final IBusinessRepository businessRepository;

    public AttributeServices(IAttributeRepository attributeRepository,
            IProductAttributeRepository productAttributeRepository,
            IVariantAttributeRepository variantAttributeRepository,
            IBusinessRepository businessRepository) {
        this.attributeRepository = attributeRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.variantAttributeRepository = variantAttributeRepository;
        this.businessRepository = businessRepository;
    }

    @Transactional(readOnly = true)
    public List<AttributeDto> getAll() {
        return attributeRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public AttributeDto create(AttributeDto request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del atributo es obligatorio");
        }
        String nombre = request.getName().trim();
        Long negocioId = TenantContext.requireBusinessId();
        if (attributeRepository.existsByNameAndBusinessId(nombre, negocioId)) {
            throw new IllegalArgumentException("Ya existe un atributo con ese nombre en el negocio");
        }

        List<String> valores = valoresUnicos(request.getValues());
        if (valores.isEmpty()) {
            throw new IllegalArgumentException("El atributo debe tener al menos un valor");
        }

        Attribute atributo = new Attribute();
        atributo.setName(nombre);
        atributo.setBusiness(businessRepository.getReferenceById(negocioId));
        for (String valor : valores) {
            AttributeValue av = new AttributeValue();
            av.setAttribute(atributo);
            av.setValue(valor);
            atributo.getValues().add(av);
        }
        attributeRepository.save(atributo);
        return toDto(atributo);
    }

    @Transactional
    public AttributeDto addValue(Long attributeId, String value) {
        Attribute atributo = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new IllegalArgumentException("El atributo indicado no existe"));
        TenantGuard.verificar(atributo.getBusiness() != null ? atributo.getBusiness().getId() : null);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El valor del atributo es obligatorio");
        }
        String limpio = value.trim();
        boolean repetido = atributo.getValues().stream()
                .anyMatch(v -> v.getValue().equalsIgnoreCase(limpio));
        if (repetido) {
            throw new IllegalArgumentException("El atributo ya tiene ese valor");
        }

        AttributeValue av = new AttributeValue();
        av.setAttribute(atributo);
        av.setValue(limpio);
        atributo.getValues().add(av);
        attributeRepository.save(atributo);
        return toDto(atributo);
    }

    @Transactional
    public boolean delete(Long id) {
        Attribute atributo = attributeRepository.findById(id).orElse(null);
        if (atributo == null) {
            return false;
        }
        TenantGuard.verificar(atributo.getBusiness() != null ? atributo.getBusiness().getId() : null);

        if (productAttributeRepository.existsByAttributeId(id)) {
            throw new IllegalArgumentException(
                    "El atributo está en uso por un producto; no se puede eliminar");
        }
        boolean enUso = atributo.getValues().stream()
                .anyMatch(v -> variantAttributeRepository.existsByAttributeValueId(v.getId()));
        if (enUso) {
            throw new IllegalArgumentException(
                    "El atributo está en uso por una variante; no se puede eliminar");
        }

        attributeRepository.delete(atributo);
        return true;
    }

    private AttributeDto toDto(Attribute atributo) {
        List<AttributeValueDto> valores = new ArrayList<>();
        for (AttributeValue valor : atributo.getValues()) {
            valores.add(new AttributeValueDto(valor.getId(), valor.getValue()));
        }
        return new AttributeDto(atributo.getId(), atributo.getName(), valores);
    }

    private static List<String> valoresUnicos(List<AttributeValueDto> valores) {
        List<String> resultado = new ArrayList<>();
        if (valores == null) {
            return resultado;
        }
        for (AttributeValueDto valor : valores) {
            if (valor == null || valor.getValue() == null || valor.getValue().isBlank()) {
                continue;
            }
            String limpio = valor.getValue().trim();
            boolean repetido = resultado.stream()
                    .anyMatch(v -> v.toLowerCase(Locale.ROOT).equals(limpio.toLowerCase(Locale.ROOT)));
            if (!repetido) {
                resultado.add(limpio);
            }
        }
        return resultado;
    }
}
