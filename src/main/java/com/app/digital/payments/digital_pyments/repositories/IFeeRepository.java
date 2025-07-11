package com.app.digital.payments.digital_pyments.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.Fee;

@Repository
public interface IFeeRepository extends JpaRepository<Fee, Long> {

    List<Fee> findBySaleId(Long saleId);

    List<Fee> findBySaleClientIdAndPaid(Long clientId, Boolean paid);
    
    List<Fee> findBySaleClientId(Long clientId);
    
    // para cobros de dia de hoy
    List<Fee> findByExpirationDateAndPaid(LocalDate expirationDate, Boolean paid);
    // para verificar cuotas atrasadas 
    List<Fee> findByPaidFalseAndExpirationDateBefore(LocalDate expirationDate);
    // para obtener atrasadas
    List<Fee> findByPaidFalseAndExpirationDateGreaterThan(LocalDate nnumber);

    // @Query("SELECT f FROM Fee f WHERE " +
    //        "(f.expirationDate = :today AND f.paid = false) OR " +
    //        "(f.paid = false AND f.expirationDate < :today) OR " +
    //        "(f.postponed = true AND f.paid = false)")
    // List<Fee> findPriorityFees(@Param("today") LocalDate today);

    List<Fee> findByPaidFalse();

    
    // ------------------------------------------
    // para obtener cuotas pospuestas y no pagadas
    // List<Fee> findByPostponedTrueAndPaidFalse();
}
