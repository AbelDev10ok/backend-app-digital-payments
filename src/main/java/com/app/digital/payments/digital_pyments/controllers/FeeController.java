package com.app.digital.payments.digital_pyments.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;
import com.app.digital.payments.digital_pyments.services.IFeeServices;
import com.app.digital.payments.digital_pyments.utils.FeeStatus;

@RestController
@RequestMapping("/api/collects-fee")
public class FeeController {

    @Autowired
    private IFeeServices feeServices;

    @PostMapping("/{feeId}/pay")
    public ResponseEntity<Void> registerPayment(
            @PathVariable Long feeId,
            @RequestParam Double amount) {
        feeServices.registerPayment(feeId, amount);
        return ResponseEntity.ok().build();
        // return ResponseEntity.(feeServices.registerPaymentFee(feeId, amount));
    }

    @PostMapping("/{feeId}/postpone")
    public ResponseEntity<FeeDto> postponeFee(
            @PathVariable Long feeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate) {
        return ResponseEntity.ok(feeServices.postponeFee(feeId, newDate));
    }


     // ========== CONSULTAS POR ESTADO ==========
    
    @GetMapping
    public ResponseEntity<List<FeeDto>> getFeesByStatus(
            @RequestParam FeeStatus status,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(feeServices.getFeesByStatus(status, date != null ? date : LocalDate.now()));
    }

    // @GetMapping("/priority")
    // public ResponseEntity<List<FeeDto>> getPriorityFees() {
    //     return ResponseEntity.ok(feeServices.getPriorityFees());
    // }

    // // ========== CONSULTAS RELACIONADAS ==========
    
    @GetMapping("/sale/{saleId}")
    public ResponseEntity<List<FeeDto>> getFeesBySale(
            @PathVariable Long saleId) {
        return ResponseEntity.ok(feeServices.getFeeBySaleId(saleId));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<FeeDto>> getClientPendingFees(
            @PathVariable Long clientId) {
        return ResponseEntity.ok(feeServices.getOutstandingFeeByClientId(clientId));
    }


}
