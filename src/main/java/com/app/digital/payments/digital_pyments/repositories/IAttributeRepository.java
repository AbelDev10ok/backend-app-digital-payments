package com.app.digital.payments.digital_pyments.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.digital.payments.digital_pyments.models.Attribute;

public interface IAttributeRepository extends JpaRepository<Attribute, Long> {

    boolean existsByNameAndBusinessId(String name, Long businessId);

    boolean existsByNameAndBusinessIdAndIdNot(String name, Long businessId, Long id);
}
