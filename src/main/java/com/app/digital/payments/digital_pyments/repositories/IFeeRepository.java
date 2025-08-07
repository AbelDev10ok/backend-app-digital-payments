package com.app.digital.payments.digital_pyments.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.Fee;

@Repository
public interface IFeeRepository extends JpaRepository<Fee, Long> {

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
