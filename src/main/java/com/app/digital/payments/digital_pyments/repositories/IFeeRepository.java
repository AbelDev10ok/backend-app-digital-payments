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

    // Resumen (cantidad y monto efectivo) de cuotas pendientes que vencen en una fecha exacta.
    // Pendiente = monto de la cuota menos lo ya cobrado (pago parcial), nunca mas
    // que la deuda de la venta (ver QueryFragments.MONTO_EFECTIVO_CUOTA).
    @Query("SELECT COUNT(f), SUM(" + QueryFragments.MONTO_EFECTIVO_CUOTA + ") FROM Fee f JOIN f.sale s " +
            "WHERE f.paid = false AND f.expirationDate = :date")
    List<Object[]> sumPendingFeesByExpirationDate(@Param("date") LocalDate date);

    // Resumen (cantidad y monto efectivo) de cuotas pendientes vencidas antes de una fecha
    @Query("SELECT COUNT(f), SUM(" + QueryFragments.MONTO_EFECTIVO_CUOTA + ") FROM Fee f JOIN f.sale s " +
            "WHERE f.paid = false AND f.expirationDate < :date")
    List<Object[]> sumOverdueFeesBefore(@Param("date") LocalDate date);

    // Versiones agrupadas por moneda (nunca se suman monedas distintas).
    // Las cuotas de una venta anulada no cuentan: esa venta ya no se cobra.
    @Query("SELECT s.currency, COUNT(f), SUM(" + QueryFragments.MONTO_EFECTIVO_CUOTA + ") FROM Fee f JOIN f.sale s " +
            "WHERE f.paid = false AND f.expirationDate = :date AND s.status != 'CANCELED' GROUP BY s.currency")
    List<Object[]> sumPendingFeesByExpirationDateGrouped(@Param("date") LocalDate date);

    @Query("SELECT s.currency, COUNT(f), SUM(" + QueryFragments.MONTO_EFECTIVO_CUOTA + ") FROM Fee f JOIN f.sale s " +
            "WHERE f.paid = false AND f.expirationDate < :date AND s.status != 'CANCELED' GROUP BY s.currency")
    List<Object[]> sumOverdueFeesBeforeGrouped(@Param("date") LocalDate date);

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
