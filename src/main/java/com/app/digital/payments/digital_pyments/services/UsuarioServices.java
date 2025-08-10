package com.app.digital.payments.digital_pyments.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.models.dtos.EmailDto;
import com.app.digital.payments.digital_pyments.models.dtos.UsuarioAuth;
import com.app.digital.payments.digital_pyments.models.dtos.UsuarioResponse;
import com.app.digital.payments.digital_pyments.repositories.IRoleRepository;
import com.app.digital.payments.digital_pyments.repositories.IUsuarioRepository;

import io.jsonwebtoken.security.Password;
import jakarta.mail.MessagingException;

@Service
public class UsuarioServices {

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailServices emailServices; // Inyecta tu servicio de email

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
    public Usuario getUsersById(Long id) {
        return usuarioRepository.findById(id).get();
    }

    public Usuario findByEmail(String email){
        return usuarioRepository.findByEmail(email).get();
    }


    @Transactional
    public void saveUser(UsuarioAuth user) throws MessagingException { // El método ahora puede lanzar una excepción de mensajería
        boolean exists = usuarioRepository.existsByEmail(user.getEmail());
        if(exists){
            throw new RuntimeException("El usuario ya existe con el email: " + user.getEmail());
        }
        Optional<Role> roleUser = roleRepository.findByName("ROLE_USER"); 
        
        if(roleUser.isEmpty()){
            throw new RuntimeException("Rol no encontrado: ROLE_USER");
        }

        Role role = roleUser.get();
        
        Usuario userDb = new Usuario();
        userDb.setEmail(user.getEmail());
        userDb.setPassword(passwordEncoder.encode(user.getPassword()));
        userDb.setRole(role);
        
        // ✨ NUEVA LÓGICA DE VERIFICACIÓN DE EMAIL ✨
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
    
    // ---
    
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
