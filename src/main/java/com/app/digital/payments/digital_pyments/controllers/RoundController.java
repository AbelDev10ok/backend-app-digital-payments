package com.app.digital.payments.digital_pyments.controllers;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.dtos.CollectionRoundDto;
import com.app.digital.payments.digital_pyments.models.dtos.RoundVisitRequestDto;
import com.app.digital.payments.digital_pyments.services.iservices.IRoundServices;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

/**
 * Ronda de cobro del dia: la hoja de ruta que arma el job diario (o la primera
 * consulta) y se consulta desde la app.
 */
@RestController
@RequestMapping("/api/loans/round")
public class RoundController {

    private final IRoundServices roundServices;

    public RoundController(IRoundServices roundServices) {
        this.roundServices = roundServices;
    }

    @GetMapping("/today")
    public ResponseEntity<CollectionRoundDto> hoy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(roundServices.getRondaDelDia(TenantContext.requireBusinessId(), date));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CollectionRoundDto> marcarVisita(@PathVariable Long itemId,
            @RequestBody(required = false) RoundVisitRequestDto request) {
        Boolean noEstaba = request != null ? request.getNoEstaba() : null;
        String note = request != null ? request.getNote() : null;
        return ResponseEntity.ok(
                roundServices.marcarVisita(TenantContext.requireBusinessId(), itemId, noEstaba, note));
    }
}
