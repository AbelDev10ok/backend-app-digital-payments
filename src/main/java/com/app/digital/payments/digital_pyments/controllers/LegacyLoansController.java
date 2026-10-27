package com.app.digital.payments.digital_pyments.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.dtos.DashboardStatsDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleUpdateRequestDto;
import com.app.digital.payments.digital_pyments.services.VentaQueryServices;
import com.app.digital.payments.digital_pyments.services.VentaServices;
import com.app.digital.payments.digital_pyments.utils.ApiResponse;
import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;

import jakarta.validation.Valid;

// Alias de compatibilidad: el endpoint canónico es /api/sales.
// Se mantiene /api/loans para no romper integraciones existentes.
@RestController
@RequestMapping("/api/loans")
@PreAuthorize("hasRole('" + Role.ROLE_USER + "')")
public class LegacyLoansController {

    private final VentaQueryServices ventaQueryServices;
    private final VentaServices ventaServices;

    LegacyLoansController(VentaServices ventaServices, VentaQueryServices ventaQueryServices) {
        this.ventaServices = ventaServices;
        this.ventaQueryServices = ventaQueryServices;
    }

    @GetMapping()
    public ResponseEntity<ApiResponse> obtenerTodosVentas(
            Pageable pageable,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) Boolean aCobrar,
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) String descriptionProduct,
            @RequestParam(required = false) Long productType,
            @RequestParam(required = false) SaleKind kind,
            @RequestParam(required = false) Payments typePayments,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount
    ) {
            Page<SaleResponseDto> response = ventaServices.getAllVentasPaginacion(pageable, year, month, day, status, aCobrar, clientName, descriptionProduct, productType, kind, typePayments, minAmount, maxAmount);
            return ResponseEntity.ok().body(new ApiResponse("Ventas obtenidas correctamente", HttpStatus.OK, response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> crearVenta(@Valid @RequestBody SaleRequestDto request) throws Exception {
        SaleResponseDto sale = ventaServices.createVenta(request);
        return ResponseEntity.ok().body(new ApiResponse("Venta creada correctamente", HttpStatus.OK, sale));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> putVenta(@Valid @RequestBody SaleUpdateRequestDto entity, @PathVariable Long id)
            throws Exception {
        ventaServices.updateSale(entity, id);
        return ResponseEntity.ok().body(new ApiResponse("Venta actualizada correctamente", HttpStatus.OK, null));
    }

    @DeleteMapping("/deleted/{loanId}")
    public ResponseEntity<ApiResponse> deletedVentas(@PathVariable Long loanId) throws Exception {
        ventaServices.deleteVenta(loanId);
        return ResponseEntity.ok().body(new ApiResponse("Venta eliminada correctamente", HttpStatus.OK, null));
    }

    // anular venta: conserva el registro con estado CANCELADO y deja la deuda en cero.
    // refund=true -> asienta la devolucion del monto ya cobrado; false -> sin devolucion.
    // restock=true -> devuelve las unidades al stock (solo venta con producto).
    // reason -> motivo opcional de la anulacion (queda en la auditoria).
    @PutMapping("/cancel/{saleId}")
    public ResponseEntity<ApiResponse> cancelVenta(
            @PathVariable Long saleId,
            @RequestParam(defaultValue = "true") boolean refund,
            @RequestParam(defaultValue = "true") boolean restock,
            @RequestParam(required = false) String reason) throws Exception {
        SaleResponseDto sale = ventaServices.cancelVenta(saleId, refund, restock, reason);
        return ResponseEntity.ok().body(new ApiResponse("Venta anulada correctamente", HttpStatus.OK, sale));
    }

    @PutMapping("/complete/{saleId}")
    public ResponseEntity<ApiResponse> marcarVentaComoCompletado(
            @PathVariable Long saleId,
            @RequestParam BigDecimal amountPaid) {
        ventaServices.markPrestamoAsCompleted(saleId, amountPaid);
        return ResponseEntity.ok().body(new ApiResponse("Préstamo marcado como completado si el monto cubre el saldo pendiente.", HttpStatus.OK, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> obtenerVentaPorId(@PathVariable Long id) throws Exception {
        SaleResponseDto venta = ventaServices.getSaleById(id);
        return ResponseEntity.ok().body(new ApiResponse("Venta obtenida correctamente", HttpStatus.OK, venta));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse> getSalesStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String productTypeName
        ) {
        if (year == null || month == null) {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }
        Map<String, Object> stats = ventaQueryServices.getSalesStats(year, month, productTypeName);
        return ResponseEntity.ok().body(new ApiResponse("Venta obtenida correctamente", HttpStatus.OK, stats));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse> getDashboardStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String currency
        ) {
        if (year == null || month == null) {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }
        DashboardStatsDto stats = ventaQueryServices.getDashboardStats(year, month, currency);
        return ResponseEntity.ok().body(new ApiResponse("Dashboard obtenido correctamente", HttpStatus.OK, stats));
    }
}
