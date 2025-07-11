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
public class SaleQuerysServices {


    @Autowired
    private ISaleRepository saleRepository;

    // Método para obtener las ventas con cuotas a cobrar hoy, o una fecha específica,
    // incluyendo cuotas atrasadas, la de la fecha y la próxima a cobrar.
    @Transactional(readOnly = true)
    public List<SaleResponseDto> getTodaysFees(LocalDate date) {

        return saleRepository.findSalesWithFeesToChargeToday(date)
                .stream()
                .map(sale -> {
                    SaleResponseDto saleResponse = convertirAVentaResponseDTO(sale);

                    // Ordenar todas las cuotas por fecha
                    List<Fee> allFees = sale.getFees().stream()
                        .sorted(Comparator.comparing(Fee::getExpirationDate))
                        .toList();

                    List<FeeDto> relevantFees = new ArrayList<>();

                    // 1. Todas las cuotas anteriores a la fecha (pagadas o no)
                    allFees.stream()
                        .filter(fee -> fee.getExpirationDate().isBefore(date))
                        .forEach(fee -> relevantFees.add(convertToFeeDto(fee)));

                    // 2. Cuota de la fecha buscada (si existe)
                    allFees.stream()
                        .filter(fee -> fee.getExpirationDate().isEqual(date))
                        .forEach(fee -> relevantFees.add(convertToFeeDto(fee)));

                    // 3. Próxima cuota futura NO pagada (la más cercana a la fecha buscada)
                    allFees.stream()
                        .filter(fee -> fee.getExpirationDate().isAfter(date) && !fee.getPaid())
                        .findFirst()
                        .ifPresent(fee -> relevantFees.add(convertToFeeDto(fee)));

                    saleResponse.setFees(relevantFees);
                    return saleResponse;
                })
                .toList();
    }

    // Método para obtener las ventas con cuotas atrasadas, no pagadas y vencidas antes de una fecha específica.
    @Transactional(readOnly = true)
    public List<SaleResponseDto> getDelayedFees(LocalDate date) {

        return saleRepository.findSalesWithDelayedFees(date)
                .stream()
                .map(sale -> {
                    SaleResponseDto saleResponse = convertirAVentaResponseDTO(sale);

                    // Ordenar todas las cuotas por fecha
                    List<Fee> allFees = sale.getFees().stream()
                        .sorted(Comparator.comparing(Fee::getExpirationDate))
                        .toList();

                    List<FeeDto> relevantFees = new ArrayList<>();

                    // 1. Todas las cuotas anteriores a la fecha (pagadas o no)
                    allFees.stream()
                        .filter(fee -> fee.getExpirationDate().isBefore(date))
                        .forEach(fee -> relevantFees.add(convertToFeeDto(fee)));

                    // 2. Cuota de la fecha buscada (si existe)
                    allFees.stream()
                        .filter(fee -> fee.getExpirationDate().isEqual(date))
                        .forEach(fee -> relevantFees.add(convertToFeeDto(fee)));

                    // 3. Próxima cuota futura NO pagada (la más cercana a la fecha buscada)
                    allFees.stream()
                        .filter(fee -> fee.getExpirationDate().isAfter(date) && !fee.getPaid())
                        .findFirst()
                        .ifPresent(fee -> relevantFees.add(convertToFeeDto(fee)));

                    saleResponse.setFees(relevantFees);
                    return saleResponse;
                })
                .toList();
    }


    // para reporte , consultas periodicas (utiliza updateAllPendingSalesDelays)
    @Transactional 
    public List<SaleResponseDto> findDelayedSales() {
        // 1. Actualizar atrasos
        updateAllPendingSalesDelays();
        
        // 2. Retornar ventas atrasadas, decir finalizaba hace 3 dias la fecha de la ultima cuota, ahora tiene 3 dias de atraso 
        return saleRepository.findByDaysLateGreaterThan(0).stream()
                .map(sale -> convertirAVentaResponseDTO(sale))
                .collect(Collectors.toList());
    }


    @Transactional
    public void updateAllPendingSalesDelays() {
        List<Sale> pendingSales = saleRepository.findByCompletedFalse();
        
        pendingSales.forEach(sale -> {
            int newDelay = LocalDate.now().isAfter(sale.getFinalPaymentDate()) ?
                (int) ChronoUnit.DAYS.between(sale.getFinalPaymentDate(), LocalDate.now()) : 0;
            
            if (newDelay != sale.getDaysLate()) {
                sale.setDaysLate(newDelay);
                saleRepository.save(sale);
            }
        });
    }

}
