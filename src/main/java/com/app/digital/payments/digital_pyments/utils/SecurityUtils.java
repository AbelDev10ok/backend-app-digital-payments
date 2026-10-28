package com.app.digital.payments.digital_pyments.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.security.core.Authentication;

import com.app.digital.payments.digital_pyments.models.Usuario;

import jakarta.servlet.http.HttpServletRequest;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Email del usuario autenticado. El filtro JWT deja como principal la
     * entidad {@link Usuario}, asi que {@code authentication.getName()} NO es el
     * email (devuelve el toString() del objeto). Si el principal no es un
     * Usuario (p. ej. tests con @WithMockUser), cae al nombre del token.
     */
    public static String emailActual(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Usuario usuario) {
            return usuario.getEmail();
        }
        return authentication.getName();
    }

    public static String hashRefreshToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /**
     * IP del cliente para logs de auditoria. Confia en X-Forwarded-For/X-Real-IP.
     * Para enforcement (rate limiting) usar RateLimitFilter con trust-proxy=false.
     */
    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
