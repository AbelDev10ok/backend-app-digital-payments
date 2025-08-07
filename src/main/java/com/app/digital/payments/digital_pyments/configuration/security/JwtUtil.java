package com.app.digital.payments.digital_pyments.configuration.security;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    
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
        
        return Collections.singletonList("ROLE_" + roles.getName().toUpperCase());

        // return roles.stream()
        //         .map(role -> new SimpleGrantedAuthority(role.getName()))
        //         .collect(Collectors.toList());
    }

}