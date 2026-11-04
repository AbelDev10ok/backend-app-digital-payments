package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.app.digital.payments.digital_pyments.configuration.security.JwtUtil;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.models.dtos.EmailDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.IRoleRepository;
import com.app.digital.payments.digital_pyments.repositories.IUsuarioRepository;

class UsuarioPasswordResetTest {

    private static final String FRONTEND_URL = "http://localhost:5173";

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private IRoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailServices emailServices;

    @Mock
    private IBusinessRepository businessRepository;

    @Mock
    private IProductTypeRepository productTypeRepository;

    @Mock
    private JwtUtil jwtUtil;

    private UsuarioServices usuarioServices;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        usuarioServices = new UsuarioServices(usuarioRepository, roleRepository, passwordEncoder,
                emailServices, businessRepository, productTypeRepository, jwtUtil, FRONTEND_URL);

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("dueño@negocio.com");
    }

    @Test
    void sendPasswordResetEmailEnlazaALaPaginaDelFrontend() throws Exception {
        when(usuarioRepository.findByEmail("dueño@negocio.com")).thenReturn(Optional.of(usuario));

        usuarioServices.sendPasswordResetEmail("dueño@negocio.com");

        String token = usuario.getResetPasswordToken();
        assertNotNull(token);
        // El enlace tiene que abrir la pagina del frontend, no un endpoint del backend
        verify(emailServices).sendPasswordResetEmail("dueño@negocio.com",
                FRONTEND_URL + "/reset-password?token=" + token);
    }

    @Test
    void sendPasswordResetEmailConservaElTokenConUnaHoraDeValidez() throws Exception {
        when(usuarioRepository.findByEmail("dueño@negocio.com")).thenReturn(Optional.of(usuario));

        LocalDateTime antes = LocalDateTime.now();
        usuarioServices.sendPasswordResetEmail("dueño@negocio.com");

        assertNotNull(usuario.getResetPasswordToken());
        assertNotNull(usuario.getResetPasswordTokenExpiryDate());
        assertTrue(usuario.getResetPasswordTokenExpiryDate().isAfter(antes.plusMinutes(50)));
        assertTrue(usuario.getResetPasswordTokenExpiryDate().isBefore(antes.plusHours(2)));

        verify(emailServices).sendPasswordResetEmail("dueño@negocio.com",
                FRONTEND_URL + "/reset-password?token=" + usuario.getResetPasswordToken());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void resetPasswordConTokenValidoHasheaLimpiaElTokenEInvalidaLasSesiones() {
        usuario.setPassword("hash-viejo");
        usuario.setResetPasswordToken("tok-valido");
        usuario.setResetPasswordTokenExpiryDate(LocalDateTime.now().plusMinutes(30));
        usuario.setRefreshToken("refresh-viejo");
        when(usuarioRepository.findByResetPasswordToken("tok-valido")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("claveSegura123")).thenReturn("hash-nuevo");

        usuarioServices.resetPassword("tok-valido", "claveSegura123");

        assertEquals("hash-nuevo", usuario.getPassword());
        assertNull(usuario.getResetPasswordToken(), "el token se limpia tras el uso");
        assertNull(usuario.getResetPasswordTokenExpiryDate(), "la expiracion se limpia tras el uso");
        assertNull(usuario.getRefreshToken(), "el cambio de clave invalida las sesiones abiertas");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void resetPasswordConTokenDesconocidoNoCambiaLaContrasena() {
        usuario.setPassword("hash-viejo");
        when(usuarioRepository.findByResetPasswordToken("tok-fantasma")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> usuarioServices.resetPassword("tok-fantasma", "claveSegura123"));

        assertEquals("hash-viejo", usuario.getPassword());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void resetPasswordConTokenExpiradoNoCambiaLaContrasena() {
        usuario.setPassword("hash-viejo");
        usuario.setResetPasswordToken("tok-viejo");
        usuario.setResetPasswordTokenExpiryDate(LocalDateTime.now().minusMinutes(5));
        usuario.setRefreshToken("refresh-viejo");
        when(usuarioRepository.findByResetPasswordToken("tok-viejo")).thenReturn(Optional.of(usuario));

        assertThrows(RuntimeException.class,
                () -> usuarioServices.resetPassword("tok-viejo", "claveSegura123"));

        assertEquals("hash-viejo", usuario.getPassword());
        assertEquals("refresh-viejo", usuario.getRefreshToken());
        verify(usuarioRepository, never()).save(any());
    }
}
