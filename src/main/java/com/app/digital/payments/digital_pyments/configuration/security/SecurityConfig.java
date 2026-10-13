package com.app.digital.payments.digital_pyments.configuration.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;

import com.app.digital.payments.digital_pyments.configuration.security.filter.JwtAutenticacionFilter;
import com.app.digital.payments.digital_pyments.configuration.security.filter.JwtValidationFilter;
import com.app.digital.payments.digital_pyments.configuration.security.filter.PaymentGateFilter;
import com.app.digital.payments.digital_pyments.configuration.security.filter.RateLimitFilter;
import com.app.digital.payments.digital_pyments.configuration.security.filter.SecurityHeadersFilter;
import com.app.digital.payments.digital_pyments.repositories.IUsuarioRepository;

import com.app.digital.payments.digital_pyments.configuration.security.filter.CorsFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final IUsuarioRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final RateLimitFilter rateLimitFilter;
    private final SecurityHeadersFilter securityHeadersFilter;
    private final PaymentGateFilter paymentGateFilter;

    public SecurityConfig(IUsuarioRepository userRepository, JwtUtil jwtUtil,
            AuthenticationConfiguration authenticationConfiguration, RateLimitFilter rateLimitFilter,
            SecurityHeadersFilter securityHeadersFilter,
            PaymentGateFilter paymentGateFilter) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.authenticationConfiguration = authenticationConfiguration;
        this.rateLimitFilter = rateLimitFilter;
        this.securityHeadersFilter = securityHeadersFilter;
        this.paymentGateFilter = paymentGateFilter;
    }

    @Bean
    AuthenticationManager authenticationManager() throws Exception{
        return authenticationConfiguration.getAuthenticationManager();
    }
    
    // encriptacion
    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }


    @Bean
    DefaultSecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        return http.authorizeHttpRequests((auth) -> auth
                .requestMatchers(HttpMethod.POST ,"/auth/*").permitAll()
                .requestMatchers(HttpMethod.GET ,"/auth/*").permitAll()
                .requestMatchers(HttpMethod.POST ,"/auth/refresh-token").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/webhooks/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/webhooks/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**").permitAll()

                .anyRequest().authenticated())
                .addFilterBefore(rateLimitFilter, JwtAutenticacionFilter.class)
                .addFilterBefore(securityHeadersFilter, RateLimitFilter.class)
                .addFilter(new JwtAutenticacionFilter(authenticationManager(), userRepository, jwtUtil))
                .addFilter(new JwtValidationFilter(authenticationManager(), userRepository))
                .addFilterAfter(paymentGateFilter, JwtValidationFilter.class)
                .csrf(config -> config.disable())
                .sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(CorsFilter corsFilter) {
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(corsFilter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }



}
