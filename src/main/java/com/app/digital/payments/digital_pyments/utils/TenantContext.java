package com.app.digital.payments.digital_pyments.utils;

/**
 * Contexto de tenant por hilo. El JwtValidationFilter lo setea con el negocio
 * del usuario autenticado y lo limpia al finalizar el request; el filtro de
 * Hibernate y los servicios leen de aca para scoping automatico.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> BUSINESS_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setBusinessId(Long businessId) {
        BUSINESS_ID.set(businessId);
    }

    public static Long getBusinessId() {
        return BUSINESS_ID.get();
    }

    /**
     * Igual que getBusinessId pero falla si no hay contexto: se usa en rutas
     * autenticadas donde operar sin negocio seria un bug.
     */
    public static Long requireBusinessId() {
        Long businessId = BUSINESS_ID.get();
        if (businessId == null) {
            throw new IllegalStateException("No hay un negocio autenticado para esta peticion");
        }
        return businessId;
    }

    public static void clear() {
        BUSINESS_ID.remove();
    }
}
