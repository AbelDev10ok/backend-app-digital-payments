package com.app.digital.payments.digital_pyments.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.digital.payments.digital_pyments.models.StockMovement;

public interface IStockMovementRepository
        extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

    boolean existsByVariantId(Long variantId);

    /**
     * Suma de los deltas de una variante: tiene que coincidir con el stock
     * cacheado en {@code product_variant.stock}.
     */
    @Query("select coalesce(sum(m.delta), 0) from StockMovement m "
            + "where m.businessId = :businessId and m.variantId = :variantId")
    Long sumDeltaByBusinessIdAndVariantId(@Param("businessId") Long businessId,
            @Param("variantId") Long variantId);

    /**
     * Variantes que todavía no tienen ningún movimiento (datos previos al libro
     * de inventario). El backfill les crea su carga inicial.
     */
    @Query(value = "select v.id from product_variant v "
            + "where not exists (select 1 from stock_movement m where m.variant_id = v.id)", nativeQuery = true)
    List<Long> findVariantIdsWithoutMovements();
}
