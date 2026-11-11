package com.app.digital.payments.digital_pyments.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.digital.payments.digital_pyments.models.ProductVariant;

public interface IProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findAllByProductIdOrderByIdAsc(Long productId);

    // Ajuste masivo de precios: siempre scopeado al negocio, nunca findAll().
    List<ProductVariant> findAllByBusinessId(Long businessId);

    List<ProductVariant> findAllByBusinessIdAndProductProductTypeIdIn(Long businessId,
            Collection<Long> productTypeIds);

    List<ProductVariant> findAllByBusinessIdAndIdIn(Long businessId, Collection<Long> ids);

    Optional<ProductVariant> findByBusinessIdAndSku(Long businessId, String sku);

    boolean existsByBusinessIdAndSku(Long businessId, String sku);

    boolean existsByBusinessIdAndSkuAndIdNot(Long businessId, String sku, Long id);

    boolean existsByBusinessIdAndBarcode(Long businessId, String barcode);

    boolean existsByBusinessIdAndBarcodeAndIdNot(Long businessId, String barcode, Long id);
}
