package com.app.digital.payments.digital_pyments.mappers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.app.digital.payments.digital_pyments.models.Product;
import com.app.digital.payments.digital_pyments.models.ProductVariant;
import com.app.digital.payments.digital_pyments.models.VariantAttribute;
import com.app.digital.payments.digital_pyments.models.dtos.ProductDto;
import com.app.digital.payments.digital_pyments.models.dtos.VariantAttributeDto;

/**
 * Convierte un producto base + una variante en la fila vendible que consume la
 * UI (catálogo y selector de ventas).
 */
public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductDto toDto(Product product, ProductVariant variant) {
        ProductDto dto = new ProductDto();
        if (product == null || variant == null) {
            return dto;
        }

        dto.setId(product.getId());
        dto.setVariantId(variant.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setBrand(product.getBrand());
        dto.setSku(variant.getSku());
        dto.setPrice(variant.getPrice());
        dto.setCost(variant.getCost());
        dto.setStock(variant.getStock());
        dto.setBarcode(variant.getBarcode());
        dto.setActive(Boolean.TRUE.equals(variant.getActive()) && Boolean.TRUE.equals(product.getActive()));
        dto.setProductActive(product.getActive());
        dto.setVariantActive(variant.getActive());
        dto.setCurrency(product.getCurrency());

        if (product.getProductType() != null) {
            dto.setProductTypeId(product.getProductType().getId());
            dto.setProductTypeName(product.getProductType().getName());
        }

        List<VariantAttributeDto> atributos = atributosDe(variant);
        dto.setAttributes(atributos);
        dto.setVariantLabel(etiquetaDe(atributos));

        return dto;
    }

    public static List<VariantAttributeDto> atributosDe(ProductVariant variant) {
        if (variant == null || variant.getAttributes() == null) {
            return new ArrayList<>();
        }
        return variant.getAttributes().stream()
                .map(va -> new VariantAttributeDto(
                        va.getAttributeValue().getAttribute().getId(),
                        va.getAttributeValue().getAttribute().getName(),
                        va.getAttributeValue().getId(),
                        va.getAttributeValue().getValue()))
                .sorted(Comparator.comparing(VariantAttributeDto::getAttributeName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
    }

    public static String etiquetaDe(List<VariantAttributeDto> atributos) {
        if (atributos == null || atributos.isEmpty()) {
            return "";
        }
        return atributos.stream()
                .map(VariantAttributeDto::getValue)
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.joining(" / "));
    }

    public static String etiquetaDe(ProductVariant variant) {
        return etiquetaDe(atributosDe(variant));
    }
}
