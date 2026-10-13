package com.app.digital.payments.digital_pyments.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.app.digital.payments.digital_pyments.utils.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class RefreshTokenController {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenController.class);

    private final JwtUtil jwtUtil;
    private final IUsuarioRepository userRepository;

    public RefreshTokenController(JwtUtil jwtUtil, IUsuarioRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
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

            // 2. Buscar al usuario y verificar que el hash del token coincida con el de la base de datos
            Usuario user = userRepository.findByEmailAndRefreshToken(username, SecurityUtils.hashRefreshToken(refreshToken))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid refresh token"));

            // 3. El usuario debe estar habilitado
            if (!user.isEnabled()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is disabled");
            }

            // 4. Rotar el refresh token: se genera uno nuevo, se persiste su hash y se devuelve
            String newAccessToken = jwtUtil.generateAccessToken(user);
            String newRefreshToken = jwtUtil.generateRefreshToken(user);
            user.setRefreshToken(SecurityUtils.hashRefreshToken(newRefreshToken));
            userRepository.save(user);

            // 5. Devolver el nuevo access token y el nuevo refresh token
            log.info("Refresh token rotado para {} desde {}", username, SecurityUtils.clientIp(httpRequest));
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken, "refreshToken", newRefreshToken));

        } catch (Exception e) {
            log.warn("Refresh token invalido o reutilizado desde {}: {}", SecurityUtils.clientIp(httpRequest), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Refresh Token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
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
            
            log.info("Logout exitoso para {} desde {}", username, SecurityUtils.clientIp(httpRequest));
            return ResponseEntity.ok("Logout successful");
        } catch (Exception e) {
            log.warn("Logout con token invalido desde {}", SecurityUtils.clientIp(httpRequest));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token or user");
        }
    }
}
