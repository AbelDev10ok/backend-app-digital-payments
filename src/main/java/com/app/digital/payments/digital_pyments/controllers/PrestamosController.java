package com.app.digital.payments.digital_pyments.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.dtos.SaleRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleUpdateRequestDto;
import com.app.digital.payments.digital_pyments.services.VentaServices;
import com.app.digital.payments.digital_pyments.services.VentaQueryServices;
import com.app.digital.payments.digital_pyments.utils.ValidationEntities;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/loans")
@PreAuthorize("hasRole('ROLE_USER')") // Se aplica a todos los métodos de esta clas
public class PrestamosController{

    @Autowired
    private ValidationEntities validationEntities;

    @Autowired
    private VentaServices prestamosServices;

    @Autowired
    private VentaQueryServices ventaQueryServices;

    @PostMapping
    public ResponseEntity<?> crearVenta(
        @RequestParam(defaultValue = "PRESTAMO") String productType,
        @Valid @RequestBody SaleRequestDto request, BindingResult result) throws Exception {
        if(result.hasFieldErrors() || result.hasGlobalErrors()) {
            return ResponseEntity 
                    .badRequest()
                    .body(validationEntities.validation(result));
        }
        
        SaleResponseDto sale = prestamosServices.createPrestamo(request, productType);
        return ResponseEntity.ok(sale);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?>  putVenta(
        @RequestBody SaleUpdateRequestDto entity, 
        @PathVariable Long id, 
        @RequestParam(required = false, name = "date") LocalDate date,
        @RequestParam(defaultValue = "PRESTAMO") String productType)
        throws Exception {
        prestamosServices.updatePrestamo(entity,id,date);
        return ResponseEntity.ok().body("update");
        
    }


    @PutMapping("/complete/{saleId}")
    public ResponseEntity<?> marcarVentaComoCompletado(
            @PathVariable Long saleId,
            @RequestParam Double amountPaid) {
        try {
            prestamosServices.markPrestamoAsCompleted(saleId, amountPaid);
            return ResponseEntity.ok().body("Préstamo marcado como completado si el monto cubre el saldo pendiente.");
        } catch (IllegalStateException e) {
            // Por ejemplo, si ya está completado o el monto es insuficiente
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al marcar como completado: " + e.getMessage());
        }
    }
    // Método para obtener productos o prestamos atrasados, que finalizaban hace 3 días o más.
    // Ahora recibe el tipo de producto como parámetro (por defecto "PRESTAMO")

    @GetMapping("/delayed")
    public ResponseEntity<List<SaleResponseDto>> obtenerVentasAtrasadosToFechaFinal(
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        List<SaleResponseDto> response = ventaQueryServices.findDelayedProducts(0, productType);
        return ResponseEntity.ok(response);
    }

    // Productos o prestamos que tienen cuotas a cobrar hoy, o una fecha específica, incluyendo cuotas atrasadas, la de la fecha y la próxima a cobrar.
    // Si no se proporciona una fecha, se usa la fecha actual. También recibe el tipo de producto.
    @GetMapping("/fees-to-charge-today")
    public ResponseEntity<List<SaleResponseDto>> obtenerVentasConCuotasToCobrarHoy(
            @RequestParam(required = false, name = "date") LocalDate date,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        List<SaleResponseDto> response = ventaQueryServices.getTodaysProducts(date != null ? date : LocalDate.now(), productType);
        return ResponseEntity.ok(response);
    }
    
    // Obtener ventas con cuotas atrasadas (de cualquier tipo)
    @GetMapping("/delayed-fees")
    public ResponseEntity<List<SaleResponseDto>> obtenerVentasCuotasAtrasadas(
            @RequestParam(required = false, name = "date") LocalDate date,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        List<SaleResponseDto> response = ventaQueryServices.getDelayedProductsFee(date != null ? date : LocalDate.now(), productType);
        return ResponseEntity.ok(response);
    }

    // Obtener todas las ventas
    @GetMapping
    public ResponseEntity<List<SaleResponseDto>> obtenerTodosPrestamos(
        @RequestParam(defaultValue = "PRESTAMO") String productType) throws Exception {
        return ResponseEntity.ok(ventaQueryServices.getAllVentas(productType));
    }

    // Obtener ventas de un cliente (de cualquier tipo)
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<SaleResponseDto>> obtenerVentasPorCliente(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        List<SaleResponseDto> productos = ventaQueryServices.getProductsByClientId(clientId, productType);
        return ResponseEntity.ok(productos);
    }

    // Obtener ventas no completados por cliente (de cualquier tipo)
    @GetMapping("/client/not-completed/{clientId}")
    public ResponseEntity<List<SaleResponseDto>> getVentasNotCompletedByClient(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        List<SaleResponseDto> productos = ventaQueryServices.getProductsNoCompletedByClient(clientId, productType);
        return ResponseEntity.ok(productos);
    }

    // Obtener ventas completos por cliente (de cualquier tipo)
    @GetMapping("/client/completed/{clientId}")
    public ResponseEntity<List<SaleResponseDto>> getVentasCompletedByClient(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        List<SaleResponseDto> productos = ventaQueryServices.getProductsCompletedByClient(clientId, productType);
        return ResponseEntity.ok(productos);
    }

    // Obtener cantidad de ventas no pagados por cliente (de cualquier tipo)
    @GetMapping("/client/not-completed/{clientId}/total")
    public ResponseEntity<Long> getTotalVentasNotCompletedByClient(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        Long amount = ventaQueryServices.getCantidadProductsNotCompletedByClient(clientId, productType);
        return ResponseEntity.ok(amount != null ? amount : 0L);
    }

    // obtener cantidad de ventas pagados por cliente (de cualquier tipo)
    @GetMapping("/client/completed/{clientId}/total")
    public ResponseEntity<Long> getTotalVentaCompletedByClient(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        Long amount = ventaQueryServices.getCantidadProductsCompletedByClient(clientId, productType);
        return ResponseEntity.ok(amount != null ? amount : 0L);
    }

    // obtener monto total de ventas pagados un año especifico (de cualquier tipo)
    @GetMapping("/completed/year/{year}")
    public ResponseEntity<?> getMontoTotalVentaCompletedByYear(
            @PathVariable int year,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        Double amount = ventaQueryServices.getMontoTotalProductsCompletedByYear(year, productType);
        return ResponseEntity.ok(amount != null ? amount : 0.0);
    }

    // obtener el total del valor de todas las ventas hechos un año especifico (de cualquier tipo)
    @GetMapping("/total/year/{year}")
    public ResponseEntity<?> getMontoTotalVentaByYear(
            @PathVariable int year,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        Double amount = ventaQueryServices.getMontoTotalProductsByYear(year, productType);
        return ResponseEntity.ok(amount != null ? amount : 0.0);
    }

    // obtener el monto total que vamos cobrando de todas las ventas un año especifico (de cualquier tipo)
    @GetMapping("/collect/year/{year}")
    public ResponseEntity<?> getTotalCollectVentaByYear(
            @PathVariable int year,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        Double amount = ventaQueryServices.getTotalCollectProductsByYear(year, productType);
        return ResponseEntity.ok(amount != null ? amount : 0.0);
    }

    // obtener monto total de ventas pagados un mes específico (de cualquier tipo)
    @GetMapping("/completed/year/{year}/month/{month}")
    public ResponseEntity<?> getMontoTotalVentaCompletedByMonth(
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        if(month < 1 || month > 12) {
            return ResponseEntity.badRequest().body("El mes debe estar entre 1 y 12");
        }
        Double amount = ventaQueryServices.getMontoTotalProductsCompletedByMonth(month, year, productType);
        return ResponseEntity.ok(amount != null ? amount : 0.0);
    }

    // obtener el total del valor de todas las ventas hechos de un mes específico (de cualquier tipo)
    @GetMapping("/total/year/{year}/month/{month}")
    public ResponseEntity<?> getMontoTotalVentaByMonth(
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        if(month < 1 || month > 12) {
            return ResponseEntity.badRequest().body("El mes debe estar entre 1 y 12");
        }
        Double amount = ventaQueryServices.getMontoTotalProductsByMonth(month, year, productType);
        return ResponseEntity.ok(amount != null ? amount : 0.0);
    }

    // obtener monto total que vamos cobrando de todas las ventas por mes (de cualquier tipo)
    @GetMapping("/collect/year/{year}/month/{month}")
    public ResponseEntity<?> getTotalCollectVentaByMonth(
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        if(month < 1 || month > 12) {
            return ResponseEntity.badRequest().body("El mes debe estar entre 1 y 12");
        }
        Double amount = ventaQueryServices.getTotalCollectProductsByMonth(month, year, productType);
        return ResponseEntity.ok(amount != null ? amount : 0.0);
    }

    // obtener ganancias totales de un mes específico (de cualquier tipo)
    @GetMapping("/profit/month/{month}/year/{year}")
    public ResponseEntity<?> getProfitVentaMonthYear(
            @PathVariable int month,
            @PathVariable int year,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        Double amount = ventaQueryServices.findTotalProfitOfProductsByMonthAndYear(month, year, productType);
        return ResponseEntity.ok(amount != null ? amount : 0.0);
    }

    // obtener ganancias totales de un año específico (de cualquier tipo)
    @GetMapping("/profit/year/{year}")
    public ResponseEntity<?> getProfitVentaYear(
            @PathVariable int year,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        Double amount = ventaQueryServices.findTotalProfitOfProductsByYear(year, productType);
        return ResponseEntity.ok(amount != null ? amount : 0.0);
    }

    // eliminar venta
    @DeleteMapping("/deleted/{loanId}")
    public ResponseEntity<?> deletedVentas(@PathVariable Long loanId) throws Exception{
        prestamosServices.deletePrestamo(loanId);
        return ResponseEntity.ok().body("Deleted");
    }

    // eliminar todas las ventas pagados por cliente (de cualquier tipo)
    @DeleteMapping("/completed-by-client/{clientId}")
    public ResponseEntity<?> deletedVentasCompletedByClient(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        ventaQueryServices.deleteProductsCompletedByClient(clientId, productType);
        return ResponseEntity.ok().body("Deleted");
    }

    // eliminar todas las ventas pagados de un año específico (de cualquier tipo)
    @DeleteMapping("/completed-by-year/{year}")
    public ResponseEntity<?> deletedVentasCompletedByYear(
            @PathVariable int year,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        ventaQueryServices.deleteProductsCompletedByYear(year, productType);
        return ResponseEntity.ok().body("Deleted");
    }

    // eliminar todas las ventas pagados de un mes específico (de cualquier tipo)
    @DeleteMapping("/completed-by-month-year/{year}/{month}")
    public ResponseEntity<?> deletedVentasCompletedByMonth(
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam(defaultValue = "PRESTAMO") String productType) {
        ventaQueryServices.deleteProductsCompletedByMonth(month, year, productType);
        return ResponseEntity.ok().body("Deleted");
    }
}
