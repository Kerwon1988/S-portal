# ADR-012 — Inventario operativo de errores

- Estado: Aceptado; ERR-001..003 y PROMPT 04 completados y validados localmente; límites operativos/retención y aprobación legal productiva siguen pendientes fuera del alcance de desarrollo
- Fecha: 2026-11-11

## Contexto
S-portal debe centralizar errores backend, frontend y base de datos. La referencia de SimbioSphere es `ErrorInventoryService`, que persiste `logs-error/error-inventory.json`; `CustomErrorController` atiende `/error` y delega, no es el registrador central (evidencia H37–H43 en [05](../plan/05-integracion-simbiosphere.md)). Los errores de SQL deben poder registrarse incluso cuando SQL Server está caído.

## Decisión recomendada
Diseñar `ErrorInventoryService` como servicio transversal en `config.errortracking`, invocable desde `shared.spi.ErrorReporter`. Persistir inventario JSON fuera del webroot y de SQL Server en volumen privado, con escritura atómica/single-writer inicialmente, deduplicación por fingerprint, límite de tamaño/caducidad, backups y estados OPEN/RESOLVED/IGNORED. Capturar ampliamente errores reportables de backend, frontend y BD; clasificarlos (incluidos errores esperados/4xx útiles para diagnóstico), y conservar stack/cadena causal y contexto técnico saneado para que una futura IA pueda diagnosticar con eficacia. `/error` solo delega desde el flujo servlet. Visor y resolución son ADMIN-only. En MVP la IA no accede al inventario ni ejecuta reparaciones.

No se copiará el `Throwable.getMessage()` crudo de la referencia: no se almacenan parámetros SQL, query string, body, cookies, headers sensibles, tokens ni valores de formulario. Frontend solo envía tras opt-in explícito: banda no modal ES/EN, denegada por defecto, aceptar/rechazar y revocación desde el pie de página; la elección versionada se guarda en localStorage, sin identidad ni evidencia backend, y cualquier error al persistir mantiene deshabilitado el envío. El copy es provisional y requiere revisión legal antes de producción. Tras consentir, el navegador envía solo evento/nombre allowlisted, marcos de assets fingerprinted, locale, correlation ID y ruta reducida; el servidor fija el origen, vuelve a validar/sanear y no persiste el payload de intake inválido como error BACKEND. Aplicar clasificación, deduplicación, límites y rate limit, sin descartar silenciosamente clases de error configuradas. Un fallo del registrador no sustituye ni oculta la excepción original.

## Alternativas
- Persistir en SQL Server: rechazado como almacén único porque no estaría disponible durante errores de conexión/esquema de BD.
- Añadir módulo de negocio `error-tracking`: rechazado; observabilidad/configuración transversal pertenece a `config`, con SPI mínimo en `shared`.
- Conservar JSON sin límite/retención ni coordinación: rechazado para producción por crecimiento, PII y escrituras concurrentes.
- Plataforma externa de logging/SIEM: complementaria; proveedor y residencia de datos quedan por decidir, no reemplaza el inventario operativo MVP.

## Consecuencias
El visor no consulta SQL como fuente del inventario y muestra datos saneados. Primera etapa presupone una instancia/single writer; varias réplicas requieren storage coordinado aprobado antes de escalar. La retención, tamaño máximo, backup y RPO deben fijarse antes de producción. Frontend intake debe aplicar CSRF/same-origin/rate limit y no aceptar un `source` confiado del cliente sin validación.

Implementación inicial: `logs-error/error-inventory.json` por defecto, configurable mediante `PORTAL_ERROR_TRACKING_PATH`; límites provisionales de 1.000 entradas, 5 MiB y 2.048 caracteres por campo. Se usa SHA-256 sobre metadatos estructurales, escritura atómica en proceso single-writer y permisos POSIX de propietario cuando el filesystem los soporta. En Windows se heredan las ACL del directorio privado. No se persisten mensajes de `Throwable`, SQL crudo, query strings ni cuerpos HTTP. Al llegar al límite solo se evictan entradas RESOLVED/IGNORED; si todas están abiertas, se informa fallo de captura sin sustituir el error de aplicación.

ERR-003 incorpora `POST /api/telemetry/errors`: el cliente solo lo usa tras consentimiento explícito; CSRF activo, `Origin` exacto same-origin, JSON no comprimido, máximo 4 KiB, allowlists de evento/nombre y rate limiting en memoria (30 de ráfaga, 30/minuto; navegador cinco/minuto). Los frames permitidos se convierten en nombres técnicos de assets fingerprinted y la ruta se vuelve a sanear en servidor. No hay acceso privilegiado de usuario ni se acepta un campo `source` del cliente. El rate limiter es por proceso y debe revisarse antes de escalar.

## Verificaciones pendientes
- ERR-001..002: implementados; 7 UT de error-tracking, 1 IT H2 y gates de capas/Modulith/DAG ejecutados y aprobados. El inventario no depende de la BD.
- ERR-003: opt-in, intake, CSRF, same-origin, límites y sanitización implementados; `mvn verify` local con H2, build frontend y Playwright 15/15 aprobados, incluidos denegación por defecto, aceptación, revocación y fallo de almacenamiento. Se verificó la instancia en `local-h2`; no se certifica SQL Server ni despliegue multi-réplica. El texto provisional necesita revisión legal antes de producción. UIK-006, ERR-001 y FND-009 están satisfechas.
- ERR-004: están presentes el visor server-side ADMIN con permiso dedicado, filtros, paginación, resolución/ignorado auditados y degradación sin detalles ante storage no disponible. Las pruebas específicas y la compilación del cambio no se ejecutaron; permanece sin verificar y abierto.
- HRD-005/007/008/010: backup, métricas/alertas, retención aprobada y storage/secretos.
- 13-preguntas-abiertas.md: cap/retención, persistencia multi-réplica, clasificación/severidad y payload frontend.
