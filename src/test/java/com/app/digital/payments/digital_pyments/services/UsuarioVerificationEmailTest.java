package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.app.digital.payments.digital_pyments.configuration.security.JwtUtil;
import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.models.dtos.UsuarioAuth;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.IRoleRepository;
import com.app.digital.payments.digital_pyments.repositories.IUsuarioRepository;

/**
 * El mail de verificacion que se manda al registrarse tiene que apuntar a la
 * pagina del frontend (app.frontend-url), nunca a un endpoint del backend.
 */
class UsuarioVerificationEmailTest {

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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        usuarioServices = new UsuarioServices(usuarioRepository, roleRepository, passwordEncoder,
                emailServices, businessRepository, productTypeRepository, jwtUtil, FRONTEND_URL);

        when(usuarioRepository.existsByEmail("dueño@negocio.com")).thenReturn(false);
        Role role = new Role();
        when(roleRepository.findByName(Role.ROLE_USER)).thenReturn(java.util.Optional.of(role));
        when(passwordEncoder.encode("claveSegura123")).thenReturn("hash-nuevo");
        when(businessRepository.save(any(Business.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productTypeRepository.save(any(ProductType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void saveUserMandaElLinkDeVerificacionALaPaginaDelFrontend() throws Exception {
        usuarioServices.saveUser(new UsuarioAuth("dueño@negocio.com", "claveSegura123"));

        ArgumentCaptor<Usuario> usuarioGuardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioGuardado.capture());
        String token = usuarioGuardado.getValue().getVerificationToken();

        verify(emailServices).sendVerificationEmail("dueño@negocio.com",
                FRONTEND_URL + "/verify?token=" + token);
        assertTrue(token != null && !token.isBlank(), "el token de verificacion no puede estar vacio");
    }
}
