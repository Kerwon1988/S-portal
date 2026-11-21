# 09 — Operación y despliegue

## Entornos y puertos
| Entorno | Configuración | Persistencia/servicios | Exposición |
|---|---|---|---|
| `dev` | Java 21, Spring Boot, Tailwind/HTMX/Alpine locales | H2 como base de pruebas; sin Docker ni SQL Server | App 8090; sin puertos locales de SQL/Compose |
| `test` | Perfil reproducible, secretos sintéticos y clock controlable | H2 en memoria; sin Testcontainers ni SQL Server | Sin servicios públicos; datos sintéticos por suite |
| `prod` | Imagen multi-stage, properties validadas, secretos externos | SQL Server 2022, volumen de ficheros, SMTP/proveedor, antivirus | HTTPS tras reverse proxy; ningún puerto DB/management en Route pública |

Puertos separados de DEMO: app local `8090`. Los puertos SQL `14330`, Mailpit `1026/8026` y ClamAV `3310` quedan descritos para despliegues Compose, pero no son prerrequisito ni gate de pruebas local.

## Docker y aplicación
- Compose definido en FND-005 describe SQL Server `mcr.microsoft.com/mssql/server:2022-latest`, Mailpit y ClamAV para despliegues Docker habilitados; no se exige arrancarlo ni probar healthchecks en desarrollo. `MSSQL_SA_PASSWORD` se inyecta por entorno/secretos protegidos y nunca se registra en Git ni en logs.
- El tag SQL Server, CU/digest y EULA quedan pendientes de la plataforma de despliegue. El prompt original menciona `mcr.microsoft.com/mssql/server:2022`, que se trata como **VERIFICAR** antes de desplegar, no como gate de desarrollo.
- Producción: Dockerfile multi-stage, runtime no-root, filesystem mínimo, assets con huella, imagen base fijada, SBOM, healthcheck, TLS terminado en proxy. Un único desplegable y una BD; no separar módulos en microservicios.
- Parámetros obligatorios validan al arranque (`portal.*`, URL pública, DB, SMTP, cifrado, endpoints DEMO); `dev/test/prod` no heredan secretos entre sí. `.env` nunca se publica.
- La falta de Docker, Mailpit, ClamAV, SQL Server, PRE o credenciales en una máquina solo pospone las pruebas que dependan de esos recursos; no deja pendiente el código, la configuración ni los tests que se puedan preparar localmente. Ejecuta las pruebas disponibles; un fallo local bloquea las tareas dependientes. Registra cada ejecución externa no realizada como `POSTPONED` (o `PREPARADO_NO_EJECUTADO` para PRE), indicando prueba, recurso/entorno, motivo, evidencia requerida e instrucciones reproducibles para otra máquina habilitada; nunca como aprobada.

## SQL Server y ficheros
- SQL Server 2022 es la única persistencia de producción; un árbol Flyway `db/migration/<module>/...`; esquema por módulo, `NVARCHAR`, UTC `datetime2(3)`, importes `DECIMAL(19,4)`, collation por confirmar. La ejecución SQL Server/Flyway no forma parte de los gates automatizados.
- H2 es el único motor de pruebas y no tiene árbol Flyway propio; su uso no certifica el dialecto de producción. No ejecutar `flyway clean` en producción; las migraciones se mantienen forward-only.
- Edición (Express/Standard/Azure SQL), límite de tamaño, TDE y precio son decisiones abiertas de ADR-006; **no se inventan cifras** ni se afirma soporte sin matriz oficial (HRD-013).
- MVP: `FileStorage` local fuera del webroot, volumen separado, nombres aleatorios, antivirus ClamAV y cuarentena. Blob/objeto es R2; descargas autorizadas con `Content-Disposition: attachment`, `nosniff` y limpieza por retención.

## Actuator, observabilidad y alertas
**Decisión de reconciliación:** puerto de management separado `8091` (supuesto ya utilizado en FND-010), accesible solo desde red/probe interna y protegido por política de servicio; no publicar Route/Ingress. App HTTP queda en `8090`. Exponer únicamente health/liveness/readiness y métricas Prometheus necesarias; no habilitar `/env`, configuración ni detalles de stack. Revisar la regla final de acceso con plataforma en FND-010/HRD-007. Las probes no deben devolver datos sensibles.

- Logs JSON en prod, `X-Correlation-Id` validado, masking de email/token/password, sin body ni PII; retención propuesta 90 días a validar.
- `ErrorInventoryService` persiste `logs-error/error-inventory.json` en volumen privado y separado de SQL Server para que errores de conexión/BD sigan registrándose. Aplicar ACL de proceso, fuera del webroot, lock/reemplazo atómico, cap de tamaño/entradas, deduplicación, backup y purga según ADR-012; el fallo del writer no oculta el error original.
- MVP de despliegue presupone una instancia/single writer para el JSON. No escalar a varias réplicas hasta acordar storage compartido/coordinación y prueba de corrupción/lock; no usar NFS no validado como solución implícita.
- Métricas agregadas: estado/edad de provisioning, backlog/errores de outbox, rate limit, EPR fallido, ClamAV, uptime/latencia/DB pool. El worker `portal.simbiosphere.worker` permanece deshabilitado por defecto; para habilitarlo se requieren URL/HMAC mediante secreto, DEMO local/entorno autorizado y observación de estados/reintentos. No habilitar liveness dependiente de DEMO. Etiquetas sin email, tenant slug ni public ID.
- Métricas del inventario: entradas por source/severity/status, errores de escritura, edad del último save y tamaño/ocupación; sin mensajes, stack, ruta con query, IP, email ni fingerprint como label.
- Alertas a ADMIN/operación: `FAILED`/`ACTION_REQUIRED` antiguos, cola email sobre umbral, ClamAV caído, EPR pendiente, saturación DB y fallos de backup. Umbrales definitivos requieren baseline HRD-003.
- Alertar al operador si el inventario no puede persistir o supera cap; mantener fallback de log estructurado saneado y no reintentar recursivamente.
- Health readiness comprueba DB y dependencias locales indispensables; liveness no depende de DEMO/SMTP para evitar reinicios en cascada. Circuit breaker gestiona dependencia externa.

## Email, secretos y seguridad operativa
- Email por puerto `EmailSender`; Mailpit es el destino de integración local cuando el servicio esté disponible. Si no está disponible, pospón la prueba de entrega y ejecútala en un entorno habilitado; esto no bloquea implementar el adapter, la configuración tipada ni sus pruebas locales. Proveedor productivo y región quedan abiertos (ADR-007). DNS SPF/DKIM/DMARC y prueba de entrega en HRD-009.
- Secret manager o mecanismo de plataforma por decidir; ningún secreto en YAML, Compose, imagen, artefactos, logs o documentación. Rotar keys DB/SMTP/HMAC/cifrado; transición con dos claves activas para HMAC.
- Egress mínimo a SQL, ClamAV, SMTP y host DEMO configurado; política de red bloquea destinos arbitrarios. HSTS solo bajo HTTPS; TLS mínimo y certificados gestionados por plataforma.
- Si se despliegan varias réplicas, worker reclama outbox/provisioning con lease y locking DB; decidir estrategia de sesiones compartidas antes de escalar (SUPUESTO: despliegue inicial puede operar una réplica).

## Backups, recuperación y releases
- Documentar backups cifrados fuera del host y el procedimiento de restauración (HRD-005); no se exige ejecutar una restauración SQL Server. Frecuencia, retención, RPO/RTO y ventanas quedan pendientes de decisión operativa en 13.
- Incluir el volumen del inventario JSON en el plan de backup/restore con retención aprobada; si el archivo falta o está corrupto, arranque recupera en modo degradado y alerta sin bloquear el servicio principal.
- Release: build reproducible y SBOM; migración compatible; smoke HTTP/health; verificación de correo/ficheros; activación de feature flags; monitorización reforzada; rollback según runbook HRD-011.
- Revertir aplicación no implica revertir migraciones destructivas. Usar migración de corrección/forward fix; restauración DB solo bajo procedimiento y decisión de incidente.
- UAT/Go-No-Go requiere ASVS/ZAP, CSP enforce, rendimiento, WCAG, estrategia de backup documentada, secretos/DNS, RAT y dependencias DEMO listas. No exige una prueba de restauración SQL Server. Si DEMO no está lista, alta conjunta no se declara completa; alternativa externa está en [05](05-integracion-simbiosphere.md).

## Decisiones relacionadas
- SQL/IDs/licencia: [ADR-004](../adr/ADR-004-estrategia-de-ids-sql-server.md), [ADR-006](../adr/ADR-006-despliegue-y-licencia-sql-server.md).
- Email: [ADR-007](../adr/ADR-007-email-transaccional.md). Cifrado: [ADR-009](../adr/ADR-009-cifrado-de-pii.md).
- Verificación de dependencias Boot 4/Flyway/Actuator y modo de gestión: FND-001/FND-010. SQL Server no se valida con los gates de desarrollo y no se afirma compatibilidad certificada.
- Persistencia, multi-réplica, cap, retención y recuperación del inventario: [ADR-012](../adr/ADR-012-inventario-operativo-de-errores.md), ERR-001/004 y pregunta abierta en 13.
