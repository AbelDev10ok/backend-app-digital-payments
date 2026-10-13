package com.app.digital.payments.digital_pyments.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.models.dtos.SubscribeResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.SubscriptionStatusDto;
import com.app.digital.payments.digital_pyments.services.SubscriptionService;
import com.app.digital.payments.digital_pyments.utils.ApiResponse;

@RestController
@RequestMapping("/api/billing")
@PreAuthorize("hasRole('" + Role.ROLE_USER + "')")
public class BillingController {

    private final SubscriptionService subscriptionService;

    public BillingController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/subscription")
    public ResponseEntity<ApiResponse> getSubscriptionStatus() {
        SubscriptionStatusDto status = subscriptionService.getSubscriptionStatus();
        return ResponseEntity.ok(new ApiResponse("Estado de suscripcion obtenido", HttpStatus.OK, status));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse> subscribe() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Usuario usuario)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Usuario no autenticado", HttpStatus.BAD_REQUEST, null));
        }
        String email = usuario.getEmail();
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Usuario no autenticado", HttpStatus.BAD_REQUEST, null));
        }
        SubscribeResponseDto result = subscriptionService.subscribe(email);
        return ResponseEntity.ok(new ApiResponse("Suscripcion iniciada", HttpStatus.OK, result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refreshStatus() {
        SubscriptionStatusDto status = subscriptionService.getSubscriptionStatus();
        return ResponseEntity.ok(new ApiResponse("Estado actualizado", HttpStatus.OK, status));
    }

    @PostMapping("/subscription/cancel")
    public ResponseEntity<ApiResponse> cancelSubscription() {
        SubscriptionStatusDto status = subscriptionService.cancelSubscription();
        return ResponseEntity.ok(new ApiResponse("Suscripcion cancelada", HttpStatus.OK, status));
    }

    @PostMapping("/subscription/pause")
    public ResponseEntity<ApiResponse> pauseSubscription() {
        SubscriptionStatusDto status = subscriptionService.pauseSubscription();
        return ResponseEntity.ok(new ApiResponse("Suscripcion pausada", HttpStatus.OK, status));
    }

    @PostMapping("/subscription/resume")
    public ResponseEntity<ApiResponse> resumeSubscription() {
        SubscriptionStatusDto status = subscriptionService.resumeSubscription();
        return ResponseEntity.ok(new ApiResponse("Suscripcion reactivada", HttpStatus.OK, status));
    }
}