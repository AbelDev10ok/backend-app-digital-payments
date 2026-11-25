package com.app.digital.payments.digital_pyments.services.iservices;

import java.util.Collection;
import java.util.List;

import com.app.digital.payments.digital_pyments.models.dtos.PaymentPromiseDto;
import com.app.digital.payments.digital_pyments.models.dtos.PaymentPromiseRequestDto;

public interface IPaymentPromiseServices {

    /** Registra una promesa de pago del cliente en una moneda. */
    PaymentPromiseDto crear(Long clientId, PaymentPromiseRequestDto request);

    /** Cancela una promesa pendiente (el cliente no la sostiene o se renegoció). */
    PaymentPromiseDto cancelar(Long promiseId);

    /** Marca como cumplida una promesa pendiente (cobro registrado fuera del flujo). */
    PaymentPromiseDto cumplir(Long promiseId);

    /** Historial de promesas del cliente, opcionalmente acotado a una moneda. */
    List<PaymentPromiseDto> listarPorCliente(Long clientId, String currency);

    /** Promesas pendientes de los clientes de la agenda (una sola consulta). */
    List<PaymentPromiseDto> listarVigentesDeClientes(Collection<Long> clientIds);
}
