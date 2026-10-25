package com.app.digital.payments.digital_pyments.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.digital.payments.digital_pyments.models.VariantAttribute;

public interface IVariantAttributeRepository extends JpaRepository<VariantAttribute, Long> {

    boolean existsByAttributeValueId(Long attributeValueId);
}
