package com.app.digital.payments.digital_pyments.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.models.dtos.ChangePasswordRequestDto;
import com.app.digital.payments.digital_pyments.services.UsuarioServices;
import com.app.digital.payments.digital_pyments.utils.ApiResponse;
import com.app.digital.payments.digital_pyments.utils.ValidationEntities;

/**
 * Regresion del bug "Usuario no encontrado" al cambiar la contrasena: el filtro
 * JWT autentica con la entidad Usuario como principal, por lo que
 * authentication.getName() devuelve el toString() del objeto y no el email.
 */
class UsuarioControllerTest {

    @Mock
    private UsuarioServices usuarioServices;

    private UsuarioController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new UsuarioController(usuarioServices, new ValidationEntities());
    }

    private ChangePasswordRequestDto request() {
        ChangePasswordRequestDto dto = new ChangePasswordRequestDto();
        dto.setCurrentPassword("ClaveActual1");
        dto.setNewPassword("ClaveNueva123");
        return dto;
    }

    private BindingResult sinErrores(ChangePasswordRequestDto dto) {
        return new BeanPropertyBindingResult(dto, "request");
    }

    /** Principal tal como lo deja JwtValidationFilter: la entidad Usuario. */
    private Authentication autenticacionDe(String email) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        return new UsernamePasswordAuthenticationToken(usuario, null, List.of());
    }

    @Test
    void usaElEmailDelPrincipalYDevuelveElRefreshNuevo() {
        ChangePasswordRequestDto dto = request();
        when(usuarioServices.changePassword("abel.alarcon.dev@gmail.com", dto))
                .thenReturn("refresh-nuevo");

        ResponseEntity<?> response = controller.changePassword(
                dto, sinErrores(dto), autenticacionDe("abel.alarcon.dev@gmail.com"));

        // El email sale del principal, no de authentication.getName()
        verify(usuarioServices).changePassword("abel.alarcon.dev@gmail.com", dto);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ApiResponse body = (ApiResponse) response.getBody();
        assertEquals("Contraseña actualizada. Las demás sesiones fueron cerradas.", body.getMessage());
        // El cliente recibe el refresh nuevo para conservar esta sesion abierta
        assertEquals(Map.of("refreshToken", "refresh-nuevo"), body.getData());
    }

    @Test
    void noLlamaAlServicioSiLaValidacionFalla() {
        ChangePasswordRequestDto dto = request();
        BindingResult errores = new BeanPropertyBindingResult(dto, "request");
        errores.rejectValue("newPassword", "Size",
                "La nueva contrasena debe tener al menos 8 caracteres");

        ResponseEntity<?> response = controller.changePassword(
                dto, errores, autenticacionDe("abel.alarcon.dev@gmail.com"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(usuarioServices);
    }
}
