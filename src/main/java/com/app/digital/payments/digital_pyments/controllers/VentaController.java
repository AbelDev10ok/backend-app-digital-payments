package com.app.digital.payments.digital_pyments.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.dtos.DashboardStatsDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleUpdateRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SalesCountsDto;
import com.app.digital.payments.digital_pyments.services.VentaServices;
import com.app.digital.payments.digital_pyments.services.VentaQueryServices;
import com.app.digital.payments.digital_pyments.utils.ApiResponse;
import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;



@RestController
@RequestMapping("/api/sales")
@PreAuthorize("hasRole('" + Role.ROLE_USER + "')") // Se aplica a todos los métodos de esta clas
public class VentaController{

    private final  VentaQueryServices ventaQueryServices;

    private final  VentaServices ventaServices;

    VentaController(VentaServices ventaServices, VentaQueryServices ventaQueryServices) {
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

    // Conteos por estado para los badges del menú. Debe declararse ANTES de /{id}.
    @GetMapping("/counts")
    public ResponseEntity<ApiResponse> getSalesCounts(
            @RequestParam(required = false) SaleKind kind
        ) {
        SalesCountsDto counts = ventaServices.getSalesCounts(kind);
        return ResponseEntity.ok().body(new ApiResponse("Conteos obtenidos correctamente", HttpStatus.OK, counts));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> crearVenta(
        @Valid @RequestBody SaleRequestDto request, BindingResult result) throws Exception {
        
        if(result.hasFieldErrors() || result.hasGlobalErrors()) {
            return ResponseEntity 
                    .badRequest()
                    .body(new ApiResponse("Errores en la solicitud", HttpStatus.BAD_REQUEST, null));
        }
        
        // Ahora el servicio solo recibe el DTO, ya que contiene el tipo de producto
        SaleResponseDto sale = ventaServices.createVenta(request);
        return ResponseEntity.ok().body(new ApiResponse("Venta creada correctamente", HttpStatus.OK, sale));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse>  putVenta(
        @Valid
        @RequestBody SaleUpdateRequestDto entity, 
        BindingResult result,
        @PathVariable Long id
    )
    throws Exception {
        if(result.hasFieldErrors() || result.hasGlobalErrors()) {
            return ResponseEntity 
                    .badRequest()
                    .body(new ApiResponse("Errores en la solicitud", HttpStatus.BAD_REQUEST, null));
        }

        ventaServices.updateSale(entity,id);
        
        return ResponseEntity.ok().body(new ApiResponse("Venta actualizada correctamente", HttpStatus.OK, null));
        
    }

    // eliminar venta
    @DeleteMapping("/deleted/{loanId}")
    public ResponseEntity<ApiResponse> deletedVentas(@PathVariable Long loanId) throws Exception{
        ventaServices.deleteVenta(loanId);
        return ResponseEntity.ok().body(new ApiResponse("Venta eliminada correctamente", HttpStatus.OK, null));
    }

    // anular venta: conserva el registro con estado CANCELADO y repone el stock.
    // refund=true -> el monto ya cobrado se devuelve al cliente; false -> se retiene.
    @PutMapping("/cancel/{saleId}")
    public ResponseEntity<ApiResponse> cancelVenta(
            @PathVariable Long saleId,
            @RequestParam(defaultValue = "true") boolean refund) throws Exception{
        SaleResponseDto sale = ventaServices.cancelVenta(saleId, refund);
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

        // Si no se pasan year/month, usar el mes actual
        if (year == null || month == null) {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }

        Map<String, Object> stats = ventaQueryServices.getSalesStats(year, month,productTypeName);
        return ResponseEntity.ok().body(new ApiResponse("Venta obtenida correctamente", HttpStatus.OK, stats));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse> getDashboardStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String currency
        ) {

        // Si no se pasan year/month, usar el mes actual
        if (year == null || month == null) {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }

        DashboardStatsDto stats = ventaQueryServices.getDashboardStats(year, month, currency);
        return ResponseEntity.ok().body(new ApiResponse("Dashboard obtenido correctamente", HttpStatus.OK, stats));
    }
   
}
