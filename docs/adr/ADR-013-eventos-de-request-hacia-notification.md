# ADR-013 — Traducción owner de eventos request hacia notification

- Estado: Accepted
- Fecha: 2026-11-20

## Contexto

`notification` había empezado a consultar `request.api` para resolver destinatario y ruta. Junto con las dependencias históricas `customer → identity → notification` y `request → customer`, esa consulta formaba un ciclo Spring Modulith `customer → identity → notification → request → customer`.

`request` posee el owner, la referencia pública y el contexto necesarios para preparar una notificación. `notification` posee el outbox, plantillas, locale efectivo, preferencias y entrega.

## Decisión

- Los eventos de dominio `RequestCreated`, `RequestStatusChanged` y `QuoteSent` permanecen en `request.api` y se publican después del commit con datos técnicos mínimos.
- `request` consume esos eventos en su propio módulo, consulta el owner/ruta y publica `notification.api.RequestNotificationRequested`.
- `RequestNotificationRequested` contiene únicamente UUID de destinatario, ruta interna sin token/query/fragment, plantilla técnica, locale de fallback y clave idempotente.
- `notification` no importa tipos, entidades, repositorios ni contratos de `request`.
- `notification` conserva locale, preferencias, cifrado, outbox, reintentos y entrega.

## Consecuencias

- El DAG conserva la dirección `request → notification` y elimina `notification → request`.
- Las claves deterministas preservan la idempotencia de replay.
- La resolución de owner/ruta permanece en el owner de datos.
- Eventos y logs no transportan PII, tokens ni bodies.

## Evidencia

- `ModularityTests` verifica que `ApplicationModules.verify()` no detecta el ciclo.
- `ModuleDagTest` conserva `notification` sin dependencia de `request`.
- Tests focales cubren traducción en `request`, consumo en `notification`, ausencia de destinatario inventado, locale y clave idempotente.
