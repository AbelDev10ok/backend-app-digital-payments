package com.app.digital.payments.digital_pyments.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.Sale;

@Repository
public interface ISaleRepository extends JpaRepository<Sale, Long>{
    Optional<Sale> findByClientId(Long clientId);
    
    // Ventas que han superado su fecha final de pago
    List<Sale> findByFinalPaymentDateBefore(LocalDate date);

    List<Sale> findByCompletedFalse();

    List<Sale> findByDaysLateGreaterThan(int daysLate);

    // puede usarse tanto para hoy como para una fecha específica
    @Query("SELECT DISTINCT s FROM Sale s JOIN s.fees f " +
       "WHERE f.expirationDate = :date AND f.paid = false")
    List<Sale> findSalesWithFeesToChargeToday(@Param("date") LocalDate date);

    // Ventas con cuotas específicas atrasadas
    @Query("SELECT DISTINCT s FROM Sale s JOIN s.fees f " +
           "WHERE f.expirationDate < :date AND f.paid = false")
    List<Sale> findSalesWithDelayedFees(@Param("date") LocalDate date);

    

} 
