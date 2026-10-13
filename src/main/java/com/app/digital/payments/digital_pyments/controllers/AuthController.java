package com.app.digital.payments.digital_pyments.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.models.dtos.UsuarioAuth;
import com.app.digital.payments.digital_pyments.services.UsuarioServices;
import com.app.digital.payments.digital_pyments.utils.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UsuarioServices userServices;

    public AuthController(UsuarioServices userServices) {
        this.userServices = userServices;
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse> verifyUser(@RequestParam String token) {
        try {
            Usuario usuarioVerificado = userServices.verifyUser(token);
            if (usuarioVerificado.isEnabled()) {
                return ResponseEntity.ok(new ApiResponse("¡Correo verificado con éxito! Ya puedes iniciar sesión.", HttpStatus.OK, null));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse("El usuario está deshabilitado. Contacta al administrador.", HttpStatus.FORBIDDEN, null));
        } catch (RuntimeException e) {
            log.warn("Token de verificacion invalido o expirado");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Token de verificacion invalido o expirado.", HttpStatus.BAD_REQUEST, null));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody UsuarioAuth entity, BindingResult result) {
        if (result.hasFieldErrors()) {
            log.warn("Errores de validacion al registrar usuario: {}", result.getAllErrors());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Datos invalidos. Revisa el email y que la contrasena tenga al menos 8 caracteres.", HttpStatus.BAD_REQUEST, null));
        }
        try {
            userServices.saveUser(entity);
            log.info("Usuario registrado: {}", entity.getEmail());
            return ResponseEntity.ok(new ApiResponse("Registro exitoso. Revisa tu correo para verificar tu cuenta.", HttpStatus.OK, null));
        } catch (Exception e) {
            log.warn("Error al registrar usuario", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("No se pudo completar el registro. Intente nuevamente.", HttpStatus.BAD_REQUEST, null));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            userServices.sendPasswordResetEmail(email);
        } catch (Exception e) {
            log.warn("Solicitud de restablecimiento para email no registrado o error de email");
        }
        // Respuesta generica siempre: no revela si el email existe (evita enumeracion).
        return ResponseEntity.ok().body("Si el email está registrado, recibirás un enlace para restablecer tu contraseña.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.length() < 8) {
                return ResponseEntity.badRequest()
                        .body("La contrasena debe tener al menos 8 caracteres.");
            }
            userServices.resetPassword(token, newPassword);
            return ResponseEntity.ok().body("Contraseña restablecida con éxito.");
        } catch (Exception e) {
            log.warn("Error al restablecer contrasena");
            return ResponseEntity.badRequest().body("No se pudo restablecer la contrasena.");
        }
    }
}
