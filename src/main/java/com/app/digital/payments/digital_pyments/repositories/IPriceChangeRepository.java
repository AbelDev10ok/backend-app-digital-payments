package com.app.digital.payments.digital_pyments.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.digital.payments.digital_pyments.models.PriceChange;

public interface IPriceChangeRepository extends JpaRepository<PriceChange, Long> {

    // Historial de un producto: lo más reciente primero (tope de 100 en el servicio).
    List<PriceChange> findTop100ByBusinessIdAndProductIdOrderByCreatedAtDescIdDesc(Long businessId, Long productId);
}