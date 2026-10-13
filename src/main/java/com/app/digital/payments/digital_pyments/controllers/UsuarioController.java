package com.app.digital.payments.digital_pyments.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.dtos.ChangePasswordRequestDto;
import com.app.digital.payments.digital_pyments.services.UsuarioServices;
import com.app.digital.payments.digital_pyments.utils.ApiResponse;
import com.app.digital.payments.digital_pyments.utils.ValidationEntities;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;

import jakarta.validation.Valid;

/**
 * Operaciones sobre la cuenta del usuario autenticado.
 * Vive fuera de /auth/* a proposito: esa zona es permitAll y aqui
 * se requiere sesion valida.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioServices usuarioServices;
    private final ValidationEntities validationEntities;

    public UsuarioController(UsuarioServices usuarioServices, ValidationEntities validationEntities) {
        this.usuarioServices = usuarioServices;
        this.validationEntities = validationEntities;
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto request,
            BindingResult result,
            Authentication authentication) {
        if (result.hasFieldErrors() || result.hasGlobalErrors()) {
            return ResponseEntity
                    .badRequest()
                    .body(validationEntities.validation(result));
        }
        usuarioServices.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse(
                "Contraseña actualizada. Las demás sesiones fueron cerradas.",
                HttpStatus.OK, null));
    }
}
