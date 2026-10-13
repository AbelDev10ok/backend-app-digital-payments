package com.app.digital.payments.digital_pyments.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.Fee;

@Repository
public interface IFeeRepository extends JpaRepository<Fee, Long>, JpaSpecificationExecutor<Fee> {

    @Override
    @EntityGraph(attributePaths = { "sale", "sale.client" })
    Page<Fee> findAll(org.springframework.data.jpa.domain.Specification<Fee> spec, Pageable pageable);

    // Resumen (cantidad y monto efectivo) de cuotas pendientes que vencen en una fecha exacta
    @Query("SELECT COUNT(f), SUM(CASE WHEN f.amount > 0 THEN f.amount ELSE s.amountFee END) FROM Fee f JOIN f.sale s " +
            "WHERE f.paid = false AND f.expirationDate = :date")
    List<Object[]> sumPendingFeesByExpirationDate(@Param("date") LocalDate date);

    // Resumen (cantidad y monto efectivo) de cuotas pendientes vencidas antes de una fecha
    @Query("SELECT COUNT(f), SUM(CASE WHEN f.amount > 0 THEN f.amount ELSE s.amountFee END) FROM Fee f JOIN f.sale s " +
            "WHERE f.paid = false AND f.expirationDate < :date")
    List<Object[]> sumOverdueFeesBefore(@Param("date") LocalDate date);

    

    
    // Suma de paidAmount para fees pagadas en ventas del rango de fechas
    @Query("SELECT SUM(f.amount) FROM Fee f JOIN f.sale s WHERE f.paid = true AND s.dateCreation BETWEEN :startDate AND :endDate")
    Double sumPaidAmountBySaleDateCreationBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Contar fees atrasadas (no pagadas y expirationDate < hoy) en ventas del rango
    @Query("SELECT COUNT(f) FROM Fee f JOIN f.sale s WHERE f.paid = false AND f.expirationDate < :today AND s.dateCreation BETWEEN :startDate AND :endDate")
    long countOverdueFeesBySaleDateCreationBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("today") LocalDate today);

    // ------

    List<Fee> findBySaleId(Long payableItemId);

    // Si necesitas cuotas específicas de una VENTA con un ID dado:
    // (Asume que el ID de Sale es el mismo que el ID de su PayableItem base)
    // @Query("SELECT f FROM Fee f JOIN f.payableItem pi WHERE pi.id = :saleId AND TYPE(pi) = Sale")
    List<Fee> findFeesBySaleId(@Param("saleId") Long saleId);

    // Ahora, mapea al cliente del PayableItem.
    List<Fee> findBySaleClientIdAndPaid(Long clientId, Boolean paid);


    // Ahora, mapea al cliente del PayableItem.
    List<Fee> findBySaleClientId(Long clientId);

    List<Fee> findByExpirationDateAndPaid(LocalDate expirationDate, Boolean paid);
    List<Fee> findByPaidFalseAndExpirationDateBefore(LocalDate expirationDate);
    List<Fee> findByPaidFalseAndExpirationDateGreaterThan(LocalDate nnumber);
    List<Fee> findByPaidFalse();

}
