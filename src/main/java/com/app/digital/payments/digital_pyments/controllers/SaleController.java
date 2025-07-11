package com.app.digital.payments.digital_pyments.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.dtos.SaleOldRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.services.ISalesServices;
import com.app.digital.payments.digital_pyments.services.SaleQuerysServices;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.app.digital.payments.digital_pyments.utils.ValidationEntities;


@RestController
@RequestMapping("/api/sales")
public class SaleController {


    @Autowired
    private ValidationEntities validationEntities;

    @Autowired
    private ISalesServices salesServices;

    @Autowired
    private SaleQuerysServices saleQuerysServices;

    // Retornar ventas atrasadas, decir finalizaba hace 3 dias la fecha de la ultima cuota, ahora tiene 3 dias de atraso 
    @GetMapping("/delays-sales")
    public ResponseEntity<List<SaleResponseDto>> getDelayedSales() {
            List<SaleResponseDto> response = saleQuerysServices.findDelayedSales();
            return ResponseEntity.ok(response);
    }

    // ventas que tienen cuotas a cobrar hoy, o una fecha específica, incluyendo cuotas atrasadas, la de la fecha y la próxima a cobrar.
    // Si no se proporciona una fecha, se usa la fecha actual
    @GetMapping("/fees-to-charge-today")
    public ResponseEntity<List<SaleResponseDto>> getSalesWithFeesToChargeToday(@RequestParam(required = false, name = "date") LocalDate date) {
        System.out.println("Fecha recibida: " + date);
        // Si no se proporciona una fecha, se usa la fecha actual
        List<SaleResponseDto> response = saleQuerysServices.getTodaysFees(date != null ? date : LocalDate.now());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/delayed-fees")
    public ResponseEntity<List<SaleResponseDto>> getDelayedFees(@RequestParam(required = false, name = "date") LocalDate date) {
        List<SaleResponseDto> response = saleQuerysServices.getDelayedFees(date != null ? date : LocalDate.now());
        return ResponseEntity.ok(response);
    }

    // Marcar venta como completada
    @PostMapping("/{saleId}/complete")
    public ResponseEntity<Void> markSaleAsCompleted(@PathVariable Long saleId) {
        salesServices.markSaleAsCompleted(saleId);
        return ResponseEntity.noContent().build();
    }

    // Actualizar días de atraso (útil para forzar actualización)
    @PostMapping("/{saleId}/update-delay")
    public ResponseEntity<Void> updateSaleDelay(@PathVariable Long saleId) {
        salesServices.updateSaleGlobalDelay(saleId);
        return ResponseEntity.noContent().build();
    }
    

    @PostMapping
    public ResponseEntity<?> crearVenta(@Valid @RequestBody SaleRequestDto request, BindingResult result) throws Exception {
        if(result.hasFieldErrors() || result.hasGlobalErrors()) {
            return ResponseEntity 
                    .badRequest()
                    .body(validationEntities.validation(result));
        }
        
        SaleResponseDto sale = salesServices.createSale(request);
        return ResponseEntity.ok(sale);
    }

    @PostMapping("/sale-old") 
    public ResponseEntity<?> createSaleOld(
        @Valid @RequestBody SaleOldRequestDto request,
        BindingResult result, 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) throws Exception 
        {
        if(result.hasFieldErrors() || result.hasGlobalErrors()) {
            return ResponseEntity 
                    .badRequest()
                    .body(validationEntities.validation(result));
        }
        return ResponseEntity.ok(salesServices.createSaleOld(request,date));
    }
    

    @GetMapping
    public ResponseEntity<List<SaleResponseDto>> getAllSales() throws Exception {
        return ResponseEntity.ok(salesServices.getAllSales());
    }

    @PutMapping("/{id}")
    public String updateSale(@PathVariable Long id, @RequestBody SaleRequestDto entity) throws Exception {

        salesServices.updateSale(entity, id);

        return "PUT method called with id: " + id + " and entity: " + entity.toString();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSale(@PathVariable Long id) throws Exception {
        salesServices.deleteSale(id);
        return ResponseEntity.ok("Sale deleted successfully");
    }
    
    
}

