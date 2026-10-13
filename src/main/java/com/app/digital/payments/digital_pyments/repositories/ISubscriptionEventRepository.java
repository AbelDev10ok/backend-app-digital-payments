package com.app.digital.payments.digital_pyments.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.digital.payments.digital_pyments.models.SubscriptionEvent;

public interface ISubscriptionEventRepository extends JpaRepository<SubscriptionEvent, Long> {

    Optional<SubscriptionEvent> findByMpIdAndEvent(String mpId, String event);
}