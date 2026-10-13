package com.app.digital.payments.digital_pyments.configuration.security.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CorsFilter implements Filter {

    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:5173");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String origin = httpRequest.getHeader(HttpHeaders.ORIGIN);

        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            httpResponse.setHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

            if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
                httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        "GET,POST,PUT,DELETE,PATCH");
                httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        "Authorization,Content-Type,X-Requested-With");
                httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
                httpResponse.setStatus(HttpStatus.OK.value());
                return;
            }
        }

        chain.doFilter(request, response);
    }
}