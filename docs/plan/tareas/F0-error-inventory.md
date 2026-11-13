# F0 — Inventario de errores (núcleo y captura backend/BD/frontend)

- Objetivo: registrar errores desde el inicio del desarrollo en un inventario central consultable y saneado.
- Módulo(s) owner: `config.errortracking`; publica el puerto mínimo `shared.spi.ErrorReporter`.

### ERR-001 — Inventario central de errores y persistencia JSON fuera de la BD
- Fase / épica / módulo owner: F0 / ERR / config
- Tipo: feat
- Depende de: FND-002, FND-003, FND-007
- Descripción: Cubre: RF-ERR-01; crea `ErrorInventoryService` central para fuentes BACKEND/FRONTEND/DATABASE, huella estable, severidad, estado, marcas de tiempo, ocurrencias y contexto de diagnóstico asistido por IA.
- Ficheros/paquetes a crear o modificar (aproximado): `shared.spi.ErrorReporter`, `config.errortracking.ErrorInventoryService`, `ErrorRecord`, `ErrorInventory`, DTOs y configuración `portal.error-tracking.*`.
- Impacto modular: `config` implementa SPI de `shared`; JSON `logs-error/error-inventory.json` fuera del webroot, sin tablas ni dependencia DB. Ciclo: ninguno.
- UI: n/a (consulta ADMIN en ERR-004).
- Seguridad: scrub/allowlist, límites de tamaño/entradas, permisos de archivo; nunca PII/secretos en bruto, tokens, body ni literales/valores SQL.
- Criterios de aceptación (Given/When/Then):
  - Given errores con misma huella When se registran Then incrementa ocurrencias sin duplicar entrada y conserva primer/último momento.
  - Given SQL Server caído When un error se registra Then persiste en JSON independiente; fallo del tracker no reemplaza la excepción original.
- Tests: TC-ERR-001: UT (fingerprint/redacción), IT (carga/reinicio/escritura), SEC (fixtures PII/secretos).
- Documentación a actualizar: DIARIO + INVENTARIO; ADR-012; README operación
- Estimación: M (≤ 1 d)
- Riesgos: JSON local no coordina varias réplicas; montar volumen persistente y confirmar single-writer antes de escalar.

### ERR-002 — Captura backend y fallos SQL con metadatos saneados
- Fase / épica / módulo owner: F0 / ERR / config
- Tipo: feat
- Depende de: ERR-001, FND-006, FND-010
- Descripción: Cubre: RF-ERR-01; conecta handlers MVC/API, filtros y aspecto de persistencia con `ErrorInventoryService`; clasifica errores observables sin descartar clases configuradas; `/error` solo delega el caso servlet.
- Ficheros/paquetes a crear o modificar (aproximado): `config.errortracking.DatabaseErrorTrackingAspect`, `ErrorTrackingRequest`, adaptadores MVC/API y filtro HTTP.
- Impacto modular: captura transversal vía `shared.spi.ErrorReporter`; `config` no importa módulos de negocio; no añade tablas/eventos. Ciclo: ninguno.
- UI: n/a; respuestas de error mantienen páginas 403/404/500 y payload API existentes.
- Seguridad: clasificar 401/403/validación esperada y aplicar deduplicación/rate limit; quitar query, body, credenciales y PII; SQL solo SQLState/código/tabla/constraint/operación y texto parametrizado.
- Criterios de aceptación (Given/When/Then):
  - Given una SQLException atraviesa aspecto y handler global When se propaga Then se contabiliza una sola vez y se conserva la excepción original.
  - Given mensaje SQL contiene valores de fila When se crea registro Then inventario guarda metadatos saneados y no incluye mensaje raw.
- Tests: TC-ERR-002: UT (clasificación/redacción), IT JPA con H2 para persistencia/deduplicación, WT/MT (handler y `/error`), SEC. No se requieren pruebas SQL Server/Testcontainers.
- Documentación a actualizar: DIARIO + INVENTARIO; evidencia de captura en 05
- Estimación: M (≤ 1 d)
- Riesgos: error tracker no debe ocultar fallo original; probar también indisponibilidad de almacenamiento JSON.

### ERR-003 — Captura frontend de errores JavaScript por endpoint same-origin
- Fase / épica / módulo owner: F0 / ERR / config
- Tipo: feat
- Depende de: ERR-001, FND-009, UIK-006
- Descripción: Cubre: RF-ERR-01; captura errores JS no controlados y fallos frontend observables de red/HTTP/HTMX, los clasifica y envía DTO acotado al endpoint `/api/telemetry/errors`.
- Ficheros/paquetes a crear o modificar (aproximado): `static/js/error-reporter.js`, controller DTO de intake, fragmento base y claves i18n de consentimiento/estado si aplica.
- Impacto modular: endpoint y adaptación en `config`; el DTO llega al SPI `shared.spi.ErrorReporter`; sin tabla ni API de negocio. Ciclo: ninguno.
- UI: script local en layouts; ningún toast intrusivo; opt-in explícito mediante banda no modal ES/EN, denegada por defecto, con aceptar/rechazar y opción visible de cambiar/retirar la decisión. Persistencia versionada solo en el navegador; sin identidad ni evidencia en backend. Correlación opcional con `correlationId`.
- Seguridad: no enviar mientras no exista consentimiento explícito; respetar rechazo y revocación en cliente; si no se puede persistir la decisión, denegar captura. Además CSRF, same-origin, rate limit, payload ≤4 KB; path sin query, message/stack scrubbed/truncados; nunca DOM, form values, cookies o headers.
- Criterios de aceptación (Given/When/Then):
  - Given error JS o fallo frontend de red/HTTP/HTMX When emite POST same-origin válido Then inventario crea entrada FRONTEND clasificada con release/locale/correlation ID y ruta segura.
  - Given CSRF ausente, origen externo o payload con PII/oversize When ingresa Then rechaza/limita y no persiste el dato sensible.
  - Given no hay opt-in, se rechazó o se retiró When ocurre un error frontend Then no se emite POST; si el navegador no puede guardar el opt-in, tampoco se habilita el envío.
- Tests: TC-ERR-003: UT (sanitización/size), WT (CSRF/origin/rate), E2E (denegación por defecto, aceptación, rechazo/revocación y fallo de persistencia), SEC.
- Documentación a actualizar: DIARIO + INVENTARIO; política de payload en 07
- Estimación: M (≤ 1 d)
- Riesgos: no se halló hook frontend en recursos DEMO inspeccionados; confirmar su cobertura antes de adoptar eventos existentes.
