package com.app.digital.payments.digital_pyments.services;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;

import static com.app.digital.payments.digital_pyments.mappers.SaleMapper.convertirAVentaResponseDTO;
import static com.app.digital.payments.digital_pyments.mappers.FeeMapper.convertToFeeDto;


@Service
public class VentaQueryServices {
    
    @Autowired
    private ISaleRepository saleRepository;

    // Obtener productos con cuotas a cobrar hoy, o una fecha específica, excluyendo el tipo indicado
    @Transactional(readOnly = true)
    public List<SaleResponseDto> getTodaysProducts(LocalDate date, String productType) {
        return saleRepository.findSalesWithFeesToChargeTodayAndProductTypeNot(date, productType)
                .stream()
                .map(sale -> {
                    SaleResponseDto saleResponse = convertirAVentaResponseDTO(sale);
                    return saleResponse;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SaleResponseDto> getAllVentas(String productType){
        return saleRepository.findByProductTypeNot(productType)
        .stream()
        .map(sale -> {
            SaleResponseDto saleResponseDto = convertirAVentaResponseDTO(sale);
            return saleResponseDto;
        }).toList();
    }


    @Transactional(readOnly = true)
    public List<SaleResponseDto> getDelayedProductsFee(LocalDate date, String productType) {
        return saleRepository.findSalesWithDelayedFeesAndProductTypeNot(date, productType)
                .stream()
                .map(sale -> {
                    SaleResponseDto saleResponse = convertirAVentaResponseDTO(sale);
                    List<Fee> allFees = sale.getFees().stream()
                        .sorted(Comparator.comparing(Fee::getExpirationDate))
                        .toList();
                    List<FeeDto> relevantFees = new ArrayList<>();
                    allFees.stream().filter(fee -> fee.getExpirationDate().isBefore(date))
                        .forEach(fee -> relevantFees.add(convertToFeeDto(fee)));
                    allFees.stream().filter(fee -> fee.getExpirationDate().isEqual(date))
                        .forEach(fee -> relevantFees.add(convertToFeeDto(fee)));
                    allFees.stream().filter(fee -> fee.getExpirationDate().isAfter(date) && !fee.getPaid())
                        .findFirst().ifPresent(fee -> relevantFees.add(convertToFeeDto(fee)));
                    saleResponse.setFees(relevantFees);
                    return saleResponse;
                })
                .toList();
    }

    // Método para obtener productos atrasados (más de X días)
    @Transactional
    public List<SaleResponseDto> findDelayedProducts(int daysLate, String productType) {
        updateAllPendingSalesDelays(productType);
        return saleRepository.findByDaysLateGreaterThanAndProductTypeNot(daysLate, productType).stream()
                .map(sale -> convertirAVentaResponseDTO(sale))
                .collect(Collectors.toList());
    }

    // Actualiza los atrasos de todas las ventas pendientes
    @Transactional
    public void updateAllPendingSalesDelays(String productType) {
        List<Sale> pendingSales = saleRepository.findByCompletedFalseAndProductTypeNot(productType);
        pendingSales.forEach(sale -> {
            int newDelay = LocalDate.now().isAfter(sale.getFinalPaymentDate()) ?
                (int) ChronoUnit.DAYS.between(sale.getFinalPaymentDate(), LocalDate.now()) : 0;
            if (newDelay != sale.getDaysLate()) {
                sale.setDaysLate(newDelay);
                saleRepository.save(sale);
            }
        });
    }

    // Obtener productos de un cliente
    @Transactional(readOnly = true)
    public List<SaleResponseDto> getProductsByClientId(Long clientId, String productType) {
        return saleRepository.findByClientIdAndProductTypeNotOrderByDateCreationDesc(clientId, productType).stream()
            .map(sale -> convertirAVentaResponseDTO(sale))
            .collect(Collectors.toList());
    }

    // Obtener productos de un cliente no pagados
    @Transactional(readOnly = true)
    public List<SaleResponseDto> getProductsNoCompletedByClient(Long clientId, String productType) {
        return saleRepository.findByClientIdAndProductTypeNotAndCompletedFalseOrderByDateCreationDesc(clientId, productType).stream()
            .map(sale -> convertirAVentaResponseDTO(sale))
            .collect(Collectors.toList());
    }

    // Obtener productos pagados por cliente
    @Transactional(readOnly = true)
    public List<SaleResponseDto> getProductsCompletedByClient(Long clientId, String productType){
        return saleRepository.findByClientIdAndProductTypeNotAndCompletedTrueOrderByDateCreationDesc(clientId, productType)
        .stream()
        .map(sale -> convertirAVentaResponseDTO(sale))
        .collect(Collectors.toList());
    }

    // Obtener la cantidad de productos pagados por cliente
    @Transactional(readOnly = true)
    public Long getCantidadProductsCompletedByClient(Long clientId, String productType){
        return saleRepository.countByClientIdAndCompletedTrue(clientId);
    }

    // Obtener la cantidad de productos no pagados por cliente
    @Transactional(readOnly = true)
    public Long getCantidadProductsNotCompletedByClient(Long clientId, String productType){
        return saleRepository.findByClientIdAndProductTypeNotAndCompletedFalseOrderByDateCreationDesc(clientId, productType).stream().count();
    }

    // obtener monto total de productos pagados un año especifico (de cualquier tipo)
    @Transactional(readOnly = true)
    public Double getMontoTotalProductsCompletedByYear(int year, String productType){
        return saleRepository.findTotalPaidSalesAmountByYear(year, productType).orElse(0.0);
    }

    // obtener el total de valor de todos los productos hechos un año especifico (de cualquier tipo)
    @Transactional(readOnly = true)
    public Double getMontoTotalProductsByYear(int year, String productType){
        return saleRepository.findTotalSalesAmountByYear(year, productType).orElse(0.0);
    }

    // obtener el monto total que vamos cobrando de los productos un año específico (de cualquier tipo)
    @Transactional(readOnly = true)
    public Double getTotalCollectProductsByYear(int year, String productType){
        // Debes tener un método en el repositorio para esto, si no existe, créalo.
        // Ejemplo: findTotalCollectedAmountByYear(int year, String productType)
        return saleRepository.findTotalCollectedAmountByYear(year, productType).orElse(0.0);
    }

    // obtener monto total de productos pagados un mes específico (de cualquier tipo)
    @Transactional(readOnly = true)
    public Double getMontoTotalProductsCompletedByMonth(int month, int year, String productType){
        return saleRepository.findTotalPaidSalesAmountByMonthAndYear(month, year, productType).orElse(0.0);
    }

    // obtener el total del valor de todos los productos hechos un mes específico (de cualquier tipo)
    @Transactional(readOnly = true)
    public Double getMontoTotalProductsByMonth(int month, int year, String productType){
        return saleRepository.findTotalSalesAmountByMonthAndYear(month, year, productType).orElse(0.0);
    }

    // obtener monto total que vamos cobrando de los productos un mes específico (de cualquier tipo)
    @Transactional(readOnly = true)
    public Double getTotalCollectProductsByMonth(int month, int year, String productType){
        // Debes tener este método en el repositorio ISaleRepository
        // Optional<Double> findTotalCollectedAmountByMonthAndYear(int month, int year, String productType);
        return saleRepository.findTotalCollectedAmountByMonthAndYear(month, year, productType).orElse(0.0);
    }

    // obtener ganancias totales de un mes específico (de cualquier tipo)
    @Transactional(readOnly = true)
    public Double findTotalProfitOfProductsByMonthAndYear(int month, int year, String productType){
        // Debes tener este método en el repositorio ISaleRepository
        // Optional<Double> findTotalProfitByMonthAndYear(int month, int year, String productType);
        return saleRepository.findTotalProfitByMonthAndYear(month, year, productType).orElse(0.0);
    }

    // obtener ganancias totales de un año especifico (de cualquier tipo)
    @Transactional(readOnly = true)
    public Double findTotalProfitOfProductsByYear(int year, String productType){
        // Debes tener este método en el repositorio ISaleRepository
        // Optional<Double> findTotalProfitByYear(int year, String productType);
        return saleRepository.findTotalProfitByYear(year, productType).orElse(0.0);
    }

    // eliminar todos los productos pagados de un año especifico (de cualquier tipo)
    @Transactional
    public void deleteProductsCompletedByYear(int year, String productType){
        saleRepository.deleteCompletedSalesByYear(year, productType);
    }

    // eliminar todos los productos pagados de un mes especifico (de cualquier tipo)
    @Transactional
    public void deleteProductsCompletedByMonth(int month, int year, String productType){
        saleRepository.deleteCompletedSalesByMonthAndYear(month, year, productType);
    }

    // eliminar todos los productos pagados por cliente (de cualquier tipo)
    @Transactional
    public void deleteProductsCompletedByClient(Long clientId, String productType){
        saleRepository.deleteCompletedSalesByClientId(clientId, productType);
    }

}
