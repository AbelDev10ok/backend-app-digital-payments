package com.app.digital.payments.digital_pyments.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;

import com.app.digital.payments.digital_pyments.utils.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Activa el filtro de tenancy de Hibernate ("tenantFilter") sobre la sesion
 * del request ANTES de que corra cualquier controller/service.
 *
 * Extiende OpenEntityManagerInViewInterceptor para GARANTIZAR el orden: en
 * preHandle, super abre (o reusa) el EntityManager del request y recien ahi
 * se habilita el filtro. Activarlo desde la cadena de filtros de seguridad o
 * desde un interceptor plano no sirve: la sesion todavia no existe y
 * unwrap() resuelve una sesion efimera desechable (bug de la Fase 11).
 *
 * Al ser una subclase de OpenEntityManagerInViewInterceptor satisface el
 * @ConditionalOnMissingBean de Boot, asi esta es la UNICA instancia de OSIV
 * registrada: abre el EM al inicio y lo cierra en afterCompletion (heredado).
 *
 * Fail-closed: si hay negocio autenticado y no se pudo activar el filtro, se
 * corta el request (mejor un error que filtrar datos de otros negocios).
 */
@Component
public class TenantFilterInterceptor extends OpenEntityManagerInViewInterceptor {

    private static final String FILTER_NAME = "tenantFilter";

    private static final Logger log = LoggerFactory.getLogger(TenantFilterInterceptor.class);

    private final EntityManagerFactory entityManagerFactory;

    public TenantFilterInterceptor(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void preHandle(WebRequest request) {
        super.preHandle(request);
        Long businessId = TenantContext.getBusinessId();
        if (businessId == null) {
            // Rutas publicas (login, register, verify): no hay tenant que aplicar
            return;
        }

        EntityManagerHolder holder = (EntityManagerHolder) TransactionSynchronizationManager
                .getResource(entityManagerFactory);
        if (!(holder instanceof EntityManagerHolder) || holder.getEntityManager() == null) {
            throw new IllegalStateException("No hay EntityManager del request para aplicar tenancy");
        }

        log.info("TENANCY preHandle uri={} businessId={}", getDescription(request), businessId);

        EntityManager em = holder.getEntityManager();
        org.hibernate.Session session = em.unwrap(org.hibernate.Session.class);
        try {
            if (session.getEnabledFilter(FILTER_NAME) == null) {
                session.enableFilter(FILTER_NAME).setParameter("businessId", businessId);
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "No se pudo aplicar el aislamiento del negocio autenticado", e);
        }
    }

    @Override
    public void postHandle(WebRequest request, ModelMap model) {
        super.postHandle(request, model);
    }

    private String getDescription(WebRequest request) {
        Object descripcion = request.getAttribute("jakarta.servlet.error.request_uri", WebRequest.SCOPE_REQUEST);
        return descripcion != null ? descripcion.toString() : "request";
    }
}
