# Mercado Pago — Configuración de Suscripciones y Producción

> Estado: las credenciales de MP se leen **solo de variables de entorno** (no hay
> valores en el repo). Falta cargar el token de **producción** de la Cuenta
> Negocio y el secreto del webhook. La reconciliación (polling) funciona como
> fallback mientras el webhook no esté configurado.

---

## 1. Arquitectura del flujo de suscripciones

```
┌──────────┐     POST /api/billing/subscribe     ┌──────────────┐
│ Frontend │ ────────────────────────────────────→│   Backend    │
│  React   │                                      │ Spring Boot  │
└──────────┘                                      └──────┬───────┘
                                                         │ POST /preapproval
                                                         ▼
                                                  ┌──────────────┐
                                                  │ Mercado Pago │
                                                  └──────┬───────┘
                                                         │
                          ┌──────────────────────────────┼──────────────────┐
                          │  WEBHOOK (producción)        │  RECONCILIACIÓN  │
                          │  MP notifica al backend      │  Backend consulta│
                          │  cuando pasa algo            │  MP al pedir     │
                          ▼                              ▼  estado          │
                   ┌──────────────┐              ┌──────────────┐           │
                   │   Backend    │              │   Backend    │           │
                   │  /api/webhooks│              │  getSubscriptionStatus  │
                   │  /mercadopago│              │  (polling)   │           │
                   └──────────────┘              └──────────────┘           │
                          │                              │                  │
                          └──────────┬───────────────────┘                  │
                                     ▼                                      │
                              ┌──────────────┐                              │
                              │  PostgreSQL  │  status: PENDING → ACTIVE    │
                              └──────────────┘                              │
```

### Estados de la suscripción

| Estado | Significado | Quién lo cambia |
|--------|-------------|-----------------|
| `PENDING` | Suscripción creada, esperando pago | `subscribe()` al crear |
| `ACTIVE` | Pago confirmado | Webhook o reconciliación |
| `PAUSED` | Pausada por el usuario (sin cobros hasta reactivar) | App (`POST .../subscription/pause`) o API de MP |
| `CANCELLED` | Cancelada por usuario o admin | App (`POST .../subscription/cancel`) o API de MP |
| `BLOCKED` | Cobro de renovación rechazado (MP bloquea tras 3 intentos) | Webhook |

**Política de acceso (SaaS estándar):** `ACTIVE` da acceso inmediato; `PAUSED`,
`CANCELLED` y `BLOCKED` conservan el acceso **hasta el fin del periodo ya pagado**
(`business.subscriptionUntil`, que solo mueven los cobros aprobados). Al vencer,
`hasActiveAccess` deniega y el frontend redirige a `/dashboard/suscripcion`.

### Renovación rechazada

Si MP rechaza el cobro de una renovación (tarjeta vencida, fondos insuficientes),
**reintenta automáticamente y tras 3 intentos cancela la suscripción**. El backend
marca `lastPaymentFailedAt` en la suscripción al recibir el webhook de pago
`rejected` (sin tocar el periodo: el acceso sigue hasta el fin del pagado), y la
UI muestra un aviso con link a la página de suscripciones de MP. Un cobro aprobado
o una acción manual del usuario limpia el aviso; si MP cancela tras los rechazos,
la UI lo explica y ofrece volver a suscribirse (con el nuevo cobro por el mes en
curso).

### Reconciliación (fallback)

Cuando el usuario consulta el estado (`GET /api/billing/subscription`), si el estado local es `PENDING`, el backend **pregunta directamente a MP** y actualiza la BD. Esto permite que la app funcione **sin webhook**.

---

## 2. Variables de entorno necesarias

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `APP_MERCADOPAGO_ACCESS_TOKEN` | Token de acceso de MP | `APP_USR-...` (el prefijo no distingue test de prod: lo define la cuenta) |
| `APP_MERCADOPAGO_WEBHOOK_SECRET` | Secreto de firma del webhook | Se genera en el panel de MP |
| `APP_WEBHOOK_URL` | URL pública del backend | `https://tudominio.com/api/webhooks/mercadopago` |
| `APP_FRONTEND_URL` | URL del frontend (para back_url) | `https://tudominio.com` |
| `DB_PASSWORD` | Password de PostgreSQL | |

### En `application.properties`

```properties
app.mercadopago.access-token=${APP_MERCADOPAGO_ACCESS_TOKEN:}
app.mercadopago.webhook-secret=${APP_MERCADOPAGO_WEBHOOK_SECRET:}
app.webhook-url=${APP_WEBHOOK_URL:}
app.frontend-url=${APP_FRONTEND_URL:http://localhost:5173}
```

En desarrollo los valores pueden quedar vacíos (las suscripciones quedan
deshabilitadas y `WebhookRegistrar` avisa en el log). En producción
(`application-prod.properties`) las credenciales de MP no tienen default:
si faltan, la app no arranca.

**Nunca** commitear credenciales en estos archivos. El repositorio es público.

---

## 3. Configuración en el panel de MP (paso a paso)

### 3.1 Entrar al panel

1. Ir a **[Tu integración](https://www.mercadopago.com.ar/panel/app)**
2. Seleccionar la app (application_id: `4435899954444318`)

### 3.2 Configurar webhook

1. Menú izquierdo → **Webhooks** → **Configurar notificaciones**
2. En **URL de notificaciones** poner:
   ```
   https://tudominio.com/api/webhooks/mercadopago
   ```
3. En **Eventos**, seleccionar:
   - [x] **Planes y suscripciones** → `subscription_preapproval`, `subscription_authorized_payment`
   - [x] **Pagos** → `payment`
   - [ ] ~~Vinculación de aplicaciones~~ (no necesario)
   - [ ] ~~Reclamos~~ (no necesario)
   - [ ] ~~Perfil de pago~~ (no necesario)

4. Click en **Guardar**
5. MP genera un **secreto de firma** → copiarlo y guardarlo como `APP_MERCADOPAGO_WEBHOOK_SECRET`

### 3.3 Probar con simulacro

- En la misma pantalla hay un botón **"Simular notificación"**
- Esto envía un evento de prueba inmediato
- Verificar en:
  - **Panel MP** → Webhooks → Historial (debe aparecer la notificación)
  - **Logs del backend** → debe aparecer: `Webhook POST recibido de MP`

---

## 4. Checklist de producción

```
[ ] Dominio con HTTPS activo (https://tudominio.com)
[ ] URL del webhook configurada en panel de MP (producción, no test)
[ ] Secreto de firma cargado en variables de entorno (APP_MERCADOPAGO_WEBHOOK_SECRET)
[ ] APP_MERCADOPAGO_ACCESS_TOKEN de producción (no el de test)
[ ] Probar con botón "Simular" del panel MP
[ ] Verificar log del backend: "Webhook POST recibido de MP"
[ ] Verificar en panel MP → Webhooks → Historial: estado "delivered"
[ ] Crear suscripción de prueba → pagar → verificar que llega webhook
[ ] Verificar que el estado cambia PENDING → ACTIVE automáticamente
```

---

## 5. Archivos clave del código

| Archivo | Función |
|---------|---------|
| `MercadoPagoService.java` | Crear preapproval, consultar MP, validar firma HMAC |
| `SubscriptionService.java` | Procesar webhook, reconciliación, lógica de estados |
| `WebhookController.java` | Endpoint `POST /api/webhooks/mercadopago` |
| `BillingController.java` | Endpoint `POST /api/billing/subscribe`, `GET /api/billing/subscription` |
| `WebhookRegistrar.java` | `ApplicationRunner`: al arrancar registra `app.webhook-url` en MP (`POST /webhooks`). Si la URL está vacía, avisa en el log y no registra nada |
| `application.properties` | Configuración de MP y URLs |

### Endpoints

| Método | URL | Descripción |
|--------|-----|-------------|
| `POST` | `/api/billing/subscribe` | Crear suscripción |
| `GET` | `/api/billing/subscription` | Consultar estado (con reconciliación) |
| `POST` | `/api/billing/subscription/cancel` | Cancelar la suscripción (sin más cobros) |
| `POST` | `/api/billing/subscription/pause` | Pausar la suscripción (sin cobros hasta reactivar) |
| `POST` | `/api/billing/subscription/resume` | Reactivar una suscripción pausada |
| `POST` | `/api/webhooks/mercadopago` | Recibir notificaciones de MP |
| `GET` | `/api/webhooks/mercadopago` | IPN probe (MP verifica que la URL existe) |

La gestión (cancelar / pausar / reactivar) usa `PUT /preapproval/{id}` de MP con
`{"status": "cancelled" | "paused" | "authorized"}`. **`cancelled` es definitivo
en MP**: para volver a suscribirse se crea un preapproval nuevo
(`POST /api/billing/subscribe`). Cada operación manual registra un
`SubscriptionEvent` de auditoría (`manual:cancel|pause|resume`).

---

## 6. Algoritmo de validación de firma

MP envía headers `x-signature` y `x-request-id`. El manifiesto es:

```
id:{data.id};request-id:{x-request-id};ts:{ts};
```

Luego se calcula HMAC-SHA256 con el secreto y se compara con la firma.

**Importante:** Si hay secreto configurado, la firma es **obligatoria**: los webhooks sin header `x-signature` (o con firma inválida) se ignoran. Solo si NO hay secreto se aceptan sin firma (desarrollo). El flag `app.mercadopago.webhook-require-signature=false` desactiva la obligatoriedad únicamente para desarrollo. En producción **siempre** debe haber secreto.

---

## 7. Troubleshooting

| Problema | Causa | Solución |
|----------|-------|----------|
| 404 "Preapproval not found for ID" en logs | Evento `subscription_authorized_payment` o `payment`: su `data.id` es una factura/pago, NO un preapproval | Ya manejado en `SubscriptionService`: cada topic consulta su endpoint (`/authorized_payments/{id}` para facturas, `/v1/payments/{id}` para pagos) y resuelve el `preapproval_id` |
| Webhook no llega al backend | URL incorrecta o inestable | Verificar URL en panel MP; usar dominio fijo (no tunnels temporales) |
| "No Notifications Found" en panel MP | No se generaron eventos desde que se configuró | Usar botón "Simular" o crear nueva suscripción |
| Firma inválida | Secreto incorrecto o manifiesto mal armado | Verificar `APP_MERCADOPAGO_WEBHOOK_SECRET` y algoritmo HMAC |
| Estado no cambia a ACTIVE | Webhook no llega + nadie consulta el estado | La reconciliación funciona al consultar `GET /api/billing/subscription` |
| 403 Forbidden | `SecurityConfig` no permite el método | Verificar `permitAll()` para POST y GET en `/api/webhooks/**` |
| 502 Bad Gateway | Tunnel caído o backend no corre | Verificar que backend y tunnel estén activos |

---

## 8. Notas para producción

- **No usar** `*.trycloudflare.com` ni tunnels temporales para webhooks de MP
- Usar **dominio propio** con HTTPS (obligatorio para MP)
- El webhook es **instantáneo**; la reconciliación es el **fallback** cuando no hay webhook
- MP **no reenvía** notificaciones anteriores a la configuración
- Si se cambia la URL del webhook, las notificaciones pendientes se pierden
