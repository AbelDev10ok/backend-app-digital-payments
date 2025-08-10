package com.app.digital.payments.digital_pyments.configuration.security;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import static com.app.digital.payments.digital_pyments.configuration.security.TokenJwtConfig.*;

@Service
public class JwtUtil {

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(token).getPayload();
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Sobrecarga para generar el ACCESS TOKEN con un tiempo de expiración corto
    public String generateAccessToken(Usuario user) throws JsonProcessingException {
        List<String> roles = getAuthoritiesFromRoles(user.getRole());
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", new ObjectMapper().writeValueAsString(roles));

        return Jwts.builder()
                .claims(claims)
                .signWith(SECRET_KEY)
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                // .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15)) // 15 minutos
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 1)) // 1 minuto para pruebas

                .compact();
    }

    // Nuevo método para generar el REFRESH TOKEN con un tiempo de expiración largo
    public String generateRefreshToken(Usuario user) {
        return Jwts.builder()
                .signWith(SECRET_KEY)
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                // .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30)) // 30 días
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 10)) // 10 minutos para pruebas
                .compact();
    }
    
    // Nuevo método para validar la expiración de cualquier token
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }


    // private Boolean isTokenExpired(String token) {
    //     return extractExpiration(token).before(new Date());
    // }

    
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public Boolean validateToken(String token, Usuario user) {
        final String username = extractUsername(token);
        return (username.equals(user.getEmail()) && !isTokenExpired(token));
    }

    //sobrecarga para Users
    public String generateToken(Usuario user) {

        List<String> roles = getAuthoritiesFromRoles(user.getRole());
        return createToken(new HashMap<>(), user.getEmail(),roles);
    }

    private String createToken(Map<String, Object> claims, String subject ,List<String> authorities){
    
        try {
            claims.put("authorities", new ObjectMapper().writeValueAsString(authorities));
        } catch (JsonProcessingException e) {
            // Handle the exception appropriately (e.g., log and rethrow)
            throw new RuntimeException("Error serializing authorities", e); 
        }

        return Jwts.builder().claims(claims).signWith(SECRET_KEY).subject(subject).issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // Example: 10 hours
                .compact();
    }

    private List<String> getAuthoritiesFromRoles(Role roles) {
        
        return Collections.singletonList(roles.getName().toUpperCase());

        // return roles.stream()
        //         .map(role -> new SimpleGrantedAuthority(role.getName()))
        //         .collect(Collectors.toList());
    }

}