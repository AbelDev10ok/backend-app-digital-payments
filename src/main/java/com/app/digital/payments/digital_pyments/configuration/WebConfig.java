package com.app.digital.payments.digital_pyments.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registro MVC: el interceptor de tenancy corre en preHandle de todos los
 * handlers. Es un WebRequestInterceptor (subclase del OSIV de Boot), por lo
 * que se registra con addWebRequestInterceptor; su super.preHandle abre el
 * EntityManager si todavia no existe, sin depender del orden de registro.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TenantFilterInterceptor tenantFilterInterceptor;

    public WebConfig(TenantFilterInterceptor tenantFilterInterceptor) {
        this.tenantFilterInterceptor = tenantFilterInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addWebRequestInterceptor(tenantFilterInterceptor).addPathPatterns("/**");
    }
}
