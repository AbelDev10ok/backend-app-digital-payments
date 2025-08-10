package com.app.digital.payments.digital_pyments.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.configuration.security.JwtUtil;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.models.dtos.UsuarioAuth;
import com.app.digital.payments.digital_pyments.repositories.IUsuarioRepository;
import com.app.digital.payments.digital_pyments.services.UsuarioServices;
import com.app.digital.payments.digital_pyments.utils.ValidationEntities;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UsuarioServices userServices;
    @Autowired
    private IUsuarioRepository userRepository;
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ValidationEntities validationEntities;

    // @PostMapping("/register")
    // public ResponseEntity<?> registerUser(@Valid @RequestBody UsuarioAuth entity,BindingResult result ) {   
    //     if(result.hasFieldErrors()){
    //         return validationEntities.validation(result);
    //     }
    //     try {
    //         // entity.setAdmin(false);
    //         userServices.saveUser(entity);     
    //         return ResponseEntity.ok().body("success");
            
    //     } catch (Exception e) {
    //         return ResponseEntity.badRequest().body(e.getMessage());
    //     }
    // }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam String token) {
        try {
            Usuario usuarioVerificado = userServices.verifyUser(token);
            if (usuarioVerificado.isEnabled()) {
                return ResponseEntity.ok("¡Correo verificado con éxito! Ya puedes iniciar sesión.");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al verificar el correo.");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    
    // ... el método registerUser() ahora solo llama al servicio
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UsuarioAuth entity, BindingResult result ) {   
         if(result.hasFieldErrors()){
             return validationEntities.validation(result);
         }
         try {
             userServices.saveUser(entity);    
             return ResponseEntity.ok().body("Registro exitoso. Revisa tu correo para verificar tu cuenta.");
         } catch (Exception e) {
             return ResponseEntity.badRequest().body(e.getMessage());
         }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody UsuarioAuth entity) {
        try {
            log.info("Iniciando intento de login para el usuario: {}", entity.getEmail());

            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(entity.getEmail(), entity.getPassword())
            );

            String email = ((User) authentication.getPrincipal()).getUsername();
            log.info("Autenticación exitosa. Buscando detalles del usuario en la base de datos.");

            Usuario user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (!user.isEnabled()) {
                log.warn("El usuario {} está deshabilitado.", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("error Usuario deshabilitado");
            }

            log.info("Usuario {} encontrado y habilitado. Generando token JWT.", email);
            String token = jwtUtil.generateToken(user);

            return ResponseEntity.ok(Map.of("id", user.getId(), "email", user.getEmail(), "token", token));

        } catch (BadCredentialsException e) {
            log.error("Credenciales incorrectas para el usuario: {}", entity.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("error Credenciales incorrectas");
        } catch (Exception e) {
            // Este es el log crucial para encontrar la causa del error 500
            log.error("Error interno del servidor al procesar el login para {}:", entity.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error Error interno del servidor");
        }
    }
}
