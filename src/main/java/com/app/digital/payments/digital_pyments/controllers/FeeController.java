package com.app.digital.payments.digital_pyments.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;
import com.app.digital.payments.digital_pyments.services.IFeeServices;

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

    @GetMapping("/sale/{saleId}")
    public ResponseEntity<List<FeeDto>> getFeesBySale(
            @PathVariable Long saleId) {
        return ResponseEntity.ok(feeServices.getFeeBySaleId(saleId));
    }

    @PostMapping("/{feeId}/postpone")
    public ResponseEntity<FeeDto> postponeFee(
            @PathVariable Long feeId,
            @RequestParam String newDate) {
        LocalDate date = LocalDate.parse(newDate);
        FeeDto updatedFee = feeServices.postponeFee(feeId, date);
        return ResponseEntity.ok(updatedFee);
    }

}
