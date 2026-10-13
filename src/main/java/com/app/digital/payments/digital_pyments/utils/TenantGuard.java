package com.app.digital.payments.digital_pyments.utils;

/**
 * Guardia de tenancy para accesos por ID: los filtros de Hibernate no aplican a
 * findById(), por lo que toda entidad obtenida por ID debe verificar que
 * pertenezca al negocio autenticado antes de usarla o devolverla.
 */
public final class TenantGuard {

    private TenantGuard() {
    }

    /**
     * Verifica que el businessId de la entidad coincida con el negocio
     * autenticado. Si no coincide responde "Recurso no encontrado" (mismo
     * mensaje generico, no filtra la existencia de recursos ajenos).
     * Un businessId nulo en la entidad se tolera (datos legados/test): la
     * verificacion estricta aplica solo cuando la entidad tiene negocio.
     */
    public static void verificar(Long businessIdDeLaEntidad) {
        if (businessIdDeLaEntidad == null) {
            return;
        }
        Long businessIdActual = TenantContext.requireBusinessId();
        if (!businessIdActual.equals(businessIdDeLaEntidad)) {
            throw new RuntimeException("Recurso no encontrado");
        }
    }
}
