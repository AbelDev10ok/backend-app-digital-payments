package com.app.digital.payments.digital_pyments.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.configuration.security.JwtUtil;
import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.models.dtos.ChangePasswordRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.EmailDto;
import com.app.digital.payments.digital_pyments.models.dtos.UsuarioAuth;
import com.app.digital.payments.digital_pyments.models.dtos.UsuarioResponse;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.IRoleRepository;
import com.app.digital.payments.digital_pyments.repositories.IUsuarioRepository;
import com.app.digital.payments.digital_pyments.utils.SecurityUtils;

import jakarta.mail.MessagingException;

@Service
public class UsuarioServices {

    // Moneda por defecto del negocio (configuracion futura por tenant)
    private static final String MONEDA_DEFAULT = "ARS";
    private static final String NOMBRE_NEGOCIO_DEFAULT = "Mi Negocio";

    // Catalogo inicial de categorias que recibe todo negocio nuevo
    // (mismas semillas que la migracion V3)
    private static final List<String> CATEGORIAS_INICIALES = List.of(
            "TV", "CELULAR", "MUEBLERIA", "RELOJ", "OTRO",
            "ELECTRODOMESTICO", "ROPA", "HERRAMIENTA");

    private final IUsuarioRepository usuarioRepository;

    private final IRoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailServices emailServices;

    private final IBusinessRepository businessRepository;

    private final IProductTypeRepository productTypeRepository;

    private final JwtUtil jwtUtil;

    public UsuarioServices(IUsuarioRepository usuarioRepository, IRoleRepository roleRepository,
            PasswordEncoder passwordEncoder, EmailServices emailServices,
            IBusinessRepository businessRepository, IProductTypeRepository productTypeRepository,
            JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailServices = emailServices;
        this.businessRepository = businessRepository;
        this.productTypeRepository = productTypeRepository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(readOnly=true)
    public List<UsuarioResponse> getAllUsers() {
        return usuarioRepository.findAll().stream()
                .map(user -> 
                    {
                        UsuarioResponse response = new UsuarioResponse();
                        response.setId(user.getId());
                        response.setEmail(user.getEmail());
                        response.setRole(user.getRole());
                        response.setEnabled(user.isEnabled());
                        return response;
                    }
                
                ) // Usa ModelMapper aquí
                .toList();
    }
    
    @Transactional(readOnly=true)
    public UsuarioResponse getUsersById(Long id) {
        return usuarioRepository.findById(id)
                .map(user -> {
                    UsuarioResponse response = new UsuarioResponse();
                    response.setId(user.getId());
                    response.setEmail(user.getEmail());
                    response.setRole(user.getRole());
                    response.setEnabled(user.isEnabled());
                    return response;
                })
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));
    }


    @Transactional
    public void saveUser(UsuarioAuth user) throws MessagingException { // El método ahora puede lanzar una excepción de mensajería
        boolean exists = usuarioRepository.existsByEmail(user.getEmail());
        if(exists){
            throw new RuntimeException("El usuario ya existe con el email: " + user.getEmail());
        }
        Optional<Role> roleUser = roleRepository.findByName(Role.ROLE_USER); 
        
        if(roleUser.isEmpty()){
            throw new RuntimeException("Rol no encontrado: " + Role.ROLE_USER);
        }

        Role role = roleUser.get();

        // Registro SaaS transaccional: se crea el negocio (tenant) junto con el
        // usuario. Si algo falla, no queda ni negocio ni usuario huerfanos.
        Business negocio = crearNegocio(user.getNombreNegocio());
        
        Usuario userDb = new Usuario();
        userDb.setEmail(user.getEmail());
        userDb.setPassword(passwordEncoder.encode(user.getPassword()));
        userDb.setRole(role);
        userDb.setBusiness(negocio);
        
        // ✨ Logica de verificación de email ✨
        // 1. Establecer el usuario como no habilitado por defecto
        userDb.setEnabled(false);
        
        // 2. Generar un token único para la verificación
        String verificationToken = UUID.randomUUID().toString();
        userDb.setVerificationToken(verificationToken);
        
        // 3. Establecer una fecha de expiración para el token (ej. 24 horas)
        userDb.setTokenExpiryDate(LocalDateTime.now().plusHours(24));
        
        // Guardar el usuario con los nuevos campos
        usuarioRepository.save(userDb);

        // 4. Preparar y enviar el email de verificación
        EmailDto emailDto = new EmailDto();
        emailDto.setDestinatario(user.getEmail());
        emailDto.setAsunto("Confirmación de correo electrónico");
        
        String verificationLink = "http://localhost:8080/auth/verify?token=" + verificationToken;
        String mensaje = "Hola, haz clic en el siguiente enlace para verificar tu correo: " + verificationLink;
        emailDto.setMensaje(mensaje);
        
        emailServices.sendEmail(emailDto);
        
        // En el controlador, puedes devolver un mensaje al usuario indicando
        // que revise su correo para completar el registro.
    }

    /**
     * Crea el negocio del registro y siembra su catalogo inicial de categorias.
     * Se llama dentro de la transaccion de saveUser.
     */
    private Business crearNegocio(String nombreNegocio) {
        String nombre = nombreNegocio != null && !nombreNegocio.isBlank()
                ? nombreNegocio.trim()
                : NOMBRE_NEGOCIO_DEFAULT;
        if (nombre.length() > 255) {
            nombre = nombre.substring(0, 255);
        }
        Business negocio = new Business(nombre, MONEDA_DEFAULT);
        negocio.setTrialEndsAt(LocalDate.now().plusDays(14));
        negocio = businessRepository.save(negocio);

        for (String categoria : CATEGORIAS_INICIALES) {
            ProductType productType = new ProductType();
            productType.setName(categoria);
            productType.setBusiness(negocio);
            productTypeRepository.save(productType);
        }
        return negocio;
    }
    

    /**
     * Nuevo método para manejar la lógica de verificación.
     * Busca al usuario por el token, valida la fecha y lo habilita.
     */
    @Transactional
    public Usuario verifyUser(String token) {
        return usuarioRepository.findByVerificationToken(token)
            .filter(usuario -> usuario.getTokenExpiryDate().isAfter(LocalDateTime.now()))
            .map(usuario -> {
                usuario.setEnabled(true);
                usuario.setVerificationToken(null);
                usuario.setTokenExpiryDate(null);
                return usuarioRepository.save(usuario);
            })
            .orElseThrow(() -> new RuntimeException("Token de verificación inválido o expirado."));
    }

    @Transactional
    public void sendPasswordResetEmail(String email) throws MessagingException {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("El email no está registrado."));

        // 1. Generar un token único para el restablecimiento de contraseña
        String resetToken = UUID.randomUUID().toString();
        usuario.setResetPasswordToken(resetToken);
        usuario.setResetPasswordTokenExpiryDate(LocalDateTime.now().plusHours(1)); // El token expira en 1 hora
        
        usuarioRepository.save(usuario);

        // 2. Preparar y enviar el email
        EmailDto emailDto = new EmailDto();
        emailDto.setDestinatario(email);
        emailDto.setAsunto("Restablecimiento de Contraseña");
        
        String resetLink = "http://localhost:8080/auth/reset-password?token=" + resetToken;
        String mensaje = "Hola, haz clic en el siguiente enlace para restablecer tu contraseña: " + resetLink;
        emailDto.setMensaje(mensaje);
        
        emailServices.sendEmail(emailDto);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        Usuario usuario = usuarioRepository.findByResetPasswordToken(token)
            .orElseThrow(() -> new RuntimeException("Token de restablecimiento inválido o expirado."));
        
        // 1. Validar que el token no haya expirado
        if (usuario.getResetPasswordTokenExpiryDate() == null || usuario.getResetPasswordTokenExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El token de restablecimiento ha expirado. Por favor, solicita uno nuevo.");
        }
        
        // 2. Encriptar y actualizar la nueva contraseña
        usuario.setPassword(passwordEncoder.encode(newPassword));
        
        // 3. Limpiar los campos del token para que no pueda ser usado de nuevo
        usuario.setResetPasswordToken(null);
        usuario.setResetPasswordTokenExpiryDate(null);

        // 4. Invalidar las sesiones activas al cambiar la contraseña
        usuario.setRefreshToken(null);
        
        usuarioRepository.save(usuario);
    }

    /**
     * Cambio de contrasena autenticado. Valida la contrasena actual, guarda la
     * nueva hasheada y rota el refresh token: las otras sesiones quedan
     * invalidadas y se devuelve el refresh nuevo para que este dispositivo
     * conserve la sesion abierta.
     */
    @Transactional
    public String changePassword(String email, ChangePasswordRequestDto request) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), usuario.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta.");
        }
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new RuntimeException("La nueva contraseña debe ser distinta de la actual.");
        }

        usuario.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Rotar el refresh token invalida las sesiones de los otros dispositivos;
        // el token nuevo se devuelve al cliente que hizo el cambio.
        String refreshNuevo = jwtUtil.generateRefreshToken(usuario);
        usuario.setRefreshToken(SecurityUtils.hashRefreshToken(refreshNuevo));

        usuarioRepository.save(usuario);
        return refreshNuevo;
    }

        
    @Transactional
    public void deleteUserById(Long id) {
        Optional<Usuario> user = usuarioRepository.findById(id);
        if(!user.isPresent()){
            throw new RuntimeException("User not found");
        }
        usuarioRepository.deleteById(id);
    }

    @Transactional
    public void disableUser(String email, boolean enabled) {
        Optional<Usuario> userOptional = usuarioRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            Usuario user = userOptional.get();
            user.setEnabled(enabled);
            usuarioRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public boolean existsUser(String email) {
        return usuarioRepository.existsByEmail(email);
    }


}
