package com.app.digital.payments.digital_pyments.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.digital.payments.digital_pyments.models.ProductAttribute;

public interface IProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {

    boolean existsByAttributeId(Long attributeId);
}
