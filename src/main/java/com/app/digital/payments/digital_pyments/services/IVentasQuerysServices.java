package com.app.digital.payments.digital_pyments.services;

import java.time.LocalDate;
import java.util.List;

import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;

public interface IVentasQuerysServices {
    // prestamos a cobrar hoy o fecha especifica
    List<SaleResponseDto> getTodaysPrestamos(LocalDate date);

    // prestamos con cuotas atrasadas
    List<SaleResponseDto> getDelayedPrestamosFee(LocalDate date);

    // prestamos atrasados con fecha de finalizacion caducada
    List<SaleResponseDto> findDelayedPrestamos();

    // actualizar atraso de todas las ventas
    void updateAllPendingSalesDelays();

    // obtener prestamos de un cliente
    List<SaleResponseDto> getPrestamoByClientId(Long clientId);

    // obtener prestamos de un cliente no pagados
    List<SaleResponseDto> getPrestamosNoCompletedByClient(Long clientId);

    // obtener prestamos pagados por cliente
    List<SaleResponseDto> getPrestamosCompletedByClient(Long clientId);

    // obtener la cantidad de prestamos pagados por cliente
    Long getCantidadPrestamosCompletedByClient(Long clientId);

    // obtener la cantidad de prestamos no pagados por cliente
    Long getCantidadPrestamosNotCompletedByClient(Long clientId);

    // obtener monto total de prestamos pagados un año especifico
    public Double getMontoTotalPrestamosCompletedByYear(int year);

    Double getMontoTotalPrestamosByYear(int year);

    Double getTotalCollectPrestamosByYear(int year);

    // obtener monto total de prestamos pagados un mes especifico
    public Double getMontoTotalPrestamosCompletedByMonth(int mont, int year);

    // obtener el total del valor de todos los prestamos echos un mes especifico
    public Double getMontoTotalPrestamosByMonth(int mont, int year);

    // obtener monto total que vamos cobrando de los prestamos un mes especifico
    public Double getTotalCollectPrestamosByMonth(int mont, int year);

    Double findTotalProfitOfLoansByMonthAndYear(int month, int year);

    Double findTotalProfitOfLoansByYear(int year);

    // eliminar todos los prestamos pagados por cliente
    public void deletedPrestamosCompletedByClient(Long id);

    // eliminar todos los prestamos pagados año especifico
    public void deletPrestamosCompletedByYear(int year);

    // eliminar todos los prestamos pagados un mes especifico
    public void deletPrestamosCompletedByMonth(int month, int year);
}
