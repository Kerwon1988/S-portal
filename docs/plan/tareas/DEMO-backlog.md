# Backlog DEMO — API M2M de aprovisionamiento (pista externa, tareas documentales)

- Objetivo: especificar cambios futuros en DEMO para el contrato portal↔SimbioSphere; este fichero no ejecuta ni autoriza cambios en DEMO (ver [roadmap](../10-roadmap-backlog.md)).
- Módulo(s) owner: repo DEMO; cada cambio se ejecuta allí con su gobernanza M01–M12 y gate correspondiente.

### DEMO-01 — ADR "API de aprovisionamiento para partners" (auth, scopes, idempotencia, errores, versionado)
- Fase / épica / módulo owner: F1 / DEMO / DEMO
- Tipo: docs
- Depende de: —
- Descripción: Cubre: RF-SIM-09; define API de plataforma versionada, autenticación M2M, scopes, idempotencia, errores, rate limit y soporte de partners.
- Ficheros/paquetes a crear o modificar (aproximado): ADR de DEMO, contrato OpenAPI y registro de decisiones; trabajo futuro exclusivo del repo DEMO.
- Impacto modular: contrato `platform` sobre `OwnerOnboardingOrchestratorApi`; owners/CSV y gate quedan en tareas DEMO-10. Sin ciclo.
- UI: n/a (API M2M).
- Seguridad: comparar HMAC/OAuth2, scope mínimo, rotación y anti-replay; no escribir secretos/valores reales.
- Criterios de aceptación (Given/When/Then):
  - Given partners M2M When ADR se revisa Then define versionado, autenticación, permisos, idempotencia y compatibilidad.
  - Given decisión sin evidencia o secreto literal When se aprueba Then se bloquea y se documenta alternativa/verificación pendiente.
- Tests: TC-DEMO-01: revisión documental/amenazas, lint de ADR/OpenAPI.
- Documentación a actualizar: DIARIO + INVENTARIO del plan; ADR de DEMO futuro
- Estimación: M (≤ 1 d)
- Riesgos: el endpoint REST de alta debe ser nuevo: `/onboarding` es MVC+CSRF y no devuelve IDs (`iam/web/OwnerOnboardingController.java:29,36,46,63-68,74-84`); el orquestador citado es contrato interno (`shared/api/OwnerOnboardingOrchestratorApi.java:3,5`).

### DEMO-02a — Cadena de seguridad `/api/platform/v1/**` con filtro HMAC (CSRF exento solo ahí)
- Fase / épica / módulo owner: F2 / DEMO / DEMO
- Tipo: feat
- Depende de: DEMO-01
- Descripción: Cubre: RF-SIM-09; agrega cadena de filtros propia para API de plataforma y valida firma HMAC con ventana anti-replay.
- Ficheros/paquetes a crear o modificar (aproximado): `config/SecurityConfig`, filtro platform y tests en repo DEMO.
- Impacto modular: adapter platform llama API interna owner; no cambia cadena `/api/v1/**` ni módulos de negocio. Sin ciclo.
- UI: n/a (endpoint de máquina a máquina).
- Seguridad: eximir CSRF solo en `/api/platform/v1/**`; firma timestamp/method/URI/body/key id, comparación constante y deny-by-default.
- Criterios de aceptación (Given/When/Then):
  - Given firma válida dentro de ±300 s When POST platform llega Then autentica scopes y no altera seguridad MVC existente.
  - Given replay, body/URI manipulado o firma ausente When llega request Then 401/403 sin ejecutar servicio.
- Tests: TC-DEMO-02a: WT (cadena/CSRF scope), UT (HMAC/anti-replay), SEC (forged signature).
- Documentación a actualizar: DIARIO + INVENTARIO DEMO futuro; OpenAPI
- Estimación: M (≤ 1 d)
- Riesgos: patrón HMAC existente es webhook y URI canónica no incluye query (`WebhookHmacAuthenticationFilter.java:33-35,76-79,133-137,152-158`); definir canonical nuevo.

### DEMO-02b — Registro de clientes de plataforma: claves, scopes, rate limit y allowlist IP
- Fase / épica / módulo owner: F2 / DEMO / DEMO
- Tipo: feat
- Depende de: DEMO-02a
- Descripción: Cubre: RF-SIM-09; administra key ID, scopes, límites y allowlist opcional para clientes M2M.
- Ficheros/paquetes a crear o modificar (aproximado): módulo platform client/key registry, adapter de secret manager y migraciones según owner DEMO.
- Impacto modular: tabla/entidad con owner registrado en contratos DEMO; no reutiliza API key tenant. Sin ciclos.
- UI: n/a (alta/rotación operada por administración segura).
- Seguridad: secreto fuera de BD/repo, dos claves activas para rotación, scopes `provisioning:write/read`, rate limit por key id.
- Criterios de aceptación (Given/When/Then):
  - Given key activa con scope requerido When firma request válida Then autoriza solo operaciones permitidas y registra key ID.
  - Given key revocada/scope insuficiente/exceso cuota When llama endpoint Then deniega, limita y no registra secreto ni IP arbitraria.
- Tests: TC-DEMO-02b: UT (scopes/rotación), IT (rate limit), SEC (secreto/allowlist).
- Documentación a actualizar: DIARIO + INVENTARIO DEMO futuro; runbook rotación
- Estimación: M (≤ 1 d)
- Riesgos: el cliente M2M no debe compartir `X-API-Key` de tenant; autenticar separadamente.

### DEMO-03a — Idempotencia a nivel de plataforma (tabla nueva; conflicto por payload distinto)
- Fase / épica / módulo owner: F2 / DEMO / DEMO
- Tipo: feat
- Depende de: DEMO-01
- Descripción: Cubre: RF-SIM-03; implementa idempotencia pre-tenant, aislada de la tabla tenant/location actual.
- Ficheros/paquetes a crear o modificar (aproximado): módulo `platform-idempotency`, tabla/migración, índice único, request hash y respuesta cifrada.
- Impacto modular: owner nuevo registrado en inventario; no reutiliza `idempotency` tenant-scoped. Ciclo: ninguno declarado.
- UI: n/a (middleware de API).
- Seguridad: bind key+partner+operación, hash canonical del payload y cifrado de respuesta; TTL/auditoría sin PII.
- Criterios de aceptación (Given/When/Then):
  - Given misma clave y payload When se repite llamada Then reproduce mismo status/body sin repetir alta.
  - Given misma clave con payload distinto When se repite Then responde 409 `IDEMPOTENCY_KEY_REUSED` y no crea segunda cuenta.
- Tests: TC-DEMO-03a: UT (hash/conflicto), IT (concurrencia/índice), SEC (aislamiento).
- Documentación a actualizar: DIARIO + INVENTARIO DEMO futuro; ADR-01
- Estimación: M (≤ 1 d)
- Riesgos: módulo actual exige tenant+location (DEMO `IdempotencyReplay.java:21-22,29,31,40,50`); no sirve antes de crear tenant.

### DEMO-03b — `POST /api/platform/v1/owner-onboardings` sobre `OwnerOnboardingOrchestratorApi`
- Fase / épica / módulo owner: F3 / DEMO / DEMO
- Tipo: feat
- Depende de: DEMO-02a, DEMO-02b, DEMO-03a
- Descripción: Cubre: RF-SIM-03; expone onboarding JSON como adaptador fino sobre contrato interno con referencia externa e idempotencia.
- Ficheros/paquetes a crear o modificar (aproximado): controller/DTO/mapper platform, validadores, OpenAPI y tests en repo DEMO.
- Impacto modular: llama `shared.api.OwnerOnboardingOrchestratorApi`; dueño interno se conserva; respuesta exporta IDs acordados y tenantSlug. Sin ciclo.
- UI: n/a (M2M).
- Seguridad: no password; emailVerified requiere client M2M/evidence; términos versionados; no exponer reintento inseguro por email.
- Criterios de aceptación (Given/When/Then):
  - Given comando válido firmado When llega POST con `Idempotency-Key` Then responde 201 con IDs, tenantSlug y módulos aplicados.
  - Given validación fallida o mismo email sin idempotencia When solicita alta Then responde error genérico tipado y no devuelve IDs ajenos.
- Tests: TC-DEMO-03b: WT (endpoint/auth), MT (orchestrator), IT (idempotencia/rollback), CT.
- Documentación a actualizar: DIARIO + INVENTARIO DEMO futuro; OpenAPI
- Estimación: M (≤ 1 d)
- Riesgos: contrato interno devuelve IDs `Long` (DEMO `shared/api/OwnerOnboardingResult.java:4-9`); validar exposición pública.

### DEMO-03c — Errores RFC 7807 y no exponer el reintento por email
- Fase / épica / módulo owner: F3 / DEMO / DEMO
- Tipo: feat
- Depende de: DEMO-03b
- Descripción: Cubre: RF-SIM-09; normaliza errores de plataforma a Problem Details y bloquea reintento/lookup inseguro por email.
- Ficheros/paquetes a crear o modificar (aproximado): handler platform, tipos de error, tests de contrato y documentación.
- Impacto modular: errores del adapter platform; usa excepciones de API interna sin modificar contrato tenant. Sin tablas/ciclos.
- UI: n/a (respuesta API `application/problem+json`).
- Seguridad: email/slugs no revelan cuenta ajena; incluir correlation ID, no stack trace, body/PII ni credenciales.
- Criterios de aceptación (Given/When/Then):
  - Given email/slug conflictivo When API responde Then usa 409 con code estable y detalle no enumerador.
  - Given caller intenta reintento solo por email When procesa Then endpoint no busca/revela IDs existentes y registra rechazo seguro.
- Tests: TC-DEMO-03c: UT (mapeo RFC7807), WT (no enumeración), CT (schema/error codes).
- Documentación a actualizar: DIARIO + INVENTARIO DEMO futuro; OpenAPI
- Estimación: M (≤ 1 d)
- Riesgos: handler actual es propio y no RFC7807 (DEMO `api/web/ApiErrorHandler.java:31,49,132-139`); preservar APIs existentes.

### DEMO-04 — `GET /api/platform/v1/tenant-slugs/{slug}/availability` con sugerencias
- Fase / épica / módulo owner: F3 / DEMO / DEMO
- Tipo: feat
- Depende de: DEMO-02a
- Descripción: Cubre: RF-SIM-01; consulta disponibilidad/sugerencias de businessSlug sin crear tenant ni revelar propietario.
- Ficheros/paquetes a crear o modificar (aproximado): controller/DTO platform, query service, OpenAPI y tests de colisión.
- Impacto modular: consulta servicio de tenant/slug existente; sin nuevas tablas ni escritura. Sin ciclos.
- UI: n/a (endpoint consumido por S-portal).
- Seguridad: HMAC + scope `provisioning:read`, rate limit, normalización; no retornar email/owner existente.
- Criterios de aceptación (Given/When/Then):
  - Given slug libre con patrón permitido When se consulta Then responde available y sugerencias deterministas si se requieren.
  - Given slug inválido/ocupado When se consulta Then 400/200 unavailable sin datos del tenant propietario.
- Tests: TC-DEMO-04: WT (validación/auth), IT (unicidad), CT (schema/sugerencias).
- Documentación a actualizar: DIARIO + INVENTARIO DEMO futuro; OpenAPI
- Estimación: S (≤ 0,5 d)
- Riesgos: validación actual regex/uniqueness es global (`OwnerOnboardingRequest.java:34,48,55-56`); mantener contrato alineado.

### DEMO-05 — Referencia externa (`S_PORTAL`, `externalAccountId`) en tabla nueva con migraciones en ambos árboles
- Fase / épica / módulo owner: F2 / DEMO / DEMO
- Tipo: feat
- Depende de: DEMO-01
- Descripción: Cubre: RF-SIM-06; persiste referencia externa con source/owner y unicidad para vincular cuenta de S-portal sin email matching.
- Ficheros/paquetes a crear o modificar (aproximado): entidad/tabla external reference, registro owner y migraciones H2 + SQL Server en repo DEMO futuro.
- Impacto modular: tabla nueva con owner DEMO registrado; no agrega `externalId` a entidades existentes sin decisión. Sin ciclos.
- UI: n/a (referencia M2M).
- Seguridad: `externalAccountId` no es secreto; validar partner scope, no usar email para resolver cuenta y auditar actor M2M.
- Criterios de aceptación (Given/When/Then):
  - Given alta con source=S_PORTAL y externalAccountId When se persiste Then reconsulta por partner devuelve el mismo vínculo.
  - Given misma referencia asociada a otra cuenta When se inserta Then constraint rechaza duplicado sin fusionar usuarios.
- Tests: TC-DEMO-05: UT (unicidad), IT (dos árboles de migración), MT (owner/contrato).
- Documentación a actualizar: DIARIO + INVENTARIO DEMO futuro; README/CHANGELOG
- Estimación: M (≤ 1 d)
- Riesgos: no añadir un campo externo a `User` por suposición; revisar el owner de identidad (`iam/domain/User.java:52,55,58,85,88`) y registrar la tabla/owner en DEMO-10.

### DEMO-06a — Propietario sin contraseña conocida y `emailVerified` de cliente de confianza con auditoría
- Fase / épica / módulo owner: F4 / DEMO / DEMO
- Tipo: feat
- Depende de: DEMO-03b, DEMO-05
- Descripción: Cubre: RF-SIM-04, RF-SIM-05; permite crear propietario vía cliente de confianza sin contraseña conocida y registrar evidencia verificada.
- Ficheros/paquetes a crear o modificar (aproximado): servicio IAM/platform adapter, flag trusted client, auditoría y tests en repo DEMO.
- Impacto modular: platform invoca `OwnerOnboardingOrchestratorApi`; identity mantiene usuario; referencia externa owner DEMO-05. Sin ciclos.
- UI: n/a (M2M; activación en DEMO-06b).
- Seguridad: confiar `emailVerified` solo con scope/key autorizado y evidencia; no confiarlo desde formulario público.
- Criterios de aceptación (Given/When/Then):
  - Given partner autenticado con scope y evidencia When crea owner Then no recibe ni persiste contraseña y marca verificación auditada.
  - Given caller sin scope/evidencia When envía emailVerified=true Then 403/422 y usuario no queda verificado.
- Tests: TC-DEMO-06a: WT (auth/scope), MT (IAM/orchestrator), SEC (trust boundary/audit).
- Documentación a actualizar: DIARIO + INVENTARIO DEMO futuro; ADR-002
- Estimación: M (≤ 1 d)
- Riesgos: DEMO verifica email por defecto (`SecurityConfig.java:221-222`); no debilitar login normal.

### DEMO-06b — `POST …/{customerAccountId}/activation-links` (enlace just-in-time de un solo uso)
- Fase / épica / módulo owner: F4 / DEMO / DEMO
- Tipo: feat
- Depende de: DEMO-06a
- Descripción: Cubre: RF-SIM-04; emite enlace de un uso para que propietario defina password justo al acceder, con TTL 15 min.
- Ficheros/paquetes a crear o modificar (aproximado): endpoint platform, servicio de activación, template UI DEMO y pruebas anti-replay.
- Impacto modular: adapter platform llama servicio IAM; token owner IAM, nunca se devuelve en eventos. Sin ciclo.
- UI: página de definición de contraseña con estados vencido/usado y marca consistente.
- Seguridad: HMAC+scope provisioning:write; token aleatorio/hashado, caducidad, una vez, rate limit y sin log/persistencia en S-portal.
- Criterios de aceptación (Given/When/Then):
  - Given cuenta provisionada y partner autorizado When solicita link Then recibe URL de un uso válida 15 min.
  - Given enlace expirado/reutilizado o customerAccountId ajeno When intenta activar Then se deniega sin revelar estado de otra cuenta.
- Tests: TC-DEMO-06b: WT (auth/flow), UT (TTL/replay), SEC (token/IDOR), CT.
- Documentación a actualizar: DIARIO + INVENTARIO DEMO futuro; OpenAPI
- Estimación: M (≤ 1 d)
- Riesgos: tokens de verificación/reset están en claro en el modelo actual (`iam/domain/User.java:85,88`); el nuevo enlace debe persistirse hasheado e independiente.

### DEMO-07 — Persistir la aceptación de términos (versión y fecha)
- Fase / épica / módulo owner: F4 / DEMO / DEMO
- Tipo: feat
- Depende de: DEMO-03b
- Descripción: Cubre: RF-SIM-01; guarda versión/fecha de términos de SimbioSphere aceptados en provisioning M2M.
- Ficheros/paquetes a crear o modificar (aproximado): DTO command, tabla/evidencia owner definido, migraciones y mapper de onboarding.
- Impacto modular: persistencia asignada a owner registrado; aceptación se relaciona con CustomerAccount y source externo. Sin ciclos.
- UI: n/a (portal recoge términos separados; DEMO conserva evidencia).
- Seguridad: timestamp server/client normalizado UTC, versión obligatoria y firma M2M; no tratar flag booleano solo como evidencia.
- Criterios de aceptación (Given/When/Then):
  - Given payload con terms.version y acceptedAt When onboarding confirma Then conserva versión/fecha asociada a customer account.
  - Given términos ausentes o fecha inválida When procesa Then 400/422 sin alta parcial ni aceptación inferida.
- Tests: TC-DEMO-07: UT (mapper/UTC), IT (persistencia), CT (schema), SEC (integridad).
- Documentación a actualizar: DIARIO + INVENTARIO DEMO futuro; OpenAPI/ADR
- Estimación: M (≤ 1 d)
- Riesgos: contrato MVC actual no persiste `acceptTerms` (OwnerOnboardingController.java:63-66); conservar ruta legacy.

### DEMO-08 — Módulos por defecto según el tipo cuando no hay selección
- Fase / épica / módulo owner: F4 / DEMO / DEMO
- Tipo: feat
- Depende de: DEMO-03b
- Descripción: Cubre: RF-SIM-02; implementa defaults filtrados por tipo/dependencias solo cuando modules se omite/null.
- Ficheros/paquetes a crear o modificar (aproximado): `BusinessModule.defaultSelection`, resolver por BusinessType, tests de matriz y API DTO.
- Impacto modular: módulo IAM/negocio owner DEMO; respeta ownership de BusinessModule y registro de contratos. Sin ciclos.
- UI: n/a (portal delega la selección).
- Seguridad: validar que módulos no aplicables/dependientes no se habilitan; documentar respuesta `modulesApplied`.
- Criterios de aceptación (Given/When/Then):
  - Given modules omitido/null When tipo RESTAURANT/RETAIL/SERVICES/REPAIR_SHOP Then aplica defaults válidos filtrados por reglas.
  - Given modules=[] When procesa Then selecciona cero opcionales; no sustituye por defaults ni habilita INVENTORY inválido.
- Tests: TC-DEMO-08: UT (matriz tipo/dependencias), IT (persistencia), CT (omitido/null/[]).
- Documentación a actualizar: DIARIO + INVENTARIO DEMO futuro; README/OpenAPI
- Estimación: M (≤ 1 d)
- Riesgos: hoy null/vacío da cero módulos y defaultSelection no tiene callers (H16/H17; `BusinessModuleSelectionValidator.java:20`, `BusinessModule.java:78-79`).

### DEMO-10 — Contratos y owners registrados, gate ArchUnit 8/8 y documentación de DEMO
- Fase / épica / módulo owner: F5 / DEMO / DEMO
- Tipo: test
- Depende de: DEMO-03c, DEMO-04, DEMO-05, DEMO-06b, DEMO-07, DEMO-08
- Descripción: Cubre: RF-SIM-09; registra tablas/APIs en artifacts modulares y alinea OpenAPI, README, inventario y changelog.
- Ficheros/paquetes a crear o modificar (aproximado): `module-api-contracts.csv`, `entity-ownership.csv`, `api-spec.yaml`, tests de arquitectura/documentos.
- Impacto modular: cada API/tabla tiene un owner único y named contract; no se añade dependencia/ciclo entre módulos.
- UI: n/a (gobernanza).
- Seguridad: gate requiere tests, no secretos ni PII en contratos/CSV; rutas platform aisladas de APIs tenant.
- Criterios de aceptación (Given/When/Then):
  - Given módulos/contratos nuevos When corre gate Then todos los owners registrados y ArchUnit mantiene 8/8.
  - Given OpenAPI desincronizado, dependencia prohibida o README incompleto When corre CI Then job falla y bloquea merge.
- Tests: TC-DEMO-10: UT (contratos/owners), MT (ArchUnit), CT (OpenAPI), CI (gate 8/8).
- Documentación a actualizar: DIARIO + INVENTARIO DEMO futuro; README/CHANGELOG/api-spec
- Estimación: M (≤ 1 d)
- Riesgos: catálogo OpenAPI actual puede estar desincronizado (README.md:96,141); validar against code.

### DEMO-11 — Tests de proveedor del contrato, aislamiento y fallo (M09)
- Fase / épica / módulo owner: F5 / DEMO / DEMO
- Tipo: test
- Depende de: DEMO-03c, DEMO-06b, DEMO-08
- Descripción: Cubre: RNF-TST-02; verifica provider contract, autorización partner, aislamiento, fallo/reintento y patrones M09.
- Ficheros/paquetes a crear o modificar (aproximado): provider contract suite, integration fixtures y testcontainers para SQL Server.
- Impacto modular: tests de módulos IAM/platform; no acceden a S-portal ni saltan owners/repositorios. Sin ciclos.
- UI: n/a (contrato/API).
- Seguridad: test scopes, anti-replay, payload distinto, IDOR y secretos; solo datos sintéticos.
- Criterios de aceptación (Given/When/Then):
  - Given request contractual válido When proveedor procesa Then schema/status/efecto coinciden con OpenAPI y el replay es idempotente.
  - Given fallo parcial, firma errónea o tenant/location de otro cliente When test ejecuta Then operación se aísla/deniega y no hay escritura cruzada.
- Tests: TC-DEMO-11: CT (provider), IT (SQL Server/aislamiento), SEC (auth/replay), M09.
- Documentación a actualizar: DIARIO + INVENTARIO DEMO futuro; reporte de contrato
- Estimación: M (≤ 1 d)
- Riesgos: H2 no sustituye la verificación específica del contrato sobre SQL Server; ejecutar provider suite con Testcontainers y registrar los motores/fixtures cubiertos.

## Épicas DEMO R2 no desglosadas
- **DEMO-09 (R2):** vínculo de cuentas existentes con prueba de posesión, webhooks de ciclo de vida y consulta de estado; requiere decisión de contrato y seguridad.
- **DEMO-12 (R2):** catálogo consultable de tipos/módulos para selector futuro; sus defaults deben conservar la semántica omitido/null frente a `[]` de DEMO-08.
