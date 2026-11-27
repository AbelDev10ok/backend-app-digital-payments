# Deploy a producción

Guía para publicar el backend (Spring Boot) y el frontend (SPA) en producción.
Sin staging: se va directo a producción, en este orden.

**Orden obligatorio: base de datos → backend → frontend.** El backend es
fail-fast: sin `DB_URL` no arranca. El frontend necesita la URL pública del
backend para compilarse.

---

## 1. Prerrequisitos de código (hacer antes de desplegar)

| # | Cambio | Por qué |
|---|--------|---------|
| 1 | `vercel.json` con rewrite SPA en el repo del frontend | Sin esto, recargar `/dashboard/cobros` devuelve 404 |
| 2 | Permitir `/actuator/health` en `SecurityConfig` | El health check del proveedor recibe 401 y reinicia el contenedor |

El health check hoy está bloqueado: `SecurityConfig.filterChain` termina con
`anyRequest().authenticated()` y `/actuator/**` no está en la lista de
`permitAll()`. Agregar esta línea junto a las demás:

```java
.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
```

`/actuator/health` no expone datos de negocio (solo estado UP/DOWN y el chequeo
de la base). Es el endpoint estándar para health checks.

---

## 2. Base de datos

### Opción recomendada: Railway Postgres

En el mismo proyecto que el backend. Ventajas para este stack: siempre
encendido (el cron diario de la ronda y los webhooks de MP no dependen de que
la base despierte), sin pooler intermedio, misma factura y mismo dashboard.

1. Crear cuenta en [railway.app](https://railway.app).
2. `New Project` → `Provision PostgreSQL`.
3. Copiar las credenciales desde la pestaña `Variables` del servicio:
   - `DB_URL` → valor de `DATABASE_URL` (formato `postgresql://...`).
     **Ojo:** Railway entrega `postgres://`; Spring necesita el prefijo JDBC:
     `jdbc:postgresql://host:puerto/db`. Si el driver rechaza la URL, ese es el
     motivo.
   - `DB_USERNAME` → `PGUSER`
   - `DB_PASSWORD` → `PGPASSWORD`

### Alternativa: Supabase / Neon

Funciona igual, pero el plan gratuito **pausa el proyecto por inactividad** y
las conexiones directas tienen límite. Para el cron diario y los webhooks,
usar el connection pooler (puerto 6543 en Supabase) y desactivar la pausa si el
plan lo permite. Si el proyecto se pausa, la app falla al arrancar.

### Esquema

No hay migraciones: `spring.jpa.hibernate.ddl-auto=update` crea y ajusta las
tablas al arrancar, y `data.sql` siembra roles y el plan de suscripción en cada
boot (idempotente). El primer arranque del backend crea todo el esquema.

`ddl-auto=update` no borra columnas ni revierte cambios. Para el primer deploy
alcanza; para cambios de entidad posteriores es deuda técnica (ver sección 7).

---

## 3. Backend

### Imagen Docker

El `Dockerfile` del repo es multietapa (Maven + JDK 17 para compilar, JRE 17
para correr) y ya lee `PORT`:

```dockerfile
CMD ["sh", "-c", "java -Dserver.port=${PORT:-${SERVER_PORT:-8080}} -jar app.jar"]
```

Los tests no corren dentro de la imagen (corren en CI y en local).

### Variables de entorno obligatorias

Sin estas, la app no arranca con el perfil `prod`:

| Variable | Nota |
|----------|------|
| `SPRING_PROFILES_ACTIVE` | `prod` activa `application-prod.properties` |
| `DB_URL` | URL JDBC completa (`jdbc:postgresql://...`) |
| `DB_USERNAME` | |
| `DB_PASSWORD` | Validada por `StartupSecurityCheck` |
| `JWT_SECRET` | ≥ 64 caracteres (HS512). Validada por `StartupSecurityCheck` |
| `APP_CORS_ALLOWED_ORIGINS` | Dominio del SPA, sin barra final |
| `APP_FRONTEND_URL` | Dominio del SPA (links de mail y `back_url` de MP) |
| `APP_WEBHOOK_URL` | `https://<backend>/api/webhooks/mercadopago` |
| `APP_MERCADOPAGO_ACCESS_TOKEN` | Credencial de **producción** de la Cuenta Negocio |
| `APP_MERCADOPAGO_WEBHOOK_SECRET` | Secreto del webhook |

Opcionales con default: `APP_RATE_LIMIT_*`, `APP_WEBHOOK_REQUIRE_SIGNATURE`.
Lista completa en `.env.example`.

Generar el `JWT_SECRET`:

```bash
openssl rand -base64 64
```

### Health check

`GET /actuator/health` → `{"status":"UP"}` cuando la base responde. Configurar
el health check del proveedor contra esa ruta (requiere el prerrequisito 2).

### Verificación post-deploy

```bash
curl https://<backend>/actuator/health     # {"status":"UP"}
curl -i https://<backend>/auth/login        # 405/400, nunca 500
```

Swagger queda en `/swagger-ui.html`.

---

## 4. Frontend

### Variables de build

Vite hornea las variables en el bundle: cambiarlas exige un rebuild.

| Variable | Valor |
|----------|-------|
| `VITE_API_URL` | `https://<backend>` (sin barra final, sin `/api`) |

### `vercel.json`

Sin el rewrite, cualquier ruta distinta de `/` da 404 al recargar:

```json
{
  "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }]
}
```

### Pasos en Vercel

1. Importar el repo `FrontEnd-app-digital-payments`.
2. Framework preset: Vite. Build: `npm run build`. Output: `dist`.
3. Cargar `VITE_API_URL` en *Environment Variables*.
4. Deploy.

`npm run build` no typechequea: correr `npx tsc -p tsconfig.app.json` antes si
se quiere esa garantía.

---

## 5. Cierre de la configuración

1. **CORS**: `APP_CORS_ALLOWED_ORIGINS` debe incluir el dominio del SPA con
   `https://` y sin barra final. Si el navegador bloquea requests, es esta
   variable.
2. **Webhook de MP**: al arrancar, `WebhookRegistrar` registra
   `APP_WEBHOOK_URL` en MP. El log debe decir `Webhook registrado exitosamente`.
   Si dice `app.webhook-url no configurado`, falta la variable.
3. **Credenciales de MP**: confirmar que el token es de producción
   (`GET https://api.mercadopago.com/users/me`; si `tags` trae `test_user`, es
   una cuenta de prueba y los cobros reales no van a funcionar).
4. **SMTP**: cargar `EMAIL_USERNAME` / `EMAIL_PASSWORD` o los mails de
   verificación y restablecimiento no salen.

---

## 6. Smoke test

Contra producción, en este orden:

- [ ] Registro de negocio nuevo → llega el mail de verificación
- [ ] Verificar el mail → login
- [ ] Alta de producto con variante → stock inicial registrado
- [ ] Venta al contado → total correcto y stock descontado
- [ ] Venta en cuotas → cuotas en la agenda
- [ ] Cobro de una cuota → se registra el pago
- [ ] Ronda del día → paradas por cliente
- [ ] Emitir remito → número correlativo
- [ ] Suscripción → checkout de MP abre y el pago se acredita
- [ ] Recargar `/dashboard/cobros` con F5 → no da 404

---

## 7. Deuda conocida

- **Sin migraciones.** Flyway está deshabilitado y `PLAN_EJECUCION.md` menciona
  archivos `V*__*.sql` que no existen. El esquema lo maneja Hibernate. Un cambio
  de entidad en producción altera la base sin revisión ni rollback. Antes del
  segundo deploy conviene introducir Flyway o al menos un backup verificado.
- **Sin staging.** Los errores se descubren en producción. El smoke test es la
  única red.
- **CSP y HSTS.** `SecurityHeadersFilter` aplica `nosniff`, `X-Frame-Options` y
  `Referrer-Policy`, pero no Content-Security-Policy ni Strict-Transport-Security.
- **CI no despliega.** El workflow corre `./mvnw -B verify`; publicar es manual.
