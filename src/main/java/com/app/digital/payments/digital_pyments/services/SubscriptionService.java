package com.app.digital.payments.digital_pyments.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Plan;
import com.app.digital.payments.digital_pyments.models.Subscription;
import com.app.digital.payments.digital_pyments.models.SubscriptionEvent;
import com.app.digital.payments.digital_pyments.models.dtos.PlanDto;
import com.app.digital.payments.digital_pyments.models.dtos.SubscribeResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.SubscriptionStatusDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IPlanRepository;
import com.app.digital.payments.digital_pyments.repositories.ISubscriptionEventRepository;
import com.app.digital.payments.digital_pyments.repositories.ISubscriptionRepository;
import com.app.digital.payments.digital_pyments.utils.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final ISubscriptionRepository subscriptionRepository;
    private final IPlanRepository planRepository;
    private final IBusinessRepository businessRepository;
    private final ISubscriptionEventRepository eventRepository;
    private final MercadoPagoService mpService;
    private final ObjectMapper objectMapper;

    public SubscriptionService(
            ISubscriptionRepository subscriptionRepository,
            IPlanRepository planRepository,
            IBusinessRepository businessRepository,
            ISubscriptionEventRepository eventRepository,
            MercadoPagoService mpService,
            ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.businessRepository = businessRepository;
        this.eventRepository = eventRepository;
        this.mpService = mpService;
        this.objectMapper = objectMapper;
    }

    /**
     * Obtiene el estado de la suscripcion del negocio autenticado.
     * Combina datos de la tabla subscription con los del negocio (trial).
     */
    @Transactional
    public SubscriptionStatusDto getSubscriptionStatus() {
        Long businessId = TenantContext.requireBusinessId();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalStateException("El negocio autenticado no existe"));

        Optional<Subscription> optSub = subscriptionRepository.findByBusinessId(businessId);

        SubscriptionStatusDto dto = new SubscriptionStatusDto();
        dto.setTrialEndsAt(business.getTrialEndsAt());
        dto.setBusinessPlanStatus(business.getPlanStatus());

        if (optSub.isPresent()) {
            Subscription sub = optSub.get();

            // Releer con bloqueo de fila: si un webhook concurrente acaba de
            // actualizar la suscripcion, tomamos su estado en vez de pisarlo
            if ("PENDING".equals(sub.getStatus()) && sub.getMpPreapprovalId() != null) {
                try {
                    sub = subscriptionRepository.findByMpPreapprovalIdForUpdate(sub.getMpPreapprovalId()).orElse(sub);
                } catch (Exception e) {
                    log.warn("No se pudo bloquear la suscripcion {} para reconciliar: {}",
                            sub.getMpPreapprovalId(), e.getMessage());
                }
            }

            // Reconciliar con MP si el estado local sigue en PENDING (webhook pudo perderse)
            if ("PENDING".equals(sub.getStatus()) && sub.getMpPreapprovalId() != null) {
                try {
                    Map<String, Object> mpPreapproval = mpService.getPreapproval(sub.getMpPreapprovalId());
                    String mpStatus = (String) mpPreapproval.get("status");
                    String newStatus = mapMpStatus(mpStatus);
                    if (!newStatus.equals(sub.getStatus())) {
                        log.info("Reconciliacion: preapproval {} cambio de {} a {} (MP: {})",
                                sub.getMpPreapprovalId(), sub.getStatus(), newStatus, mpStatus);
                        sub.setStatus(newStatus);
                        if ("ACTIVE".equals(newStatus)) {
                            Object nextPaymentDate = mpPreapproval.get("next_payment_date");
                            if (nextPaymentDate instanceof String dateStr) {
                                sub.setCurrentPeriodEnd(LocalDate.parse(dateStr.substring(0, 10)));
                            }
                        }
                        aplicarEstadoDelNegocio(sub, newStatus);
                        sub.setUpdatedAt(LocalDateTime.now());
                        subscriptionRepository.save(sub);
                    }
                } catch (Exception e) {
                    log.warn("No se pudo reconciliar con MP: {}", e.getMessage());
                }
            }

            dto.setId(sub.getId());
            dto.setStatus(sub.getStatus());
            dto.setCurrentPeriodStart(sub.getCurrentPeriodStart());
            dto.setCurrentPeriodEnd(sub.getCurrentPeriodEnd());
            dto.setMpPreapprovalId(sub.getMpPreapprovalId());
            dto.setMpInitPoint(sub.getMpInitPoint());
            dto.setLastPaymentFailedAt(sub.getLastPaymentFailedAt());
            dto.setCreatedAt(sub.getCreatedAt());

            if (sub.getPlan() != null) {
                dto.setPlan(toPlanDto(sub.getPlan()));
            }
        } else {
            dto.setStatus(business.getPlanStatus() != null ? business.getPlanStatus() : "TRIALING");
        }

        return dto;
    }

    /**
     * Inicia la suscripcion: crea un preapproval en MP y guarda la referencia.
     */
    @Transactional
    public SubscribeResponseDto subscribe(String payerEmail) {
        Long businessId = TenantContext.requireBusinessId();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalStateException("El negocio autenticado no existe"));

        Plan plan = planRepository.findFirstByActiveTrueOrderById()
                .orElseThrow(() -> new IllegalStateException("No hay un plan activo disponible"));

        // Evitar duplicados: si ya hay un subscription pendiente con init_point
        Optional<Subscription> existing = subscriptionRepository.findByBusinessId(businessId);
        if (existing.isPresent()) {
            Subscription sub = existing.get();
            if ("PENDING".equals(sub.getStatus()) && sub.getMpInitPoint() != null) {
                return new SubscribeResponseDto(sub.getMpInitPoint(), sub.getMpPreapprovalId(), sub.getStatus());
            }
            if ("ACTIVE".equals(sub.getStatus())) {
                throw new RuntimeException("La suscripcion ya esta activa");
            }
            if ("PAUSED".equals(sub.getStatus())) {
                throw new RuntimeException("La suscripcion esta pausada. Usa 'Reactivar suscripcion' para continuar");
            }
        }

        try {
            Map<String, Object> mpResponse = mpService.createPreapproval(
                    payerEmail, plan.getPrice(), plan.getCurrency());

            String preapprovalId = (String) mpResponse.get("id");
            String initPoint = (String) mpResponse.get("init_point");
            String mpStatus = (String) mpResponse.get("status");

            LocalDate now = LocalDate.now();
            LocalDate periodEnd = now.plusMonths(1);

            Subscription sub;
            if (existing.isPresent()) {
                sub = existing.get();
                sub.setPlan(plan);
                sub.setMpPreapprovalId(preapprovalId);
                sub.setMpInitPoint(initPoint);
                sub.setStatus("PENDING");
                sub.setCurrentPeriodStart(now);
                sub.setCurrentPeriodEnd(periodEnd);
                sub.setUpdatedAt(LocalDateTime.now());
            } else {
                sub = new Subscription();
                sub.setBusiness(business);
                sub.setPlan(plan);
                sub.setMpPreapprovalId(preapprovalId);
                sub.setMpInitPoint(initPoint);
                sub.setStatus("PENDING");
                sub.setCurrentPeriodStart(now);
                sub.setCurrentPeriodEnd(periodEnd);
                sub.setCreatedAt(LocalDateTime.now());
                sub.setUpdatedAt(LocalDateTime.now());
            }

            subscriptionRepository.save(sub);

            log.info("Suscripcion creada para negocio {}: preapproval {}", businessId, preapprovalId);

            return new SubscribeResponseDto(initPoint, preapprovalId, mpStatus);

        } catch (HttpStatusCodeException e) {
            String mpBody = e.getResponseBodyAsString();
            log.error("MP /preapproval failed. status={}, body={}", e.getStatusCode(), mpBody);

            String message;
            if (mpBody != null && !mpBody.isBlank()) {
                try {
                    Map<String, Object> errorMap = objectMapper.readValue(mpBody, Map.class);
                    Object detail = errorMap.get("message");
                    if (detail == null) detail = errorMap.get("error");
                    if (detail == null) detail = mpBody;
                    message = "Mercado Pago rechazo la solicitud: " + detail;
                } catch (Exception parseEx) {
                    message = "Mercado Pago rechazo la solicitud: " + mpBody;
                }
            } else {
                message = "Mercado Pago rechazo la solicitud (HTTP " + e.getStatusCode().value() + "). Verifica las credenciales y el back_url.";
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);

        } catch (Exception e) {
            log.error("Error inesperado creando suscripción", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperado al crear la suscripcion");
        }
    }

    /**
     * Cancela la suscripcion: no se realizan mas cobros. El acceso se
     * conserva hasta el fin del periodo ya pagado. En MP es definitivo:
     * para volver a suscribirse se crea un preapproval nuevo.
     */
    @Transactional
    public SubscriptionStatusDto cancelSubscription() {
        return cambiarEstadoManual("CANCELLED", "cancelled", "manual:cancel",
                "La suscripcion ya esta cancelada");
    }

    /**
     * Pausa la suscripcion: MP no cobra mientras este pausada. Puede
     * reactivarse sin pasar por el checkout.
     */
    @Transactional
    public SubscriptionStatusDto pauseSubscription() {
        return cambiarEstadoManual("PAUSED", "paused", "manual:pause",
                "Solo se puede pausar una suscripcion activa");
    }

    /** Reactiva una suscripcion pausada. */
    @Transactional
    public SubscriptionStatusDto resumeSubscription() {
        return cambiarEstadoManual("ACTIVE", "authorized", "manual:resume",
                "Solo se puede reactivar una suscripcion pausada");
    }

    private SubscriptionStatusDto cambiarEstadoManual(
            String nuevoEstado, String mpStatus, String evento, String mensajeTransicionInvalida) {
        Long businessId = TenantContext.requireBusinessId();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalStateException("El negocio autenticado no existe"));

        Subscription sub = subscriptionRepository.findByBusinessId(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay una suscripcion para gestionar"));

        if (sub.getMpPreapprovalId() == null || sub.getMpPreapprovalId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La suscripcion no tiene un preapproval de Mercado Pago asociado");
        }

        String estadoAnterior = sub.getStatus();
        boolean transicionValida = switch (nuevoEstado) {
            case "CANCELLED" -> !"CANCELLED".equals(estadoAnterior);
            case "PAUSED" -> "ACTIVE".equals(estadoAnterior);
            case "ACTIVE" -> "PAUSED".equals(estadoAnterior);
            default -> false;
        };
        if (!transicionValida) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensajeTransicionInvalida);
        }

        try {
            mpService.updatePreapprovalStatus(sub.getMpPreapprovalId(), mpStatus);
        } catch (HttpStatusCodeException e) {
            String mpBody = e.getResponseBodyAsString();
            log.warn("MP rechazo el cambio de estado {}: status={}, body={}", mpStatus, e.getStatusCode(), mpBody);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mercado Pago rechazo la solicitud: " + (mpBody != null && !mpBody.isBlank()
                            ? mpBody : "HTTP " + e.getStatusCode().value()));
        } catch (Exception e) {
            log.error("Error inesperado cambiando el estado de la suscripcion a {}", mpStatus, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error inesperado al actualizar la suscripcion en Mercado Pago");
        }

        sub.setStatus(nuevoEstado);
        sub.setUpdatedAt(LocalDateTime.now());
        // Una accion manual invalida el aviso de cobro rechazado
        sub.setLastPaymentFailedAt(null);
        aplicarEstadoDelNegocio(sub, nuevoEstado);
        subscriptionRepository.save(sub);

        // Evento de auditoria (ademas sirve para trazabilidad junto a los webhooks)
        SubscriptionEvent event = new SubscriptionEvent();
        event.setSubscription(sub);
        event.setEvent(evento);
        event.setMpId(sub.getMpPreapprovalId());
        event.setData("{\"manual\":true,\"status\":\"" + mpStatus + "\"}");
        event.setCreatedAt(LocalDateTime.now());
        eventRepository.save(event);

        log.info("Suscripcion {} del negocio {}: {} -> {}",
                evento, businessId, estadoAnterior, nuevoEstado);

        return getSubscriptionStatus();
    }

    /**
     * Procesa un webhook de Mercado Pago. Cada topic notifica un recurso
     * distinto y su data.id es el id de ESE recurso (no siempre un preapproval):
     * - subscription_preapproval        → data.id es el preapproval.
     * - subscription_authorized_payment → data.id es una factura; se resuelve
     *   el preapproval_id consultando /authorized_payments/{id}.
     * - payment                         → data.id es un pago; si trae
     *   preapproval_id pertenece a una suscripcion.
     */
    @Transactional
    public void processWebhook(String requestBody, String signatureHeader, String requestId, String dataId, String type) {
        try {
            // Firma (hardening): si hay secreto configurado es obligatoria.
            // Con header x-signature se valida siempre; sin header solo se
            // acepta cuando la firma no es obligatoria (desarrollo sin secreto).
            if (signatureHeader != null && !signatureHeader.isBlank()) {
                if (!mpService.validarFirma(signatureHeader, requestId, dataId, null)) {
                    log.warn("Firma de webhook invalida, ignorando");
                    return;
                }
            } else if (mpService.isSignatureRequired()) {
                log.warn("Webhook sin x-signature ignorado (firma obligatoria): data.id={}, type={}", dataId, type);
                return;
            }

            String mpId = dataId;
            String eventType = type;

            // Si no vino dataId por query param, intentar extraer del body
            if ((mpId == null || mpId.isBlank()) && requestBody != null) {
                Map<String, Object> body = objectMapper.readValue(requestBody, Map.class);
                mpId = extraerDataId(body);
                if (eventType == null || eventType.isBlank()) {
                    Object typeField = body.get("type");
                    eventType = typeField != null ? typeField.toString() : "";
                }
            }

            if (mpId == null || mpId.isBlank()) {
                log.warn("Webhook sin data.id, ignorando");
                return;
            }

            if (eventType == null || eventType.isBlank()) eventType = "unknown";

            // Idempotencia: dedup por notification id (del body) o por mpId+type+status
            String notificationId = null;
            if (requestBody != null) {
                try {
                    Map<String, Object> bodyMap = objectMapper.readValue(requestBody, Map.class);
                    Object notifId = bodyMap.get("id");
                    if (notifId != null) notificationId = notifId.toString();
                } catch (Exception ignored) {}
            }
            String eventKey = notificationId != null
                    ? "notif:" + notificationId
                    : "webhook:" + eventType + ":" + mpId;

            if (eventRepository.findByMpIdAndEvent(mpId, eventKey).isPresent()) {
                log.info("Webhook ya procesado: {}", eventKey);
                return;
            }

            // Resolver el preapproval segun el topic del evento
            String preapprovalId;
            Map<String, Object> paymentInfo = null;

            switch (eventType) {
                case "subscription_authorized_payment" -> {
                    // La factura trae el preapproval_id de la suscripcion
                    Map<String, Object> invoice = mpService.getAuthorizedPayment(mpId);
                    Object pid = invoice != null ? invoice.get("preapproval_id") : null;
                    if (pid == null) {
                        log.warn("Factura {} sin preapproval_id, ignorando", mpId);
                        return;
                    }
                    paymentInfo = invoice;
                    preapprovalId = pid.toString();
                }
                case "payment" -> {
                    Map<String, Object> payment = mpService.getPayment(mpId);
                    Object pid = payment != null ? payment.get("preapproval_id") : null;
                    if (pid == null) {
                        log.info("Pago {} no pertenece a una suscripcion, ignorando", mpId);
                        return;
                    }
                    paymentInfo = payment;
                    preapprovalId = pid.toString();
                }
                case "subscription_preapproval", "preapproval", "unknown" -> preapprovalId = mpId;
                default -> {
                    log.info("Topic '{}' no soportado, ignorando (data.id={})", eventType, mpId);
                    return;
                }
            }

            // Buscar la suscripcion local con bloqueo de fila: serializa el
            // leer-modificar-guardar si llegan dos webhooks de la misma suscripcion
            Optional<Subscription> optSub = subscriptionRepository.findByMpPreapprovalIdForUpdate(preapprovalId);
            if (optSub.isEmpty()) {
                log.warn("Webhook para suscripcion {} sin suscripcion local", preapprovalId);
                return;
            }

            // Re-verificar idempotencia bajo bloqueo: otro webhook identico pudo
            // haberse procesado mientras este esperaba el bloqueo
            if (eventRepository.findByMpIdAndEvent(mpId, eventKey).isPresent()) {
                log.info("Webhook ya procesado bajo bloqueo: {}", eventKey);
                return;
            }

            Subscription sub = optSub.get();
            String oldStatus = sub.getStatus();

            // 1) Resincronizar estado con el preapproval actual en MP
            String newStatus = null;
            Map<String, Object> eventData = paymentInfo;
            try {
                Map<String, Object> mpPreapproval = mpService.getPreapproval(preapprovalId);
                eventData = mpPreapproval;
                newStatus = mapMpStatus((String) mpPreapproval.get("status"));
                if ("ACTIVE".equals(newStatus)) {
                    // Actualizar periodo: desde la respuesta de MP
                    Object nextPaymentDate = mpPreapproval.get("next_payment_date");
                    if (nextPaymentDate instanceof String dateStr) {
                        sub.setCurrentPeriodEnd(LocalDate.parse(dateStr.substring(0, 10)));
                    } else {
                        sub.setCurrentPeriodEnd(LocalDate.now().plusMonths(1));
                    }
                }
            } catch (Exception e) {
                log.warn("No se pudo consultar el preapproval {} en MP: {}", preapprovalId, e.getMessage());
            }

            // 2) Extender el periodo cuando el pago de la suscripcion esta aprobado
            if (isPagoAprobado(paymentInfo)) {
                LocalDate fechaPago = extractFechaPago(paymentInfo);
                if (fechaPago != null) {
                    sub.setCurrentPeriodStart(fechaPago);
                    sub.setCurrentPeriodEnd(fechaPago.plusMonths(1));
                }
                if (newStatus == null) newStatus = "ACTIVE";
                // Cobro aprobado: el aviso de renovacion rechazada ya no aplica
                sub.setLastPaymentFailedAt(null);
            } else if (isPagoRechazado(paymentInfo)) {
                // Cobro de renovacion rechazado: marcar para avisar al usuario.
                // El acceso no se corta: sigue hasta el fin del periodo pagado.
                log.warn("Cobro de renovacion rechazado para la suscripcion {}", preapprovalId);
                sub.setLastPaymentFailedAt(LocalDateTime.now());
            }

            if (newStatus == null) {
                // Ni cambio de estado ni pago nuevo: no hay nada que persistir
                if (eventData == null) return;
                newStatus = oldStatus;
            }
            sub.setStatus(newStatus);

            aplicarEstadoDelNegocio(sub, newStatus);

            sub.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(sub);

            // Registrar evento para idempotencia
            SubscriptionEvent event = new SubscriptionEvent();
            event.setSubscription(sub);
            event.setEvent(eventKey);
            event.setMpId(mpId);
            event.setData(objectMapper.writeValueAsString(eventData));
            event.setCreatedAt(LocalDateTime.now());
            eventRepository.save(event);

            log.info("Webhook procesado: suscripcion {} ({}) cambio de {} a {} (event: {})",
                    preapprovalId, eventType, oldStatus, newStatus, eventKey);

        } catch (PessimisticLockingFailureException e) {
            // No romper el webhook: MP reintenta solo y la proxima vez tomara el bloqueo
            log.warn("No se pudo bloquear la suscripcion del webhook (data.id={}, type={}): {}",
                    dataId, type, e.getMessage());
        } catch (HttpStatusCodeException e) {
            log.warn("MP respondio {} al procesar webhook (data.id={}, type={}): {}",
                    e.getStatusCode(), dataId, type, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error al procesar webhook de MP", e);
        }
    }

    /**
     * Verifica si el negocio autenticado tiene acceso activo a la aplicacion.
     * Politica (SaaS estandar): ACTIVE da acceso inmediato; CANCELLED,
     * PAUSED y BLOCKED conservan el acceso hasta el fin del periodo ya
     * pagado (Business.subscriptionUntil). Ademas vale el trial vigente.
     */
    public boolean hasActiveAccess(Long businessId) {
        LocalDate hoy = LocalDate.now();

        // Suscripcion activa
        Optional<Subscription> optSub = subscriptionRepository.findByBusinessId(businessId);
        if (optSub.isPresent()) {
            String status = optSub.get().getStatus();
            if ("ACTIVE".equals(status)) return true;
        }

        Optional<Business> optBiz = businessRepository.findById(businessId);
        if (optBiz.isPresent()) {
            Business biz = optBiz.get();

            // Periodo pagado vigente (lo mueven solo los cobros aprobados)
            LocalDate subscriptionUntil = biz.getSubscriptionUntil();
            if (subscriptionUntil != null && !subscriptionUntil.isBefore(hoy)) {
                return true;
            }

            // Trial
            LocalDate trialEndsAt = biz.getTrialEndsAt();
            if (trialEndsAt != null && !trialEndsAt.isBefore(hoy)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Politica de acceso (SaaS estandar): ACTIVE da acceso inmediato;
     * CANCELLED, PAUSED y BLOCKED conservan el acceso hasta el fin del
     * periodo ya pagado. subscriptionUntil solo lo mueven los cobros
     * aprobados: aqui nunca se extiende para no regalar tiempo.
     */
    private void aplicarEstadoDelNegocio(Subscription sub, String newStatus) {
        Business business = sub.getBusiness();
        switch (newStatus) {
            case "ACTIVE" -> {
                business.setPlanStatus("ACTIVE");
                business.setSubscriptionUntil(sub.getCurrentPeriodEnd());
            }
            case "CANCELLED", "PAUSED", "BLOCKED" ->
                business.setPlanStatus(newStatus);
            default -> {
                // PENDING u otros: sin cambios en el plan del negocio
            }
        }
        businessRepository.save(business);
    }

    private String mapMpStatus(String mpStatus) {
        if (mpStatus == null) return "BLOCKED";
        return switch (mpStatus) {
            case "authorized" -> "ACTIVE";
            case "pending" -> "PENDING";
            case "cancelled" -> "CANCELLED";
            case "paused" -> "PAUSED";
            default -> "BLOCKED";
        };
    }

    /** El cobro de la suscripcion esta aprobado (factura o pago de MP). */
    private boolean isPagoAprobado(Map<String, Object> paymentInfo) {
        if (paymentInfo == null) return false;
        // Factura (authorized_payments): payment.status del cobro asociado
        if (paymentInfo.get("payment") instanceof Map<?, ?> payment) {
            return "approved".equals(payment.get("status"));
        }
        // Pago (/v1/payments): status del propio pago
        return "approved".equals(paymentInfo.get("status"));
    }

    /** El cobro de la suscripcion fue rechazado (factura o pago de MP). */
    private boolean isPagoRechazado(Map<String, Object> paymentInfo) {
        if (paymentInfo == null) return false;
        if (paymentInfo.get("payment") instanceof Map<?, ?> payment) {
            return "rejected".equals(payment.get("status"));
        }
        return "rejected".equals(paymentInfo.get("status"));
    }

    /** Fecha de cobro: debit_date en facturas, date_approved en pagos. */
    private LocalDate extractFechaPago(Map<String, Object> paymentInfo) {
        if (paymentInfo == null) return null;
        Object fecha = paymentInfo.get("debit_date");
        if (fecha == null) fecha = paymentInfo.get("date_approved");
        if (fecha == null) fecha = paymentInfo.get("date_created");
        if (fecha instanceof String dateStr && dateStr.length() >= 10) {
            try {
                return LocalDate.parse(dateStr.substring(0, 10));
            } catch (Exception ignored) {
                log.warn("Fecha de pago no reconocida: {}", dateStr);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extraerDataId(Map<String, Object> body) {
        try {
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            if (data != null && data.get("id") != null) {
                return data.get("id").toString();
            }
            // Fallback: el id puede venir directamente
            if (body.get("id") != null) {
                return body.get("id").toString();
            }
        } catch (Exception e) {
            log.warn("No se pudo extraer data.id del webhook", e);
        }
        return null;
    }

    private PlanDto toPlanDto(Plan plan) {
        PlanDto dto = new PlanDto();
        dto.setId(plan.getId());
        dto.setName(plan.getName());
        dto.setDescription(plan.getDescription());
        dto.setPrice(plan.getPrice());
        dto.setCurrency(plan.getCurrency());
        dto.setTrialDays(plan.getTrialDays());
        return dto;
    }
}