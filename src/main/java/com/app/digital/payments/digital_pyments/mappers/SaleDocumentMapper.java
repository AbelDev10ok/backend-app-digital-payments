package com.app.digital.payments.digital_pyments.mappers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import com.app.digital.payments.digital_pyments.models.SaleDocument;
import com.app.digital.payments.digital_pyments.models.dtos.RemitoResumenDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleDocumentDto;

public final class SaleDocumentMapper {

    private SaleDocumentMapper() {
    }

    public static String formatearNumero(Long number) {
        return String.format(Locale.ROOT, "%06d", number == null ? 0 : number);
    }

    public static RemitoResumenDto aResumen(SaleDocument document) {
        if (document == null) {
            return null;
        }
        return new RemitoResumenDto(
                document.getId(),
                document.getNumber(),
                formatearNumero(document.getNumber()),
                document.getIssuedAt());
    }

    public static SaleDocumentDto aDto(SaleDocument document) {
        if (document == null) {
            return null;
        }
        SaleDocumentDto dto = new SaleDocumentDto();
        dto.setId(document.getId());
        dto.setType(document.getType());
        dto.setNumber(document.getNumber());
        dto.setNumberLabel(formatearNumero(document.getNumber()));
        dto.setIssuedAt(document.getIssuedAt());
        dto.setSaleId(document.getSale() != null ? document.getSale().getId() : null);
        dto.setSaleDate(document.getSaleDate());

        dto.setBusinessName(document.getBusinessName());
        dto.setBusinessAddress(document.getBusinessAddress());
        dto.setBusinessTaxId(document.getBusinessTaxId());
        dto.setBusinessPhone(document.getBusinessPhone());

        dto.setClientName(document.getClientName());
        dto.setClientDoc(document.getClientDoc());
        dto.setClientAddress(document.getClientAddress());
        dto.setClientPhone(document.getClientPhone());

        dto.setProductName(document.getProductName());
        dto.setVariantLabel(document.getVariantLabel());
        dto.setSku(document.getSku());
        dto.setQuantity(document.getQuantity());
        dto.setUnitPrice(document.getUnitPrice());
        dto.setSubtotal(subtotal(document));
        dto.setTotal(document.getTotal());
        dto.setCurrency(document.getCurrency());
        dto.setNotes(document.getNotes());
        return dto;
    }

    private static BigDecimal subtotal(SaleDocument document) {
        if (document.getUnitPrice() == null) {
            return BigDecimal.ZERO;
        }
        int quantity = document.getQuantity() != null ? document.getQuantity() : 1;
        return document.getUnitPrice()
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
