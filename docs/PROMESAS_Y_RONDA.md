# Promesas de pago y ronda diaria

Cómo el sistema distingue una deuda reclamada de una deuda con fecha prometida, y cómo se arma el plan de cobro del día.

Este documento cubre dos piezas que trabajan juntas:

- **Promesa de pago** — el cliente se compromete a pagar un monto en una fecha. Es una gestión comercial, no un pago.
- **Ronda diaria** — el plan de cobro del día: una lista ordenada por riesgo con las paradas que el cobrador tiene que visitar.

---

## 1. Promesa de pago

### Qué es y qué no es

Una promesa de pago registra un compromiso: "te pago el viernes". **No mueve plata.** No toca saldos, no toca cuotas, no emite comprobantes. Es información para que la agenda sepa que esa deuda ya fue reclamada y tiene una fecha acordada.

Esto es deliberado. Si la promesa registrara un pago, el saldo del cliente mentiría: el cobrador vería la deuda saldada y dejaría de reclamarla, cuando en realidad el cliente sólo dijo que iba a pagar.

### Alcance: cliente + moneda

Una promesa se registra **por cliente y por moneda**. Nunca mezcla monedas, porque las monedas no se suman entre sí. Un cliente con deuda en ARS y en USD puede tener una promesa en cada una, y son dos compromisos independientes.

### Modelo de datos

Tabla `payment_promise`:

| Campo | Tipo | Notas |
|---|---|---|
| `id` | bigint | PK |
| `business_id` | bigint | Tenant. Filtrado por `tenantFilter` |
| `client_id` | bigint | Cliente que promete |
| `currency` | varchar(3) | ISO 4217. Obligatoria |
| `amount` | numeric(12,2) | Monto prometido. Puede ser parcial sobre la deuda |
| `promised_date` | date | Fecha prometida. Obligatoria |
| `status` | varchar(20) | `PENDING` \| `FULFILLED` \| `CANCELLED` |
| `note` | varchar(500) | Comentario del cobrador. Opcional |
| `created_at` | timestamp | Alta |
| `resolved_at` | timestamp | Cuándo se cerró. `null` mientras está pendiente |

### Estados

```
                    ┌──────────────┐
                    │   PENDING    │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
       (cobro cubre)  (a mano)    (fecha pasó)
              │            │            │
              ▼            ▼            ▼
        FULFILLED    CANCELLED    sigue PENDING
                                   pero "vencida"
```

Dos detalles que importan:

- **`vencida` no se guarda.** Se deriva al leer: `status == PENDING && promisedDate < hoy`. Si se guardara, haría falta un proceso que marque promesas vencidas cada noche — un cron más para mantener un dato que se puede calcular en una comparación de fechas. La entidad expone `estaVencida(hoy)` para eso.
- **`CANCELLED` y `FULFILLED` son terminales.** Una promesa cerrada no se reabre. Si el cliente promete de nuevo, se registra una promesa nueva. Así queda el historial de compromisos, que es lo que permite medir si el cliente cumple.

### Reglas de validación

En `PaymentPromiseServices.crear`:

1. La moneda es obligatoria y debe estar soportada (`CurrencyUtils.esSoportada`).
2. El monto debe ser **mayor a cero**.
3. La fecha prometida es obligatoria y **no puede ser anterior a hoy**. Una promesa con fecha pasada no es una promesa: es un registro histórico falso.
4. El cliente debe existir y pertenecer al negocio del token (`TenantGuard.verificar`).
5. El cliente debe tener **deuda pendiente en esa moneda**. Sin deuda no hay nada que prometer.
6. El monto prometido **no puede superar la deuda** del cliente en esa moneda. Se recalcula el saldo de cada operación no anulada antes de sumar, para no depender de un caché que pueda estar viejo.

### Endpoints

Base: `/api/loans`. Todos resuelven el tenant desde el token.

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/clients/{clientId}/promises` | Registra una promesa |
| `GET` | `/clients/{clientId}/promises?currency=` | Lista promesas del cliente. `currency` es opcional |
| `POST` | `/promises/{promiseId}/cancel` | Cancela una promesa pendiente |
| `POST` | `/promises/{promiseId}/fulfill` | Marca una promesa pendiente como cumplida |

Cancelar y cumplir sobre una promesa ya cerrada devuelve error: sólo se opera sobre `PENDING`.

### Cómo se ve en la agenda

`GET /api/loans/collections/today` devuelve el bloque `promesas`: las promesas pendientes de los clientes que aparecen en la agenda, en una sola consulta (sin N+1). El frontend agrupa por cliente + moneda y muestra un badge en la cabecera del grupo:

- **Ámbar** — promesa vigente (la fecha todavía no llegó).
- **Rojo** — promesa vencida (la fecha pasó y no se cobró).

Click en el badge abre el modal de gestión: cumplir o cancelar. Cuando no hay promesa, el botón "Promesa" abre el alta.

El modal precarga **la deuda del grupo** como monto sugerido, no lo cobrable hoy. La distinción es real: un cobro no puede superar lo que vence hoy, pero una promesa es un compromiso sobre el total de la deuda.

---

## 2. El job diario de la ronda

### Qué hace

`RoundScheduler` arma la ronda de cobro de cada negocio una vez por día.

```java
@Scheduled(cron = "0 0 6 * * *", zone = "America/Argentina/Buenos_Aires")
```

Corre a las **6:00 hora argentina**. Requiere `@EnableScheduling` en la clase principal; sin esa anotación el método existe pero nunca se ejecuta.

### La ronda es un plan guardado, no un cálculo al vuelo

La ronda se **persiste** en `collection_round` (una por negocio y día) con sus paradas en `collection_round_item`. Esto no es un detalle de implementación: es la diferencia entre un plan y una lista.

- El plan es **estable**: el cobrador arranca con una hoja de ruta y no le cambia bajo los pies mientras trabaja.
- El plan es **medible**: al cierre se puede comparar lo planificado contra lo cobrado.
- El plan **sobrevive al reinicio**: si el servidor se cae y vuelve, la ronda del día sigue siendo la misma.

Si se calculara en cada consulta, el orden cambiaría con cada cobro y no habría contra qué comparar.

### Idempotencia y concurrencia

`construirRonda` busca la ronda del día; si existe la devuelve, si no la crea. Dos procesos pueden intentar crearla al mismo tiempo (el job de las 6 y un cobrador que abre la app a las 6:01). Para que no se duplique:

- Se bloquea la fila del negocio (`findByIdForUpdate`, lock pesimista).
- El segundo proceso espera el lock y encuentra la ronda que ya guardó el primero.
- Hay un unique `(business_id, round_date)` como red de seguridad.

Un reinicio del servidor no reconstruye la ronda: si ya existe, se respeta. Eso significa que un reinicio **no pisa** el plan que el cobrador ya empezó a trabajar.

### Aislamiento de fallos

El job itera los negocios con `try/catch` por negocio. Si uno falla (base caída, dato corrupto), se loguea un warning y el resto sigue. Un negocio roto no puede dejar sin ronda a todos los demás.

### Qué entra en la ronda

Las cuotas pendientes del negocio con vencimiento hasta hoy: `cuotasPendientesDelNegocio(businessId, dia)`.

### Agrupación: cliente + moneda

Las cuotas se agrupan por **cliente + moneda**. Nunca se mezclan monedas en una misma parada. Un cliente con deuda en ARS y USD genera dos paradas, y se cobran por separado.

El monto de la parada es el **cobrable**, con el mismo tope que la agenda: por operación, nunca se promete más que la deuda de la venta. Si una venta tiene varias cuotas vencidas, se suman con ese tope.

### Orden por riesgo

El orden no es por monto: las monedas no se suman entre sí. Es:

1. **Promesa vencida primero.** El cliente que prometió y no cumplió es la prioridad comercial: ya hubo contacto y falló.
2. **Mayor atraso.** A igual condición, primero quien debe desde hace más tiempo.
3. **Nombre.** A igual atraso, orden alfabético. Es estable y explicable: el cobrador puede entender por qué el cliente está donde está.

Un orden que el cobrador no puede explicar es un orden que no va a respetar.

### Estados de una parada

| Estado | Cuándo | Se guarda |
|---|---|---|
| `PENDIENTE` | Sin cobro y sin marca de visita | No (derivado) |
| `COBRADO` | La deuda de la parada ya no existe | No (derivado) |
| `NO_ESTABA` | El cobrador marcó que no encontró al cliente | Sí (`no_estaba = true`) |

Sólo `NO_ESTABA` se persiste. `COBRADO` y `PENDIENTE` se derivan del estado de las cuotas, que es la fuente de verdad. Guardar el estado de cobro en la parada sería duplicar información que ya vive en las cuotas, y dos fuentes de verdad se desincronizan.

### Endpoints

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/loans/round/today?date=` | Devuelve la ronda del día. Si no existe, la crea |
| `PUT` | `/api/loans/round/items/{itemId}` | Marca una visita: `{ noEstaba, note }` |

El `PUT` devuelve la ronda completa ya actualizada. El frontend aplica esa respuesta directamente, sin volver a pedirla: un `GET` extra sería un viaje de red para traer exactamente lo que el servidor ya mandó.

### Frontend

Ruta `/dashboard/ronda`, en el sidebar como "Ronda del día". Muestra las paradas con su badge de estado, permite marcar "No estaba" (y deshacer), ocultar las cobradas y abrir el cobro por cliente desde cada parada.

---

## 3. Cómo se conectan

```
                    ┌──────────────────────┐
                    │  Cliente promete     │
                    │  (POST /promises)    │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │  payment_promise     │
                    │  PENDING             │
                    └──────────┬───────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
   (6:00 job)            (cobro)                (fecha pasó)
        │                      │                      │
        ▼                      ▼                      ▼
  ┌───────────┐        ┌──────────────┐      ┌──────────────┐
  │  Ronda    │        │ FULFILLED    │      │ Vencida      │
  │  ordena   │        │ (derivado)   │      │ (derivado)   │
  │  por      │        └──────────────┘      └──────┬───────┘
  │  riesgo   │                                     │
  └─────┬─────┘                                     │
        │                                           │
        └───────────────────┬───────────────────────┘
                            ▼
                  ┌──────────────────┐
                  │ Primera parada   │
                  │ de la ronda      │
                  └──────────────────┘
```

La promesa alimenta el orden de la ronda. El job no lee promesas para decidir *qué* cobrar (eso lo deciden las cuotas), las lee para decidir *en qué orden* cobrar. Es la única señal de comportamiento que el sistema tiene hoy: si el cliente cumplió o no.

---

## 4. Decisiones y por qué

**La promesa no registra pago.** Si lo hiciera, el saldo mentiría y el cobrador dejaría de reclamar una deuda que sigue viva.

**`vencida` se deriva, no se guarda.** Evita un cron nocturno para mantener un dato que es una comparación de fechas.

**La ronda se guarda.** Un plan que cambia con cada cobro no es un plan. Guardarlo permite medir planificado vs. cobrado y sobrevivir reinicios.

**Sólo `NO_ESTABA` se persiste.** Las cuotas son la fuente de verdad del cobro. Duplicar el estado en la parada crea dos verdades que se desincronizan.

**El orden prioriza promesas vencidas.** Es la señal más barata y más explicable que existe: ya hubo contacto y el cliente falló. Un modelo predictivo sin historial de cumplimiento sería humo; con promesas cumplidas y vencidas, en unos meses, hay señal real.

**El job aísla fallos por negocio.** Un negocio con datos rotos no puede dejar a todos los demás sin ronda.

---

## 5. Limitaciones conocidas

- **El envío de recordatorios es manual.** El backend no manda mensajes: el cobrador envía por `wa.me` desde la agenda. La promesa registra la gestión, no dispara la comunicación.
- **No hay notificación de promesa vencida.** Se ve en la agenda y en la ronda, pero no hay alerta push ni mail. Es el siguiente paso natural: un resumen diario con las promesas vencidas.
- **El job reintenta con backoff lineal.** Cada negocio se intenta hasta 3 veces (2 s, 4 s) antes de darse por vencido; un fallo transitorio se resuelve sin intervención. Si el proceso se cae a mitad de la ronda, el reintento no aplica: la consulta `GET /round/today` arma la ronda faltante cuando el cobrador la abre. Configurable con `app.round.retry.attempts` y `app.round.retry.backoff-ms`.
- **Una sola ronda por negocio y día.** No hay rondas por vendedor. Si el negocio tiene varios vendedores, todos ven la misma ronda y el filtro por cartera se aplica en la agenda, no en la ronda.
