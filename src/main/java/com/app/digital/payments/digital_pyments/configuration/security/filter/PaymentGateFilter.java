package com.app.digital.payments.digital_pyments.configuration.security.filter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.app.digital.payments.digital_pyments.services.SubscriptionService;
import com.app.digital.payments.digital_pyments.utils.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de gating (402 Payment Required).
 * Corre despues de la validacion JWT y verifica que el negocio tenga acceso
 * activo (suscripcion pagada o trial vigente).
 *
 * Rutas exentas: /auth/*, /api/billing/*, /api/webhooks/*, /error
 */
@Component
public class PaymentGateFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PaymentGateFilter.class);

    private static final Set<String> EXEMPT_PREFIXES = Set.of(
            "/auth",
            "/api/billing",
            "/api/webhooks",
            "/error");

    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    public PaymentGateFilter(SubscriptionService subscriptionService, ObjectMapper objectMapper) {
        this.subscriptionService = subscriptionService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return EXEMPT_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Long businessId = TenantContext.getBusinessId();

        // Sin negocio autenticado (ruta publica): pasar
        if (businessId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (subscriptionService.hasActiveAccess(businessId)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Acceso bloqueado (402) para negocio {} en {}", businessId, request.getRequestURI());

        Map<String, String> body = Map.of(
                "message", "Suscripcion requerida. Visita /dashboard/suscripcion para activar tu plan.",
                "payment_required", "true");
        response.setStatus(402);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}