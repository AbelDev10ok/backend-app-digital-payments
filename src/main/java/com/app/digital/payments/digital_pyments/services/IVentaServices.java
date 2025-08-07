package com.app.digital.payments.digital_pyments.services;

import java.time.LocalDate;
import java.util.List;

import com.app.digital.payments.digital_pyments.models.dtos.SaleOldRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleUpdateRequestDto;


public interface IVentaServices {
    SaleResponseDto createPrestamo(SaleRequestDto saleDto, String productType) throws Exception;
    SaleResponseDto createPrestamoOld(SaleOldRequestDto saleDto, LocalDate date) throws Exception;
    SaleResponseDto getSaleById(Long id) throws Exception;
    void deleteSale(Long id) throws Exception;
    List<SaleResponseDto> getAllSales() throws Exception;
    void markPrestamoAsCompleted(Long saleId, Double amountPaid);
    void updatePrestamo(SaleUpdateRequestDto sale, Long id, LocalDate date) throws Exception;
    void updateSaleGlobalDelay(Long saleId) ;
    List<SaleResponseDto> getAll() throws Exception;
    void deletePrestamo(Long id) throws Exception;

}
