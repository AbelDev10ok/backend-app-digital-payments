package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.app.digital.payments.digital_pyments.configuration.security.JwtUtil;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.models.dtos.ChangePasswordRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.IRoleRepository;
import com.app.digital.payments.digital_pyments.repositories.IUsuarioRepository;
import com.app.digital.payments.digital_pyments.utils.SecurityUtils;

class UsuarioChangePasswordTest {

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private IRoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
        // Los mocks no usados (rol, negocio, categorias) quedan en null-safe por defecto
        usuarioServices = new UsuarioServices(usuarioRepository, roleRepository, passwordEncoder,
                null, businessRepository, productTypeRepository, jwtUtil);

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("dueño@negocio.com");
        usuario.setPassword("hash-viejo");
        usuario.setRefreshToken("refresh-activo");
    }

    private ChangePasswordRequestDto request(String actual, String nueva) {
        ChangePasswordRequestDto dto = new ChangePasswordRequestDto();
        dto.setCurrentPassword(actual);
        dto.setNewPassword(nueva);
        return dto;
    }

    @Test
    void changePasswordValidaHasheaYDevuelveRefreshNuevo() {
        when(usuarioRepository.findByEmail("dueño@negocio.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("ClaveActual1", "hash-viejo")).thenReturn(true);
        when(passwordEncoder.encode("ClaveNueva123")).thenReturn("hash-nuevo");
        when(jwtUtil.generateRefreshToken(usuario)).thenReturn("refresh-nuevo");

        String refreshDevuelto = usuarioServices.changePassword("dueño@negocio.com",
                request("ClaveActual1", "ClaveNueva123"));

        assertEquals("hash-nuevo", usuario.getPassword());
        // Rota la sesion: guarda el hash del token nuevo y lo devuelve al cliente
        assertEquals("refresh-nuevo", refreshDevuelto);
        assertEquals(SecurityUtils.hashRefreshToken("refresh-nuevo"), usuario.getRefreshToken());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void changePasswordRechazaContrasenaActualIncorrecta() {
        when(usuarioRepository.findByEmail("dueño@negocio.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> usuarioServices.changePassword("dueño@negocio.com", request("mala", "ClaveNueva123")));

        assertEquals("hash-viejo", usuario.getPassword());
        assertNotNull(usuario.getRefreshToken());
        verify(usuarioRepository, never()).save(usuario);
    }

    @Test
    void changePasswordRechazaContrasenaIgualALaActual() {
        when(usuarioRepository.findByEmail("dueño@negocio.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mismaclave", "hash-viejo")).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> usuarioServices.changePassword("dueño@negocio.com", request("mismaclave", "mismaclave")));

        verify(usuarioRepository, never()).save(usuario);
    }

    @Test
    void changePasswordFallaConUsuarioInexistente() {
        when(usuarioRepository.findByEmail("fantasma@negocio.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> usuarioServices.changePassword("fantasma@negocio.com", request("x", "y")));
    }
}
