package com.app.digital.payments.digital_pyments.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.digital.payments.digital_pyments.models.Business;

public interface IBusinessRepository extends JpaRepository<Business, Long> {
}
