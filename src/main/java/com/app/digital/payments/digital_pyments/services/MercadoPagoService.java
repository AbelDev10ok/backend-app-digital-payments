package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cliente de la API de Mercado Pago.
 * Opera con el recurso /preapproval (suscripciones recurrentes).
 * Nunca manejamos tarjetas: el checkout es hosted en MP.
 */
@Service
public class MercadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoService.class);

    private static final String MP_API_BASE = "https://api.mercadopago.com";

    private final String accessToken;
    private final String webhookSecret;
    private final String frontendUrl;
    private final boolean webhookRequireSignature;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MercadoPagoService(
            @Value("${app.mercadopago.access-token:}") String accessToken,
            @Value("${app.mercadopago.webhook-secret:}") String webhookSecret,
            @Value("${app.frontend-url:https://numerate-crumpet-oxymoron.ngrok-free.dev}") String frontendUrl,
            @Value("${app.mercadopago.webhook-require-signature:true}") boolean webhookRequireSignature,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.accessToken = accessToken;
        this.webhookSecret = webhookSecret;
        this.frontendUrl = frontendUrl;
        this.webhookRequireSignature = webhookRequireSignature;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Crea una suscripcion (preapproval) en Mercado Pago.
     * Devuelve el init_point para redirigir al checkout hosted.
     */
    public Map<String, Object> createPreapproval(String payerEmail, BigDecimal amount, String currencyId)
            throws Exception {

        if (!isConfigured()) {
            throw new RuntimeException("Mercado Pago no configurado: falta ACCESS_TOKEN");
        }

        // Normalizar email de usuarios de prueba de MP: TESTUSER<n> → test_user_<n>@testuser.com
        String mpPayerEmail = payerEmail;
        if (mpPayerEmail != null && mpPayerEmail.startsWith("TESTUSER")) {
            mpPayerEmail = mpPayerEmail.split("@")[0]
                    .replace("TESTUSER", "test_user_") + "@testuser.com";
        }

        String url = MP_API_BASE + "/preapproval";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Content-Type", "application/json");

        String endDate = OffsetDateTime.now(ZoneOffset.UTC)
                .plusYears(5)
                .truncatedTo(ChronoUnit.MILLIS)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Map<String, Object> autoRecurring = Map.of(
                "frequency", 1,
                "frequency_type", "months",
                "transaction_amount", amount,
                "currency_id", currencyId,
                "end_date", endDate);


        Map<String, Object> body = Map.of(
                "reason", "Cobros&Ventas - Plan Mensual",
                  "external_reference", "SUB-"+UUID.randomUUID(),
                "auto_recurring", autoRecurring,
                "back_url", frontendUrl + "/dashboard/suscripcion",
                "payer_email", mpPayerEmail,
                "status", "pending");

        String jsonBody = objectMapper.writeValueAsString(body);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        Map<String, Object> logBody = new HashMap<>(body);
        logBody.put("payer_email", payerEmail);
        log.info("Enviando POST {} a Mercado Pago. JSON enviado (payer_email enmascarado): {}",
                url, objectMapper.writeValueAsString(logBody));

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> errorBody = response.getBody();
            String detail = errorBody != null ? errorBody.toString() : "sin detalle";
            throw new RuntimeException(
                    "Error al crear la suscripcion en Mercado Pago: HTTP "
                            + response.getStatusCode() + " - " + detail);
        }

        return response.getBody();
    }

    /**
     * Obtiene el estado actual de un preapproval desde MP.
     */
    public Map<String, Object> getPreapproval(String preapprovalId) throws Exception {
        String url = MP_API_BASE + "/preapproval/" + preapprovalId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(
                    "Error al consultar preapproval en MP: HTTP " + response.getStatusCode());
        }

        return response.getBody();
    }

    /**
     * Cambia el estado de un preapproval en MP (gestion de suscripcion).
     * Valores permitidos por MP: cancelled (cancelar), paused (pausar) y
     * authorized (reanudar una suscripcion pausada).
     */
    public Map<String, Object> updatePreapprovalStatus(String preapprovalId, String status) throws Exception {
        if (!isConfigured()) {
            throw new RuntimeException("Mercado Pago no configurado: falta ACCESS_TOKEN");
        }

        String url = MP_API_BASE + "/preapproval/" + preapprovalId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Content-Type", "application/json");

        String jsonBody = objectMapper.writeValueAsString(Map.of("status", status));
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        log.info("Enviando PUT {} a Mercado Pago (nuevo estado: {})", url, status);
        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.PUT, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> errorBody = response.getBody();
            String detail = errorBody != null ? errorBody.toString() : "sin detalle";
            throw new RuntimeException(
                    "Error al actualizar el preapproval en Mercado Pago: HTTP "
                            + response.getStatusCode() + " - " + detail);
        }

        return response.getBody();
    }

    /**
     * Obtiene una factura (pago agendado) de una suscripcion desde MP.
     * Topic subscription_authorized_payment: el data.id de la notificacion es
     * el id de la factura, NO del preapproval. La respuesta incluye
     * preapproval_id, debit_date y el pago asociado.
     */
    public Map<String, Object> getAuthorizedPayment(String authorizedPaymentId) throws Exception {
        return getMpResource(MP_API_BASE + "/authorized_payments/" + authorizedPaymentId, "factura de suscripcion");
    }

    /**
     * Obtiene un pago desde MP (topic payment). Si el pago pertenece a una
     * suscripcion, la respuesta incluye preapproval_id.
     */
    public Map<String, Object> getPayment(String paymentId) throws Exception {
        return getMpResource(MP_API_BASE + "/v1/payments/" + paymentId, "pago");
    }

    private Map<String, Object> getMpResource(String url, String resourceLabel) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(
                    "Error al consultar " + resourceLabel + " en MP: HTTP " + response.getStatusCode());
        }

        return response.getBody();
    }

    public boolean isConfigured() {
        return accessToken != null && !accessToken.isBlank();
    }

    /** True si hay secreto de webhook configurado. */
    public boolean isWebhookSecretConfigured() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    /**
     * La firma x-signature es obligatoria cuando hay secreto configurado
     * (hardening: sin secreto no hay forma de autenticar el origen). El flag
     * app.mercadopago.webhook-require-signature permite desactivarlo en
     * desarrollo.
     */
    public boolean isSignatureRequired() {
        return webhookRequireSignature && isWebhookSecretConfigured();
    }

    /**
     * Valida la firma x-signature de un webhook de MP.
     * Formato header: ts=<timestamp>,v1=<hmac>
     * Manifest oficial MP: id:{data.id};request-id:{x-request-id};ts:{ts};
     */
    public boolean validarFirma(String signatureHeader, String requestId, String dataId, String ts) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Webhook secret no configurado, saltando validacion de firma");
            return true;
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        try {
            String headerTs = null;
            String v1 = null;
            for (String part : signatureHeader.split(",")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) {
                    if ("ts".equals(kv[0].trim())) headerTs = kv[1].trim();
                    if ("v1".equals(kv[0].trim())) v1 = kv[1].trim();
                }
            }
            if (headerTs == null || v1 == null) return false;

            // Usar ts del header si no viene como parametro
            if (ts == null || ts.isBlank()) ts = headerTs;

            // Manifest oficial de MP: id:{data.id};request-id:{x-request-id};ts:{ts};
            String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
            String expected = hmacSha256(webhookSecret, manifest);

            boolean valid = expected.equals(v1);
            if (!valid) {
                log.warn("Firma invalida. manifest={}, expected={}, got={}", manifest, expected, v1);
            }
            return valid;
        } catch (Exception e) {
            log.warn("Error al validar firma del webhook", e);
            return false;
        }
    }

    private String hmacSha256(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes("UTF-8"), "HmacSHA256");
        mac.init(keySpec);
        byte[] result = mac.doFinal(data.getBytes("UTF-8"));
        StringBuilder hex = new StringBuilder();
        for (byte b : result) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /**
     * Registra un webhook en Mercado Pago para recibir notificaciones de preapproval.
     * Usa el endpoint /webhooks de la API de MP.
     */
    public Map<String, Object> registerWebhook(String webhookUrl, List<String> topics) throws Exception {
        if (!isConfigured()) {
            throw new RuntimeException("Mercado Pago no configurado: falta ACCESS_TOKEN");
        }

        String url = MP_API_BASE + "/webhooks";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Content-Type", "application/json");

        Map<String, Object> body = Map.of(
                "url", webhookUrl,
                "topics", topics
        );
        String jsonBody = objectMapper.writeValueAsString(body);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        log.info("Registrando webhook en MP: {}", webhookUrl);
        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> errorBody = response.getBody();
            String detail = errorBody != null ? errorBody.toString() : "sin detalle";
            throw new RuntimeException(
                    "Error al registrar webhook en Mercado Pago: HTTP "
                            + response.getStatusCode() + " - " + detail);
        }

        return response.getBody();
    }

    /**
     * Lista los webhooks registrados en Mercado Pago.
     */
    public List<Map<String, Object>> listWebhooks() throws Exception {
        if (!isConfigured()) {
            throw new RuntimeException("Mercado Pago no configurado: falta ACCESS_TOKEN");
        }

        String url = MP_API_BASE + "/webhooks";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, List.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(
                    "Error al listar webhooks en MP: HTTP " + response.getStatusCode());
        }

        return response.getBody();
    }

    /**
     * Elimina un webhook por su ID.
     */
    public void deleteWebhook(String webhookId) throws Exception {
        if (!isConfigured()) {
            throw new RuntimeException("Mercado Pago no configurado: falta ACCESS_TOKEN");
        }

        String url = MP_API_BASE + "/webhooks/" + webhookId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                url, HttpMethod.DELETE, entity, Void.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(
                    "Error al eliminar webhook en MP: HTTP " + response.getStatusCode());
        }

        log.info("Webhook {} eliminado", webhookId);
    }
}