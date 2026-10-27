package com.app.digital.payments.digital_pyments.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.dtos.SellerMetricsDto;
import com.app.digital.payments.digital_pyments.services.iservices.ISellerMetricServices;
import com.app.digital.payments.digital_pyments.utils.ApiResponse;

// Métricas del negocio por vendedor (página "Vendedores").
@RestController
@RequestMapping("/api/sellers")
@PreAuthorize("hasRole('" + Role.ROLE_USER + "')")
public class SellerController {

    private final ISellerMetricServices sellerMetricServices;

    SellerController(ISellerMetricServices sellerMetricServices) {
        this.sellerMetricServices = sellerMetricServices;
    }

    // Métricas por vendedor del período (mes/año): cartera actual, operaciones,
    // vendido, cobrado y deuda, todo por moneda y separado VENTA/PRESTAMO.
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse> getSellerMetrics(
            @RequestParam int year,
            @RequestParam int month) {
        SellerMetricsDto metrics = sellerMetricServices.getSellerMetrics(year, month);
        return ResponseEntity.ok()
                .body(new ApiResponse("Métricas de vendedores obtenidas correctamente", HttpStatus.OK, metrics));
    }
}
