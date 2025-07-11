package com.app.digital.payments.digital_pyments.services;

import java.time.LocalDate;
import java.util.List;

import com.app.digital.payments.digital_pyments.models.dtos.SaleOldRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;

public interface ISalesServices {
    SaleResponseDto createSale(SaleRequestDto sale) throws Exception;
    SaleResponseDto createSaleOld(SaleOldRequestDto saleDto, LocalDate date) throws Exception;
    SaleResponseDto getSaleById(Long id) throws Exception;
    void updateSale(SaleRequestDto sale, Long id) throws Exception;    
    void deleteSale(Long id) throws Exception;
    List<SaleResponseDto> getAllSales() throws Exception;
    void markSaleAsCompleted(Long saleId);
    void updateSaleGlobalDelay(Long saleId);
}
