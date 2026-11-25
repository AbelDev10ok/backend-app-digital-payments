package com.app.digital.payments.digital_pyments.mappers;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.app.digital.payments.digital_pyments.models.PaymentPromise;
import com.app.digital.payments.digital_pyments.models.dtos.PaymentPromiseDto;

public class PaymentPromiseMapper {

    private PaymentPromiseMapper() {
    }

    /**
     * Convierte la promesa a DTO. El estado guardado es el del negocio; la
     * condición de vencida se calcula con la fecha de referencia (hoy), así el
     * cliente ve "vencida" sin que nadie tenga que persistir ese cambio.
     */
    public static PaymentPromiseDto toDto(PaymentPromise promise, LocalDate hoy) {
        PaymentPromiseDto dto = new PaymentPromiseDto();
        dto.setId(promise.getId());
        dto.setCurrency(promise.getCurrency());
        dto.setAmount(promise.getAmount());
        dto.setPromisedDate(promise.getPromisedDate());
        dto.setStatus(promise.getStatus());
        dto.setNote(promise.getNote());
        dto.setCreatedAt(promise.getCreatedAt());
        dto.setResolvedAt(promise.getResolvedAt());

        if (promise.getClient() != null) {
            dto.setClientId(promise.getClient().getId());
            dto.setClientName(promise.getClient().getName());
        }

        boolean vencida = promise.estaVencida(hoy);
        dto.setVencida(vencida);
        if (vencida && promise.getPromisedDate() != null) {
            dto.setDiasAtraso(Math.max(ChronoUnit.DAYS.between(promise.getPromisedDate(), hoy), 0));
        }
        return dto;
    }
}
