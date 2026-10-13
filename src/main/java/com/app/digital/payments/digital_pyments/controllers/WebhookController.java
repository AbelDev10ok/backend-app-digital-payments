package com.app.digital.payments.digital_pyments.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.services.SubscriptionService;

/**
 * Endpoint publico para webhooks de Mercado Pago.
 * Recibe notificaciones de cambios de estado en preapprovals (suscripciones).
 * La validacion de firma se hace en SubscriptionService -> MercadoPagoService.
 */
@RestController
@RequestMapping("/api/webhooks/mercadopago")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final SubscriptionService subscriptionService;

    public WebhookController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody(required = false) String body,
            @RequestHeader(value = "x-signature", required = false) String signatureHeader,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestParam(value = "type", required = false) String type) {

        log.info("Webhook POST recibido de MP. type={}, data.id={}, x-request-id={}", type, dataId, requestId);

        subscriptionService.processWebhook(body, signatureHeader, requestId, dataId, type);

        // MP espera un 200 para dejar de intentar
        return ResponseEntity.ok("OK");
    }

    @GetMapping
    public ResponseEntity<String> handleWebhookGet(
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "data.id", required = false) String dataId) {

        log.info("Webhook GET (IPN probe) recibido de MP. topic={}, id={}, type={}, data.id={}", topic, id, type, dataId);

        // MP envia GET como validacion de la URL. Responder 200 y procesar si hay datos.
        // En IPN el tipo del evento viene en "topic"; en webhooks, en "type".
        if (dataId != null || id != null) {
            String mpId = dataId != null ? dataId : id;
            String eventType = (type != null && !type.isBlank()) ? type : topic;
            subscriptionService.processWebhook(null, null, null, mpId, eventType);
        }

        return ResponseEntity.ok("OK");
    }
}