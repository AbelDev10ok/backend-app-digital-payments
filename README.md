# Backend — App-cobros (digital-pyments)

API REST del producto Cobros&Ventas: ventas, préstamos, cuotas, clientes,
catálogo con variantes, inventario y suscripciones. Spring Boot 3.5, Java 17,
PostgreSQL, JWT. Multi-tenant por `Business`.

## Arranque

Necesita PostgreSQL local con la base `digital_payments` (puerto 5432). El
esquema se crea solo: `ddl-auto=update` + `data.sql` idempotente.

```powershell
$env:DB_PASSWORD = "tu-password-de-postgres"   # obligatorio: no hay default en el repo
.\mvnw.cmd spring-boot:run
```

`http://localhost:8080` · Swagger en `/swagger-ui.html`.

Todas las variables de entorno están documentadas en `.env.example`.

## Tests

```powershell
.\mvnw.cmd test
```

Un solo test de integración usa Testcontainers (Postgres 16) y se **salta en
silencio** si no hay Docker. Un build verde no prueba que corrió.

## Documentación

| Documento | Contenido |
|-----------|-----------|
| `docs/DEPLOY.md` | Deploy a producción: base de datos, backend, variables, smoke test |
| `docs/PROMESAS_Y_RONDA.md` | Promesas de pago y el job diario de la ronda |
| `AGENTS.md` | Reglas del backend: multi-tenant, catálogo, inventario, remitos, vendedores |
| `docs/MERCADO_PAGO_SETUP.md` | Suscripciones con Mercado Pago |

## Convenciones que no se negocian

- **Multi-tenant**: todo dato de negocio lleva `business`. Resolver el tenant con
  `TenantContext.requireBusinessId()` y validar propiedad con `TenantGuard`.
- **Sin migraciones**: Flyway está deshabilitado. El esquema lo cambia Hibernate.
- **Secretos solo por entorno**: nada de credenciales en `application.properties`.
- **Español** en mensajes de error, comentarios y commits.
