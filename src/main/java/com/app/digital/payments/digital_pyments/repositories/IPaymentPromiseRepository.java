package com.app.digital.payments.digital_pyments.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.PaymentPromise;
import com.app.digital.payments.digital_pyments.utils.PromiseStatus;

@Repository
public interface IPaymentPromiseRepository extends JpaRepository<PaymentPromise, Long> {

    List<PaymentPromise> findByClientIdOrderByPromisedDateDesc(Long clientId);

    List<PaymentPromise> findByClientIdAndCurrencyOrderByPromisedDateDesc(Long clientId, String currency);

    // Cobro por cliente: las promesas pendientes se recorren de la más vieja a la más nueva
    List<PaymentPromise> findByClientIdAndCurrencyAndStatusOrderByPromisedDateAsc(
            Long clientId, String currency, PromiseStatus status);

    // Agenda de cobros: promesas pendientes de todos los clientes listados, en una sola consulta
    List<PaymentPromise> findByClientIdInAndStatusOrderByPromisedDateAsc(
            Collection<Long> clientIds, PromiseStatus status);
}
