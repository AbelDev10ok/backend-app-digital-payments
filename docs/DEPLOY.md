# Deploy a produccion

Guia para publicar el backend (Spring Boot) y el frontend (SPA) en produccion.
Sin staging: se va directo a produccion, en este orden.

**Orden obligatorio: base de datos -> backend -> frontend.** El backend es
fail-fast: sin `DB_URL` no arranca. El frontend necesita la URL publica del
backend para compilarse.

---

## 1. Prerrequisitos de codigo (hacer antes de desplegar)

| # | Cambio | Por que |
|---|--------|---------|
| 1 | `vercel.json` con rewrite SPA en el repo del frontend | Sin esto, recargar `/dashboard/cobros` devuelve 404 |
| 2 | Permitir `/actuator/health` en `SecurityConfig` | El health check del proveedor recibe 401 y reinicia el contenedor |

El health check hoy esta bloqueado: `SecurityConfig.filterChain` termina con
`anyRequest().authenticated()` y `/actuator/**` no esta en la lista de
`permitAll()`. Agregar esta linea junto a las demas:

```java
.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
```

`/actuator/health` no expone datos de negocio (solo estado UP/DOWN y el chequeo
de la base). Es el endpoint estandar para health checks.

---

## 2. Base de datos

### Opcion recomendada: Railway Postgres

En el mismo proyecto que el backend. Ventajas para este stack: siempre
encendido (el cron diario de la ronda y los webhooks de MP no dependen de que
la base despierte), sin pooler intermedio, misma factura y mismo dashboard.

1. Crear cuenta en [railway.app](https://railway.app).
2. `New Project` -> `Provision PostgreSQL`.
3. Copiar las credenciales desde la pestana `Variables` del servicio:
   - `DB_URL` -> valor de `DATABASE_URL` (formato `postgresql://...`).
     **Ojo:** Railway entrega `postgres://`; Spring necesita el prefijo JDBC:
     `jdbc:postgresql://host:puerto/db`. Si el driver rechaza la URL, ese es el
     motivo.
   - `DB_USERNAME` -> `PGUSER`
   - `DB_PASSWORD` -> `PGPASSWORD`

### Alternativa: Supabase / Neon

Funciona igual, pero el plan gratuito **pausa el proyecto por inactividad** y
las conexiones directas tienen limite. Para el cron diario y los webhooks,
usar el connection pooler (puerto 6543 en Supabase) y desactivar la pausa si el
plan lo permite. Si el proyecto se pausa, la app falla al arrancar.

### Esquema

No hay migraciones: `spring.jpa.hibernate.ddl-auto=update` crea y ajusta las
tablas al arrancar, y `data.sql` siembra roles y el plan de suscripcion en cada
boot (idempotente). El primer arranque del backend crea todo el esquema.

`ddl-auto=update` no borra columnas ni revierte cambios. Para el primer deploy
alcanza; para cambios de entidad posteriores es deuda tecnica (ver seccion 7).

---

## 3. Backend

### Imagen Docker

El `Dockerfile` del repo es multietapa (Maven + JDK 17 para compilar, JRE 17
para correr) y ya lee `PORT`:

```dockerfile
CMD ["sh", "-c", "java -Dserver.port=${PORT:-${SERVER_PORT:-8080}} -jar app.jar"]
```

Los tests no corren dentro de la imagen (se corren en CI y en local).

### Variables de entorno obligatorias

Sin estas, la app no arranca con el perfil `prod`:

| Variable | Nota |
|----------|------|
| `SPRING_PROFILES_ACTIVE` | `prod` activa `application-prod.properties` |
| `DB_URL` | URL JDBC completa (`jdbc:postgresql://...`) |
| `DB_USERNAME` | |
| `DB_PASSWORD` | Validada por `StartupSecurityCheck` |
| `JWT_SECRET` | >= 64 caracteres (HS512). Validada por `StartupSecurityCheck` |
| `APP_CORS_ALLOWED_ORIGINS` | Dominio real del SPA, sin barra final |
| `APP_FRONTEND_URL` | Dominio real del SPA (links de mail y `back_url` de MP) |
| `APP_WEBHOOK_URL` | `https://<backend>/api/webhooks/mercadopago` |
| `APP_MERCADOPAGO_ACCESS_TOKEN` | Credencial de **produccion** de la Cuenta Negocio |
| `APP_MERCADOPAGO_WEBHOOK_SECRET` | Secreto de firma del webhook |

Opcionales con default: `APP_RATE_LIMIT_PER_MINUTE`,
`APP_RATE_LIMIT_SENSITIVE_PER_MINUTE`, `APP_RATE_LIMIT_TRUST_PROXY`,
`APP_WEBHOOK_REQUIRE_SIGNATURE`. Lista completa en `.env.example`.

Generar el `JWT_SECRET`:

```bash
openssl rand -base64 64
```

### Health check

`GET /actuator/health` -> `{"status":"UP"}` cuando la base responde. Configurar
el health check del proveedor contra esa ruta (requiere el prerrequisito 2).

### Verificacion post-deploy

```bash
curl https://<backend>/actuator/health          # {"status":"UP"}
curl -i https://<backend>/auth/login             # 405/400, nunca 500
```

Swagger UI queda en `https://<backend>/swagger-ui.html`.

---

## 4. Frontend

### Variables de build

Vite hornea las variables en el bundle: se define **antes** de compilar y
cambiar el valor exige un rebuild.

| Variable | Valor |
|----------|-------|
| `VITE_API_URL` | `https://<backend>` (sin barra final, sin `/api`) |

### `vercel.json` (rewrite SPA)

Sin esto, cualquier ruta que no sea `/` devuelve 404 al recargar:

```json
{
  "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }]
}
```

### Pasos en Vercel

1. Importar el repo `FrontEnd-app-digital-payments`.
2. Framework preset: Vite. Build: `npm run build`. Output: `dist`.
3. Cargar `VITE_API_URL` en `Settings -> Environment Variables`.
4. Deploy.

`npm run build` no typechequea: correr `npx tsc -p tsconfig.app.json` antes de
publicar si se quiere esa garantia.

---

## 5. Cierre de la configuracion

1. **CORS**: `APP_CORS_ALLOWED_ORIGINS` debe incluir el dominio del SPA con
   `https://` y sin barra final. Si el navegador da CORS, es esta variable.
2. **Webhook de MP**: al arrancar, `WebhookRegistrar` registra la URL de
   `APP_WEBHOOK_URL` en MP. El log debe decir
   `Webhook registrado exitosamente`. Si dice `app.webhook-url no configurado`,
   falta la variable.
3. **Credenciales de MP**: confirmar que el token es de produccion
   (`GET https://api.mercadopago.com/users/me`; si `tags` trae `test_user`, es
   una cuenta de prueba y los cobros reales no van a funcionar).
4. **SMTP**: si el mail de verificacion no sale, revisar `EMAIL_USERNAME` /
   `EMAIL_PASSWORD` en el entorno.

---

## 6. Smoke test

Ejecutar en orden, contra produccion:

- [ ] Registro de negocio nuevo -> llega el mail de verificacion
- [ ] Verificar el mail -> login
- [ ] Alta de producto con variante
- [ ] Venta al contado -> se descuenta el stock
- [ ] Venta en cuotas -> aparecen las cuotas en la agenda
- [ ] Cobro de una cuota -> se registra el pago
- [ ] Anular una venta -> vuelve el stock
- [ ] Emitir un remito -> numero correlativo visible
- [ ] Recargar `/dashboard/cobros` (F5) -> no da 404
- [ ] Suscripcion: checkout de MP abre y el pago se acredita

---

## 7. Deuda conocida

- **Sin migraciones.** Flyway esta deshabilitado y `PLAN_EJECUCION.md` menciona
  archivos `V3__*.sql` que no existen. El esquema lo maneja Hibernate. Un
  cambio de entidad en produccion altera la base sin revision ni rollback.
  Antes del segundo deploy conviene introducir Flyway o al menos un backup
  verificado.
- **Sin staging.** Los errores se descubren en produccion. Mantener el smoke
  test como red de seguridad.
- **CSP y HSTS.** `SecurityHeadersFilter` aplica `nosniff`, `X-Frame-Options`,
  `Referrer-Policy` y `Permissions-Policy`, pero no Content-Security-Policy ni
  Strict-Transport-Security. Pendiente para endurecer.
- **CI no despliega.** El workflow corre `./mvnw -B verify`; publicar es manual.
