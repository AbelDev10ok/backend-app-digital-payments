package com.app.digital.payments.digital_pyments.configuration.security.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.app.digital.payments.digital_pyments.configuration.security.JwtUtil;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.repositories.IUsuarioRepository;
import com.app.digital.payments.digital_pyments.utils.ApiResponse;
import com.app.digital.payments.digital_pyments.utils.SecurityUtils;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static com.app.digital.payments.digital_pyments.configuration.security.TokenJwtConfig.*;


public class JwtAutenticacionFilter extends UsernamePasswordAuthenticationFilter  {

    private static final Logger log = LoggerFactory.getLogger(JwtAutenticacionFilter.class);

    private final AuthenticationManager authenticationManager;
    private final IUsuarioRepository userRepository;
    private final JwtUtil jwtUtil;

    public JwtAutenticacionFilter(AuthenticationManager authenticationManager, IUsuarioRepository userRepository, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        setFilterProcessesUrl("/auth/login"); // Endpoint para el login
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
                log.error("Error de parseo del body de login", e);
            } catch (DatabindException e) {
                log.error("Error de deserialización del body de login", e);
            } catch(java.io.IOException e) {
                log.error("Error de entrada/salida al leer el body de login", e);
            }

        request.setAttribute("attemptedUsername", username);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);

        return authenticationManager.authenticate(authToken); 
    }


    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            Authentication authResult) throws IOException, ServletException {

        User user = (User) authResult.getPrincipal();
        String username = user.getUsername();

        log.info("Login exitoso para {} desde {}", username, SecurityUtils.clientIp(request));

        // Obtener el usuario de la base de datos para generar los tokens y guardar el refresh token
        Usuario userDb = userRepository.findByEmail(username).orElseThrow(() -> 
            new UsernameNotFoundException("User not found in database"));

        // Generar ambos tokens
        String accessToken = jwtUtil.generateAccessToken(userDb);
        String refreshToken = jwtUtil.generateRefreshToken(userDb);

        // Guardar el hash del refresh token en la base de datos (nunca en claro)
        userDb.setRefreshToken(SecurityUtils.hashRefreshToken(refreshToken));
        userRepository.save(userDb);
        
        // Devolver ambos tokens en la respuesta
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("accessToken", accessToken);
        body.put("refreshToken", refreshToken);
        

        response.addHeader(HEADER_STRING, TOKEN_PREFIX + accessToken);
        response.getWriter().write(new ObjectMapper().writeValueAsString(new ApiResponse("Login succes", HttpStatus.OK, body)));
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(CONTENT_TYPE);
    }
    


        @Override
        protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failed) throws IOException, ServletException {

                String attempted = String.valueOf(request.getAttribute("attemptedUsername"));
                log.warn("Intento de login fallido para {} desde {}", attempted, SecurityUtils.clientIp(request));

                Map<String,String> body = new HashMap<>();
                body.put("message", "Error de autenticacion username o password incorrecto");
                body.put("error", "Authentication failed");
                body.put("status", String.valueOf(HttpServletResponse.SC_UNAUTHORIZED));

                response.getWriter().write(new ObjectMapper().writeValueAsString(body));
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(CONTENT_TYPE);
            }

}
