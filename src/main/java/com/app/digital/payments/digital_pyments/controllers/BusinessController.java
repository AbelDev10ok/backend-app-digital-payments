package com.app.digital.payments.digital_pyments.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.dtos.BusinessMeDto;
import com.app.digital.payments.digital_pyments.models.dtos.BusinessUpdateRequestDto;
import com.app.digital.payments.digital_pyments.services.BusinessServices;
import com.app.digital.payments.digital_pyments.utils.ValidationEntities;

import jakarta.validation.Valid;

/**
 * Configuracion del negocio propio (tenant autenticado).
 */
@RestController
@RequestMapping("/api/business")
public class BusinessController {

    private final BusinessServices businessServices;
    private final ValidationEntities validationEntities;

    public BusinessController(BusinessServices businessServices, ValidationEntities validationEntities) {
        this.businessServices = businessServices;
        this.validationEntities = validationEntities;
    }

    @GetMapping("/me")
    public ResponseEntity<BusinessMeDto> getMyBusiness() {
        return ResponseEntity.ok(businessServices.getMyBusiness());
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyBusiness(
            @Valid @RequestBody BusinessUpdateRequestDto request,
            org.springframework.validation.BindingResult result) {
        if (result.hasFieldErrors() || result.hasGlobalErrors()) {
            return ResponseEntity
                    .badRequest()
                    .body(validationEntities.validation(result));
        }
        return ResponseEntity.ok(businessServices.updateMyBusiness(request));
    }
}
