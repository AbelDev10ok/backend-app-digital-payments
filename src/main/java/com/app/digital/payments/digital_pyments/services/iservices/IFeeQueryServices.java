package com.app.digital.payments.digital_pyments.services.iservices;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.digital.payments.digital_pyments.models.dtos.CobrosHoyResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.CuotaListItemDto;
import com.app.digital.payments.digital_pyments.utils.FeeStatus;

/**
 * Consultas globales sobre cuotas (scoped al negocio autenticado por el
 * tenantFilter de Hibernate).
 */
public interface IFeeQueryServices {

    /**
     * Vista global de cuotas con filtros combinables.
     *
     * @param estado   TODAY | DELAYED | UPCOMING | PENDING | PAID
     * @param desde    fecha de vencimiento desde (inclusive)
     * @param hasta    fecha de vencimiento hasta (inclusive)
     * @param clientId filtro por cliente
     */
    Page<CuotaListItemDto> getCuotas(FeeStatus estado, LocalDate desde, LocalDate hasta,
            Long clientId, Pageable pageable);

    /**
     * Agenda de cobros del dia: pendientes que vencen hoy + vencidas,
     * ordenadas por antiguedad de vencimiento.
     */
    CobrosHoyResponseDto getCobrosDeHoy(LocalDate fecha);
}
