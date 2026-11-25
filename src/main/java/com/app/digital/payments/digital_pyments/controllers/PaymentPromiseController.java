package com.app.digital.payments.digital_pyments.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.dtos.PaymentPromiseDto;
import com.app.digital.payments.digital_pyments.models.dtos.PaymentPromiseRequestDto;
import com.app.digital.payments.digital_pyments.services.iservices.IPaymentPromiseServices;
import com.app.digital.payments.digital_pyments.utils.ApiResponse;

/**
 * Promesas de pago: el cliente se compromete a pagar un monto en una fecha.
 * No registra plata; sólo documenta la gestión para la agenda de cobros.
 */
@RestController
@RequestMapping("/api/loans")
public class PaymentPromiseController {

    private final IPaymentPromiseServices promiseServices;

    public PaymentPromiseController(IPaymentPromiseServices promiseServices) {
        this.promiseServices = promiseServices;
    }

    @PostMapping("/clients/{clientId}/promises")
    public ResponseEntity<ApiResponse> crear(@PathVariable Long clientId,
            @RequestBody PaymentPromiseRequestDto request) {
        return ResponseEntity.ok(new ApiResponse("Promesa de pago registrada", HttpStatus.OK,
                promiseServices.crear(clientId, request)));
    }

    @PostMapping("/promises/{promiseId}/cancel")
    public ResponseEntity<ApiResponse> cancelar(@PathVariable Long promiseId) {
        return ResponseEntity.ok(new ApiResponse("Promesa de pago cancelada", HttpStatus.OK,
                promiseServices.cancelar(promiseId)));
    }

    @PostMapping("/promises/{promiseId}/fulfill")
    public ResponseEntity<ApiResponse> cumplir(@PathVariable Long promiseId) {
        return ResponseEntity.ok(new ApiResponse("Promesa de pago cumplida", HttpStatus.OK,
                promiseServices.cumplir(promiseId)));
    }

    @GetMapping("/clients/{clientId}/promises")
    public ResponseEntity<List<PaymentPromiseDto>> listar(@PathVariable Long clientId,
            @RequestParam(required = false) String currency) {
        return ResponseEntity.ok(promiseServices.listarPorCliente(clientId, currency));
    }
}
