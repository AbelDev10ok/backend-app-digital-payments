package com.app.digital.payments.digital_pyments.configuration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.ProductVariant;
import com.app.digital.payments.digital_pyments.repositories.IProductVariantRepository;
import com.app.digital.payments.digital_pyments.repositories.IStockMovementRepository;
import com.app.digital.payments.digital_pyments.services.StockServices;

/**
 * Backfill de inventario: las variantes creadas antes del libro de movimientos
 * reciben su carga inicial para que {@code SUM(delta) == stock} valga desde el
 * primer arranque. Es idempotente: sólo actúa sobre variantes sin movimientos.
 */
@Component
public class StockBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StockBackfillRunner.class);

    private final IStockMovementRepository movementRepository;
    private final IProductVariantRepository variantRepository;
    private final StockServices stockServices;

    public StockBackfillRunner(IStockMovementRepository movementRepository,
            IProductVariantRepository variantRepository, StockServices stockServices) {
        this.movementRepository = movementRepository;
        this.variantRepository = variantRepository;
        this.stockServices = stockServices;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Long> sinMovimientos = movementRepository.findVariantIdsWithoutMovements();
        int inicializadas = 0;
        for (Long variantId : sinMovimientos) {
            ProductVariant variante = variantRepository.findById(variantId).orElse(null);
            if (variante == null || variante.getStock() == null || variante.getStock() <= 0) {
                continue;
            }
            stockServices.registrarCargaInicialSiFalta(variante);
            inicializadas++;
        }
        if (inicializadas > 0) {
            log.info("Inventario: carga inicial registrada para {} variantes", inicializadas);
        }
    }
}
