package com.app.digital.payments.digital_pyments.services.iservices;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.digital.payments.digital_pyments.models.dtos.SaleRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleUpdateRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SalesCountsDto;
import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;


public interface IVentaServices {
    SaleResponseDto createVenta(SaleRequestDto saleDto) throws Exception;
    SaleResponseDto getSaleById(Long id) throws Exception;
    void deleteVenta(Long id) throws Exception;
    SaleResponseDto cancelVenta(Long id, boolean refund, boolean restock, String reason) throws Exception;
    Page<SaleResponseDto> getAllVentasPaginacion(
        Pageable pageable,
        Integer year,
        Integer month,
        Integer day,
        SaleStatus status,
        Boolean aCobrar,
        String clientName,
        String descriptionProduct,
        Long productType,
        SaleKind kind,
        Payments typePayments,
        BigDecimal minAmount,
        BigDecimal maxAmount
    );
    SalesCountsDto getSalesCounts(SaleKind kind);
    void markPrestamoAsCompleted(Long saleId, BigDecimal amountPaid);
    void updateSale(SaleUpdateRequestDto sale, Long id) throws Exception;

}
