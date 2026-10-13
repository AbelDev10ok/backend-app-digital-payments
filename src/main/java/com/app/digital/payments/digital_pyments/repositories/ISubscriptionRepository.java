package com.app.digital.payments.digital_pyments.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.Subscription;

import jakarta.persistence.LockModeType;

@Repository 
public interface ISubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByBusinessId(Long businessId);

    Optional<Subscription> findByMpPreapprovalId(String mpPreapprovalId);

    /**
     * Igual que findByMpPreapprovalId pero con bloqueo pesimista de fila
     * (SELECT ... FOR UPDATE). Serializa el leer-modificar-guardar cuando
     * llegan webhooks concurrentes de la misma suscripcion; el bloqueo se
     * libera al commit de la transaccion.
     */
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Subscription s WHERE s.mpPreapprovalId = :mpPreapprovalId")
Optional<Subscription> findByMpPreapprovalIdForUpdate(@Param("mpPreapprovalId") String mpPreapprovalId);
}
