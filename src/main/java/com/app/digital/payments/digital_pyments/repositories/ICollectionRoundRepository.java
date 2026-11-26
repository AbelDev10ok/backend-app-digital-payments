package com.app.digital.payments.digital_pyments.repositories;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.CollectionRound;

@Repository
public interface ICollectionRoundRepository extends JpaRepository<CollectionRound, Long> {

    /** Ronda del dia con sus paradas ya cargadas (una sola consulta). */
    @EntityGraph(attributePaths = "items")
    Optional<CollectionRound> findByBusinessIdAndRoundDate(Long businessId, LocalDate roundDate);

    boolean existsByBusinessIdAndRoundDate(Long businessId, LocalDate roundDate);
}
