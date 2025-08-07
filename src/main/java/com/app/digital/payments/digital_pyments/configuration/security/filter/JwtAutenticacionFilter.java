package com.app.digital.payments.digital_pyments.configuration.security.filter;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.app.digital.payments.digital_pyments.models.Usuario;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import static com.app.digital.payments.digital_pyments.configuration.security.TokenJwtConfig.*;


public class JwtAutenticacionFilter extends UsernamePasswordAuthenticationFilter  {

    private AuthenticationManager authenticationManager;

    public JwtAutenticacionFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }


    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)throws org.springframework.security.core.AuthenticationException {
        
        Usuario user = null;
        String password = null;
        String username = null;

            try {
                user = new ObjectMapper().readValue(request.getInputStream(), Usuario.class);
                username = user.getEmail();
                password =user.getPassword();
            } catch (StreamReadException e) {
                e.printStackTrace();
            } catch (DatabindException e) {
                e.printStackTrace();
            } catch(java.io.IOException e) {
                e.printStackTrace();
            }

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);

        return authenticationManager.authenticate(authToken); 
    }


     @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            Authentication authResult) throws IOException, ServletException {

            // claims
            User user = (User) authResult.getPrincipal();
            String username = user.getUsername();
            Collection<? extends GrantedAuthority> authorities = authResult.getAuthorities();


            // creamos clamis
            Claims claims = Jwts.claims()
                                .add("authorities",new ObjectMapper().writeValueAsString(authorities))
                                .add("username",username)
                                .build(); 
            String token = Jwts.builder()
                            .subject(username)
                            .claims(claims)
                            .expiration(new Date(System.currentTimeMillis() + 3600000))
                            .issuedAt(new Date())
                            .signWith(SECRET_KEY)
                            .compact();
            response.addHeader(HEADER_STRING,TOKEN_PREFIX + token);
                
                Map<String,String> body = new HashMap<>();
                body.put("username", username);
                body.put("token", token);
                response.getWriter().write(new ObjectMapper().writeValueAsString(body));
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType(CONTENT_TYPE);
    }


        @Override
        protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failed) throws IOException, ServletException {

                Map<String,String> body = new HashMap<>();
                body.put("message", "Error de autenticacion username o password incorrecto");
                body.put("error", failed.getMessage());
                body.put("status", String.valueOf(HttpServletResponse.SC_UNAUTHORIZED));
                body.put("request", request.getHeaderNames().toString());

                response.getWriter().write(new ObjectMapper().writeValueAsString(body));
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(CONTENT_TYPE);
            }

}
