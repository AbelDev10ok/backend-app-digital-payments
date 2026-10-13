package com.app.digital.payments.digital_pyments.configuration;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.app.digital.payments.digital_pyments.services.MercadoPagoService;

@Component
public class WebhookRegistrar implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WebhookRegistrar.class);

    private final MercadoPagoService mpService;

    @Value("${app.webhook-url:}")
    private String webhookUrl;

    public WebhookRegistrar(MercadoPagoService mpService) {
        this.mpService = mpService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("app.webhook-url no configurado, no se registra webhook en MP");
            return;
        }

        if (!mpService.isConfigured()) {
            log.warn("Mercado Pago no configurado, no se registra webhook");
            return;
        }

        try {
            log.info("Registrando webhook en MP: {}", webhookUrl);
            List<String> topics = List.of("subscription_preapproval", "payment", "subscription_authorized_payment");
            Map<String, Object> result = mpService.registerWebhook(webhookUrl, topics);
            log.info("Webhook registrado exitosamente: {}", result);
        } catch (Exception e) {
            log.warn("No se pudo registrar webhook en MP (puede ya existir): {}", e.getMessage());
        }
    }
}
