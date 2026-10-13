package com.app.digital.payments.digital_pyments.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.dtos.CobrosHoyResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.CuotaListItemDto;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;
import com.app.digital.payments.digital_pyments.services.FeeQueryServices;
import com.app.digital.payments.digital_pyments.services.iservices.IFeeServices;
import com.app.digital.payments.digital_pyments.utils.ApiResponse;
import com.app.digital.payments.digital_pyments.utils.FeeStatus;

@RestController
@RequestMapping("/api/loans")
public class FeeController {

    private final IFeeServices feeServices;
    private final FeeQueryServices feeQueryServices;

    public FeeController(IFeeServices feeServices, FeeQueryServices feeQueryServices) {
        this.feeServices = feeServices;
        this.feeQueryServices = feeQueryServices;
    }

    /**
     * Vista global de cuotas con filtros combinables (scoped al negocio del token).
     */
    @GetMapping("/fees")
    public ResponseEntity<Page<CuotaListItemDto>> getCuotas(
            @RequestParam(required = false) FeeStatus status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long clientId,
            @PageableDefault(size = 20, sort = "expirationDate") Pageable pageable) {
        return ResponseEntity.ok(feeQueryServices.getCuotas(status, from, to, clientId, pageable));
    }

    /**
     * Agenda de cobros del dia: cuotas que vencen hoy + vencidas, con resumen.
     */
    @GetMapping("/collections/today")
    public ResponseEntity<CobrosHoyResponseDto> getCobrosDeHoy(
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(feeQueryServices.getCobrosDeHoy(date));
    }

    @PostMapping("/collects-fee/{feeId}/pay")
    public ResponseEntity<?> registerPayment(
            @PathVariable Long feeId,
            @RequestParam BigDecimal amount,
            @RequestParam(name = "date") LocalDate date,
            @RequestParam(name = "paymentMethod", required = false, defaultValue = "EFECTIVO") String paymentMethod) 
        {
                feeServices.registerPayment(feeId, amount, date, paymentMethod);
                return ResponseEntity.ok().build();

    }

    @GetMapping("/sale/{saleId}")
    public ResponseEntity<List<FeeDto>> getFeesBySale(
            @PathVariable Long saleId) {
        return ResponseEntity.ok(feeServices.getFeeBySaleId(saleId));
    }

    @PostMapping("/fee/{feeId}/postpone")
    public ResponseEntity<ApiResponse> postponeFee(
            @PathVariable Long feeId,
            @RequestParam(required = false) LocalDate newDateExpiration,
            @RequestParam(required = false) LocalDate newDatePayment,
            @RequestParam(required = false) BigDecimal amount) 
    {
            FeeDto updatedFee = feeServices.postponeFee(feeId, newDateExpiration, newDatePayment, amount);
            return ResponseEntity.ok(new ApiResponse("Cuota pospuesta correctamente", HttpStatus.OK, updatedFee));

    }


    @DeleteMapping("/fee/{feeId}")
    public ResponseEntity<ApiResponse> deleteFee(@PathVariable Long feeId) {
            feeServices.deleteFee(feeId);
            return ResponseEntity.ok().body(new ApiResponse("Cuota eliminada correctamente", HttpStatus.OK, feeId));
    }

}
