# Monitorización y alertas

El puerto 8091 de Actuator es interno al runtime; `health`, `info` y `prometheus` están expuestos allí, nunca en 8090 público. `env` no se expone. Alertas y dashboards deben consultar métricas agregadas, sin email, `publicId`, tenant slug, token o payload.

| Señal | Regla inicial | Runbook |
|---|---|---|
| Readiness | DOWN durante 2 min | verificar dependencias, despliegue y rollback |
| Cola email | backlog > 100 durante 15 min o retry/failed subiendo | revisar SMTP/outbox, no consultar cuerpos |
| Provisioning FAILED | valor > 0 durante 30 min | revisar correlation IDs y estado técnico, escalar owner |
| Anti-bot/capacidad | outcome=capacity durante 5 min | revisar capacidad/rate y abuso agregado |
| JVM/errors 5xx | saturation/5xx > umbral de presupuesto | validar recursos, logs redactados, rollback si regresión |

Las reglas exactas requieren baseline y umbrales aprobados. Scrape, dashboard y disparo de alertas: **PREPARADO_NO_EJECUTADO**.
