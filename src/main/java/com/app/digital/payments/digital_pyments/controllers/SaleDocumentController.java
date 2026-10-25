package com.app.digital.payments.digital_pyments.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.dtos.SaleDocumentDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleDocumentRequestDto;
import com.app.digital.payments.digital_pyments.services.SaleDocumentServices;
import com.app.digital.payments.digital_pyments.utils.ApiResponse;

import jakarta.validation.Valid;

/**
 * Documentos comerciales de una venta. Hoy: remito (emisión y reimpresión).
 * Mismos permisos que las ventas (ROLE_USER).
 */
@RestController
@RequestMapping("/api/sales/{saleId}/remito")
@PreAuthorize("hasRole('" + Role.ROLE_USER + "')")
public class SaleDocumentController {

    private final SaleDocumentServices saleDocumentServices;

    SaleDocumentController(SaleDocumentServices saleDocumentServices) {
        this.saleDocumentServices = saleDocumentServices;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> obtenerRemito(@PathVariable Long saleId) {
        SaleDocumentDto document = saleDocumentServices.obtenerRemito(saleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La venta no tiene remito"));
        return ResponseEntity.ok()
                .body(new ApiResponse("Remito obtenido correctamente", HttpStatus.OK, document));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> emitirRemito(
            @PathVariable Long saleId,
            @Valid @RequestBody(required = false) SaleDocumentRequestDto request) {
        String notes = request != null ? request.getNotes() : null;
        SaleDocumentDto document = saleDocumentServices.emitirRemito(saleId, notes);
        return ResponseEntity.ok()
                .body(new ApiResponse("Remito emitido correctamente", HttpStatus.OK, document));
    }
}
