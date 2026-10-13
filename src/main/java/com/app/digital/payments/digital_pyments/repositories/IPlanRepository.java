package com.app.digital.payments.digital_pyments.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.digital.payments.digital_pyments.models.Plan;

public interface IPlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findFirstByActiveTrueOrderById();
}