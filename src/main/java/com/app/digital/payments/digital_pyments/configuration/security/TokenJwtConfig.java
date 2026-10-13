package com.app.digital.payments.digital_pyments.configuration.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class TokenJwtConfig {

    private static final Logger log = LoggerFactory.getLogger(TokenJwtConfig.class);

    private static final String JWT_SECRET = System.getenv("JWT_SECRET");

    // La clave va a quedar secreta y segura solo en el servidor.
    // Si JWT_SECRET no está definida, se genera una aleatoria (los tokens se
    // invalidan al reiniciar la app).
    public static final SecretKey SECRET_KEY;

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final String CONTENT_TYPE = "application/json";

    static {
        if (JWT_SECRET != null && !JWT_SECRET.isBlank()) {
            byte[] keyBytes = JWT_SECRET.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 64) {
                throw new IllegalArgumentException("JWT_SECRET debe tener al menos 64 caracteres para HS512");
            }
            SECRET_KEY = Keys.hmacShaKeyFor(keyBytes);
        } else {
            log.warn("JWT_SECRET no está definida; se genera una clave aleatoria. Los tokens se invalidarán al reiniciar.");
            SECRET_KEY = Jwts.SIG.HS512.key().build();
        }
    }
}
