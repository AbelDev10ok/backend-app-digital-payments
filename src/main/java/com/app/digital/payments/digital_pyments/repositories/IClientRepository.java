package com.app.digital.payments.digital_pyments.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.Client;


@Repository
public interface IClientRepository extends JpaRepository<Client, Long> {
    
    
}
