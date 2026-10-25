package com.app.digital.payments.digital_pyments.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.Payment;

@Repository
public interface IPaymentRepository extends JpaRepository<Payment, Long> {

    /** Pagos de una cuota, del mas antiguo al mas nuevo (para el historial con horarios). */
    List<Payment> findByFeeIdOrderByPaidAtAscIdAsc(Long feeId);
}
