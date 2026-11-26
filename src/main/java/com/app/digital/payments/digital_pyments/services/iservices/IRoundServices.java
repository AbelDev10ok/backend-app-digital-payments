package com.app.digital.payments.digital_pyments.services.iservices;

import java.time.LocalDate;

import com.app.digital.payments.digital_pyments.models.dtos.CollectionRoundDto;

/**
 * Ronda de cobro del dia: arma una sola vez el plan ordenado por riesgo y lo
 * devuelve con el estado derivado de cada parada.
 */
public interface IRoundServices {

    /**
     * Ronda del dia (la arma si no existe). Es idempotente: si ya fue armada,
     * devuelve el plan original sin reordenarlo.
     */
    CollectionRoundDto getRondaDelDia(Long businessId, LocalDate fecha);

    /** Alias explicito del armado, para el job diario. */
    CollectionRoundDto construirRonda(Long businessId, LocalDate fecha);

    /**
     * Marca la visita de una parada: si el cliente no estaba, o si se le dejo
     * una nota. Devuelve la ronda completa con el estado recalculado.
     */
    CollectionRoundDto marcarVisita(Long businessId, Long itemId, Boolean noEstaba, String note);
}
