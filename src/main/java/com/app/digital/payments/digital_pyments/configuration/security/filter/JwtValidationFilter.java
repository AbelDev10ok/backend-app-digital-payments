package com.app.digital.payments.digital_pyments.configuration.security.filter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.repositories.IUsuarioRepository;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static com.app.digital.payments.digital_pyments.configuration.security.TokenJwtConfig.*;

public class JwtValidationFilter extends BasicAuthenticationFilter{

    private final IUsuarioRepository userRepository;


    public JwtValidationFilter(AuthenticationManager authenticationManager,IUsuarioRepository userRepository) {
        super(authenticationManager);
        this.userRepository = userRepository;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException, StreamReadException, DatabindException, java.io.IOException {

                String header = request.getHeader(HEADER_STRING);
                if(header == null || !header.startsWith(TOKEN_PREFIX)){
                    chain.doFilter(request, response);
                    return;
                }
                String token = header.replace(TOKEN_PREFIX, "");
                
                try {
                    // verificamos la firma del token
                    Claims claims = Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(token).getPayload();
                    String username = claims.getSubject();

                    Usuario user = userRepository.findByEmail(username)
                            .orElseThrow(() -> new JwtException("User not found in database"));
                    // user is enabled
                    if (!user.isEnabled()) {
                        System.out.println(user.isEnabled());
                        throw new JwtException("User is disabled");
                    }

                    // 1. Obtener los roles como un objeto del token
                    Object authoritiesClaims = claims.get("authorities");

                    // 2. Si no es nulo, usar ObjectMapper para deserializar el String a una lista de strings
                    Collection<? extends GrantedAuthority> authorities = null;
                    if (authoritiesClaims != null) {
                        // El valor es un String, lo parseamos a una lista de strings
                        List<String> rolesList = new ObjectMapper().readValue(authoritiesClaims.toString(), new TypeReference<List<String>>() {});

                        // Mapeamos la lista de strings a una colección de GrantedAuthority
                        authorities = rolesList.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                    }

                    // Si no hay roles, la colección estará vacía o nula. Puedes manejar esto.
                    if (authorities == null) {
                        authorities = Collections.emptyList();
                    }

                    // null porque el password solo se valida cuando creamos el token
                    
                    // INICIAMOS SESCION Y AUTHENTICAMOS
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken (user, null, authorities);
                    SecurityContextHolder .getContext().setAuthentication(authenticationToken);
                    
                    // continuar con los demas filtros
                    chain.doFilter(request, response);
                    return;

                } catch (JwtException e) {
                    
                    Map<String,String> body = new HashMap<>();
                    body.put("message", e.getMessage());
                    body.put("error", "token no es valido");
                    body.put("status", String.valueOf(HttpServletResponse.SC_UNAUTHORIZED));
                    body.put("request", request.getHeaderNames().toString());

                    response.getWriter().write(new ObjectMapper().writeValueAsString(body));
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(CONTENT_TYPE);
                }

    }

}
