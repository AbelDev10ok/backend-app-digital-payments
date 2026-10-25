package com.app.digital.payments.digital_pyments.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.digital.payments.digital_pyments.models.Business;

import jakarta.persistence.LockModeType;

public interface IBusinessRepository extends JpaRepository<Business, Long> {

    /**
     * Bloquea el negocio para serializar la numeración de documentos: dos
     * emisiones simultáneas no pueden obtener el mismo número correlativo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Business b WHERE b.id = :id")
    Optional<Business> findByIdForUpdate(@Param("id") Long id);
}
