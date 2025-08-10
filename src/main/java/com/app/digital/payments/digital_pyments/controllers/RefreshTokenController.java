package com.app.digital.payments.digital_pyments.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.configuration.security.JwtUtil;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.repositories.IUsuarioRepository;

@RestController
@RequestMapping("/auth")
public class RefreshTokenController {
      @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private IUsuarioRepository userRepository;

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body("Refresh token is required");
        }
        
        try {
            String username = jwtUtil.extractUsername(refreshToken);
            
            // 1. Validar el refresh token: firma, expiración
            if (jwtUtil.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired");
            }

            // 2. Buscar al usuario y verificar si el token coincide con el de la base de datos
            Usuario user = userRepository.findByEmailAndRefreshToken(username, refreshToken)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid refresh token"));

            // 3. Generar un nuevo access token
            String newAccessToken = jwtUtil.generateAccessToken(user);
            
            // 4. Devolver el nuevo access token y el mismo refresh token
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken, "refreshToken", refreshToken));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Refresh Token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body("Refresh token is required");
        }
        
        try {
            String username = jwtUtil.extractUsername(refreshToken);
            Usuario user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            // Invalidar el refresh token en la base de datos
            user.setRefreshToken(null);
            userRepository.save(user);
            
            return ResponseEntity.ok("Logout successful");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token or user");
        }
    }
}
