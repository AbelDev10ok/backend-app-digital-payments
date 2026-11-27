package com.app.digital.payments.digital_pyments.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.dtos.StockAdjustmentRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.StockEntryRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.StockMovementDto;
import com.app.digital.payments.digital_pyments.services.StockServices;
import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.utils.StockMovementType;

/**
 * Inventario: ingresos, ajustes por conteo e historial de movimientos. Todo
 * scopeado al negocio del token.
 */
@RestController
@RequestMapping("/api/stock")
@PreAuthorize("hasRole('" + Role.ROLE_USER + "')")
public class StockController {

    private final StockServices stockServices;

    public StockController(StockServices stockServices) {
        this.stockServices = stockServices;
    }

    /** Ingreso de mercadería por lote: varias líneas en una sola operación. */
    @PostMapping("/entries")
    public ResponseEntity<List<StockMovementDto>> registrarIngreso(@RequestBody StockEntryRequestDto request) {
        return ResponseEntity.ok(stockServices.registrarIngreso(request));
    }

    /** Ajuste por conteo físico: se manda lo contado y el server calcula el delta. */
    @PostMapping("/adjustment")
    public ResponseEntity<StockMovementDto> registrarAjuste(@RequestBody StockAdjustmentRequestDto request) {
        return ResponseEntity.ok(stockServices.registrarAjuste(request));
    }

    /** Historial de movimientos (sólo lectura). */
    @GetMapping("/movements")
    public ResponseEntity<Page<StockMovementDto>> getMovimientos(
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) StockMovementType type,
            @PageableDefault(size = 50, sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(stockServices.historial(variantId, from, to, type, pageable));
    }
}
