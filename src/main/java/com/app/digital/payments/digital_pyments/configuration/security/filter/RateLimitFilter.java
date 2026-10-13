package com.app.digital.payments.digital_pyments.configuration.security.filter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.app.digital.payments.digital_pyments.utils.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final long WINDOW_MILLIS = 60_000L;

    private static final Set<String> SENSITIVE_PATHS = Set.of(
            "/auth/login",
            "/auth/register",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/auth/refresh-token");

    private final int maxRequestsPerMinute;
    private final int sensitiveRequestsPerMinute;
    private final boolean trustProxy;
    private final Map<String, Deque<Long>> requests = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.rate-limit.max-requests-per-minute:10}") int maxRequestsPerMinute,
            @Value("${app.rate-limit.sensitive-requests-per-minute:5}") int sensitiveRequestsPerMinute,
            @Value("${app.rate-limit.trust-proxy:false}") boolean trustProxy) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.sensitiveRequestsPerMinute = sensitiveRequestsPerMinute;
        this.trustProxy = trustProxy;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return !(uri.startsWith("/auth") || uri.equals("/email/send"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = clientIp(request) + "|" + request.getRequestURI();
        int limit = sensitiveLimit(request.getRequestURI());
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = requests.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MILLIS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limit) {
                log.warn("Rate limit superado para {} en {} ({} req/min)", key, request.getRequestURI(), limit);
                ApiResponse body = new ApiResponse(
                        "Demasiadas solicitudes. Intente más tarde.",
                        HttpStatus.TOO_MANY_REQUESTS,
                        null);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(new ObjectMapper().writeValueAsString(body));
                return;
            }
            timestamps.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    private int sensitiveLimit(String uri) {
        if (SENSITIVE_PATHS.contains(uri)) {
            return sensitiveRequestsPerMinute;
        }
        return maxRequestsPerMinute;
    }

    private String clientIp(HttpServletRequest request) {
        // Por defecto NO se confia en headers de forwarding (evita spoofing).
        if (trustProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
