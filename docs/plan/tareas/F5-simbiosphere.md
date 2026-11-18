# F5 — Integración SimbioSphere (alta conjunta, saga y panel): tareas

- Objetivo: aprovisionar tenant mediante contrato M2M sin transacción distribuida ni credenciales duplicadas (hito M5; ver [roadmap](../10-roadmap-backlog.md)).
- Módulo(s) owner: `simbiosphere` en portal; la pista DEMO es externa y no se modifica durante esta planificación.

### SIM-001 — simbiosphere: modelo de aprovisionamiento y vínculo con migraciones
- Fase / épica / módulo owner: F5 / SIM / simbiosphere
- Tipo: feat
- Depende de: FND-006, CUS-001
- Descripción: Cubre: RF-SIM-03, RF-SIM-06; modela provisioning, referencia externa y negocios vinculados con estados explícitos.
- Ficheros/paquetes a crear o modificar (aproximado): `simbiosphere.{api,domain,application,infrastructure}`, `simbiosphere.provisioning`, `account_link`, `linked_business`.
- Impacto modular: tablas owner `simbiosphere`; consume `customer.api`, publica `ProvisioningStateChanged` con IDs. Ciclo: request→simbiosphere se añade solo en SIM-014.
- UI: n/a (dominio; panel SIM-008/010).
- Seguridad: `public_id` externo, unicidad `(external_system, external_account_id)`; jamás vincular por email ni guardar token/password DEMO.
- Criterios de aceptación (Given/When/Then):
  - Given alta cliente con provisioning When persiste Then comienza PENDING y se relaciona con ClientAccount por ID estable.
  - Given CustomerAccount con varios tenants When se modela vínculo Then admite 1..N negocios sin asumir ownerUserId en Tenant.
- Tests: TC-SIM-001: UT (máquina/uniqueness), IT de repositorios con H2, revisión estática de migraciones y MT (API/evento). No se requiere ejecutar migraciones SQL Server.
- Documentación a actualizar: DIARIO + INVENTARIO; ER 04 y saga 05
- Estimación: M (≤ 1 d)
- Riesgos: la relación OWNER sale de CustomerAccountMembership y Tenant no tiene owner directo (`iam/application/OwnerOnboardingService.java:50-61`, `tenant/domain/Tenant.java:62`); no limitar vínculo a un tenant.

### SIM-003 — Cliente HTTP tipado con HMAC, timeouts, circuit breaker y errores RFC 7807
- Fase / épica / módulo owner: F5 / SIM / simbiosphere
- Tipo: feat
- Depende de: SIM-002, FND-007
- Descripción: Cubre: RF-SIM-09; implementa cliente tipado sobre OpenAPI con firma HMAC, timeouts y mapeo de Problem Details.
- Ficheros/paquetes a crear o modificar (aproximado): `simbiosphere.infrastructure.DemoClient`, signer, properties y `ProblemDetailMapper`.
- Impacto modular: `simbiosphere.api` no expone transporte; llama solo a host externo; sin tablas/eventos nuevos. Ciclo: ninguno.
- UI: n/a (adapter HTTP; errores se muestran por SIM-007).
- Seguridad: secreto por key ID del gestor externo, timestamp anti-replay, connect 3 s/read 10 s; no PII en logs.
- Criterios de aceptación (Given/When/Then):
  - Given contrato y secreto de test When se llama API Then firma canonical, `Idempotency-Key` y HMAC se validan con test vector.
  - Given timeout/Problem Details desconocido When responde DEMO Then circuit breaker limita llamadas y mapea error sin filtrar body/credenciales.
- Tests: TC-SIM-003: UT (canonical/HMAC), CT (request/response), IT (timeouts/circuit breaker).
- Documentación a actualizar: DIARIO + INVENTARIO; ADR-002
- Estimación: M (≤ 1 d)
- Riesgos: Resilience4j/RestClient con Boot 4 se verifica en FND-001; HMAC se inspira en `webhook-service/src/main/java/com/misnegocios/orders/webhookservice/security/WebhookHmacAuthenticationFilter.java:33-35,76-79,133-137,152-158`.

### SIM-004 — Stubs WireMock y tests de contrato
- Fase / épica / módulo owner: F5 / SIM / simbiosphere
- Tipo: test
- Depende de: SIM-003
- Descripción: Cubre: RF-SIM-09, RNF-TST-02; valida request/response del cliente contra OpenAPI usando WireMock, sin depender de DEMO desplegado.
- Ficheros/paquetes a crear o modificar (aproximado): `src/test/resources/contracts/simbiosphere/`, WireMock tests y validación OpenAPI.
- Impacto modular: test del adapter `simbiosphere`; no crea contrato paralelo ni tablas. Ciclo: ninguno.
- UI: n/a (test de protocolo).
- Seguridad: secretos/firmas efímeros; stubs sin PII real; comprobar no envío de password y no logging de body.
- Criterios de aceptación (Given/When/Then):
  - Given OpenAPI del contrato When se generan stubs Then operaciones, códigos y schemas se validan en `mvn test`.
  - Given firma inválida, 409/422/503 o payload incompatible When corre test Then fallo queda clasificado sin llamada de red a DEMO.
- Tests: TC-SIM-004: CT (WireMock/OpenAPI), UT (errores/HMAC), IT (build offline).
- Documentación a actualizar: DIARIO + INVENTARIO; contrato en 05
- Estimación: M (≤ 1 d)
- Riesgos: plugins/versiones de contrato cambian; fijar artefactos y revisar diff.

### SIM-005 — Aprovisionamiento local en el registro y transición tras verificar el email
- Fase / épica / módulo owner: F5 / SIM / simbiosphere
- Tipo: feat
- Depende de: SIM-001, CUS-002
- Descripción: Cubre: RF-SIM-03, RF-SIM-05; guarda intención PENDING junto al registro y la marca READY solo tras evidencia requerida.
- Ficheros/paquetes a crear o modificar (aproximado): listener `ClientRegistered/EmailVerified`, `ProvisioningService`, outbox/EPR y evento con IDs.
- Impacto modular: simbiosphere consume `customer.api`/identity event publicado; request no invoca adapter remoto. Sin ciclos.
- UI: indicador de alta pendiente y términos SimbioSphere separados.
- Seguridad: llamada remota no ocurre en transacción local; invitación ADMIN espera aceptación de términos.
- Criterios de aceptación (Given/When/Then):
  - Given registro conjunto When User todavía no verifica email Then provisioning queda PENDING sin HTTP remoto.
  - Given EmailVerified confiable When evento se procesa Then transita a READY idempotente y outbox conserva IDs, no credenciales.
- Tests: TC-SIM-005: UT (estados), MT (eventos/EPR), IT (transacción/no HTTP antes de verificar).
- Documentación a actualizar: DIARIO + INVENTARIO; secuencia 05
- Estimación: M (≤ 1 d)
- Riesgos: evento perdido bloquea alta; EPR y métrica por estado obligatorios.

### SIM-006 — Worker de aprovisionamiento con claim, backoff y estados
- Fase / épica / módulo owner: F5 / SIM / simbiosphere
- Tipo: feat
- Depende de: SIM-005, SIM-003
- Descripción: Cubre: RF-SIM-03; reclama READY, llama DEMO fuera de transacción y guarda PROVISIONED/ACTION_REQUIRED/reintento/FAILED.
- Ficheros/paquetes a crear o modificar (aproximado): `simbiosphere.application.ProvisioningWorker`, repositorio claim, lease y scheduler.
- Impacto modular: solo `simbiosphere.provisioning`; envía `ProvisioningStateChanged` con IDs. Sin dependencia en DEMO Java.
- UI: estado visible a cliente/ADMIN por SIM-007/010.
- Seguridad: SQL Server `UPDLOCK, READPAST`, lease, idempotency key = public_id; credenciales fuera de transacción.
- Criterios de aceptación (Given/When/Then):
  - Given dos workers y una fila READY When reclaman Then solo uno procesa y la llamada ocurre sin transacción abierta.
  - Given fallo técnico temporal When reintenta Then aplica 30 s/2 m/10 m/30 m/2 h/6 h con jitter y termina FAILED al agotar.
- Tests: TC-SIM-006: UT (backoff/claim), IT (concurrencia/restart), CT (idempotencia), SEC (sin logs PII).
- Documentación a actualizar: DIARIO + INVENTARIO; runbook 09
- Estimación: M (≤ 1 d)
- Riesgos: respuesta remota perdida tras éxito; misma Idempotency-Key y reconciliación por estado.

### SIM-007 — Errores de negocio: ACTION_REQUIRED y UI de corrección
- Fase / épica / módulo owner: F5 / SIM / simbiosphere
- Tipo: feat
- Depende de: SIM-006, UIK-008
- Descripción: Cubre: RF-SIM-03; convierte conflictos de email/slug/módulos en ACTION_REQUIRED con pasos de corrección y reintento.
- Ficheros/paquetes a crear o modificar (aproximado): `simbiosphere.application.BusinessErrorMapper`, controller y `templates/simbiosphere/action-required.html`.
- Impacto modular: estados/error codes owner simbiosphere; notifica por `notification.api`; sin lectura de repos DEMO. Sin ciclos.
- UI: `/cuenta/simbiosphere/accion-requerida`, campos editables según error, traducción y confirmación.
- Seguridad: revalidar propietario, no mostrar detalles de cuentas DEMO ajenas; corrección bajo sesión y CSRF.
- Criterios de aceptación (Given/When/Then):
  - Given respuesta 409 SLUG_TAKEN When cliente propone slug libre Then actualiza comando y reencola con misma cuenta/idempotency policy documentada.
  - Given error 409 email existente When corrige cuenta When prueba de posesión no existe Then no vincula ni revela propietario.
- Tests: TC-SIM-007: UT (problem mapping), WT (corrección/owner), SEC (no enumeración), MT (notificación).
- Documentación a actualizar: DIARIO + INVENTARIO; errores 05
- Estimación: M (≤ 1 d)
- Riesgos: el reintento interno busca owner solo por email (`platform/application/PlatformOwnerOnboardingService.java:38-41,67-84`); usar contrato idempotente y no exponerlo.

### SIM-008 — Panel "Mi SimbioSphere" con enlace de acceso y activación de contraseña
- Fase / épica / módulo owner: F5 / SIM / simbiosphere
- Tipo: feat
- Depende de: SIM-006, DEMO-06b
- Descripción: Cubre: RF-SIM-04, RF-SIM-07; muestra estado/tenant y solicita enlace de activación just-in-time a DEMO al pulsar acción.
- Ficheros/paquetes a crear o modificar (aproximado): `simbiosphere.web.MySimbioSphereController`, client activation API, panel fragment y bundles.
- Impacto modular: simbiosphere consulta su vínculo; consume cliente HTTP; no guarda activation token ni consulta DB DEMO. Sin ciclos.
- UI: `/cuenta/simbiosphere`; estado, tipo, negocio, login URL y botón de activación.
- Seguridad: owner-only, enlace un uso de 15 min, no persistir ni loguear token; login DEMO según `config/SecurityConfig.java:177-179`.
- Criterios de aceptación (Given/When/Then):
  - Given estado PROVISIONED When titular solicita activar Then redirige al enlace recién emitido y no lo guarda en portal.
  - Given usuario distinto o API sin DEMO-06b When abre panel/activa Then deniega u ofrece estado pendiente sin filtrar tenant.
- Tests: TC-SIM-008: WT (panel/owner), CT (activation response), SEC (token no persistido), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; flujo en 05
- Estimación: M (≤ 1 d)
- Riesgos: enlace requiere tarea DEMO-06b; no usar password ni reutilizar token en panel.

### SIM-009 — Campos del alta conjunta y comprobación de slug
- Fase / épica / módulo owner: F5 / SIM / simbiosphere
- Tipo: feat
- Depende de: SIM-003, CUS-002, DEMO-04
- Descripción: Cubre: RF-SIM-01, RF-SIM-02; captura negocio/sede/tipo/slug y delega selección de módulos por defecto a DEMO.
- Ficheros/paquetes a crear o modificar (aproximado): `simbiosphere.web.OnboardingFields`, slug availability client, templates y bundles.
- Impacto modular: simbiosphere consume customer identity por API; crea intención via SIM-005; no replica reglas DEMO. Sin ciclos.
- UI: checkbox «crear negocio», 4 tipos traducidos, slug editable/autosugerido y sede inicial.
- Seguridad: validar rangos/regex con contrato; aceptación de términos SimbioSphere separada; no enviar password.
- Criterios de aceptación (Given/When/Then):
  - Given tipo/slug válido When consulta disponibilidad Then muestra libre/ocupado y permite completar sin enviar credenciales.
  - Given `modules` omitido/null When portal llama DEMO Then pide defaults; `[]` se reserva para cero opcionales, según evidencias H16/H17 en 05.
- Tests: TC-SIM-009: WT (campos/locales), CT (availability/modules), SEC (validación/consentimiento).
- Documentación a actualizar: DIARIO + INVENTARIO; catálogo OpenAPI 05
- Estimación: M (≤ 1 d)
- Riesgos: selección vacía en DEMO da cero módulos opcionales (`BusinessModuleSelectionValidator.java:20`; `CustomerAccountCapabilityService.java:64,69-70`).

### SIM-010 — Backoffice: estado, reintento manual y alertas de aprovisionamiento
- Fase / épica / módulo owner: F5 / SIM / simbiosphere
- Tipo: feat
- Depende de: SIM-006, NTF-004, USR-002
- Descripción: Cubre: RF-SIM-07, RF-USR-02; ofrece consulta de estado y reintento manual con alertas a ADMIN.
- Ficheros/paquetes a crear o modificar (aproximado): `simbiosphere.web.AdminProvisioningController`, query/command services, alert template.
- Impacto modular: simbiosphere consulta sus tablas y `identity.api`; notification recibe eventos; sin escritura de tablas externas. Sin ciclos.
- UI: panel de aprovisionamiento en ficha/admin con estado, intentos, error sanitizado y `correlationId`.
- Seguridad: `PROVISIONING_MANAGE`; ADMIN-only reintento, confirmación, auditoría e idempotencia.
- Criterios de aceptación (Given/When/Then):
  - Given provisioning FAILED/ACTION_REQUIRED When ADMIN reintenta Then se registra actor y la fila vuelve a estado procesable.
  - Given EMPLOYEE sin permiso o fila PROVISIONED When solicita reintento Then 403/409 y no se duplica tenant.
- Tests: TC-SIM-010: WT (panel/permiso), IT (reintento/idempotencia), SEC (rol), MT (alerta).
- Documentación a actualizar: DIARIO + INVENTARIO; runbook 09
- Estimación: M (≤ 1 d)
- Riesgos: exponer texto remoto puede filtrar PII; mostrar error normalizado y truncado.

### SIM-011 — Alternativa temporal: enlace a /onboarding de DEMO tras feature flag
- Fase / épica / módulo owner: F5 / SIM / simbiosphere
- Tipo: feat
- Depende de: CUS-002
- Descripción: Cubre: RF-SIM-10; ofrece alternativa temporal a alta portal cuando la API M2M no está lista, sin vínculo automático.
- Ficheros/paquetes a crear o modificar (aproximado): feature flag/URL por entorno, CTA y aviso de deuda técnica.
- Impacto modular: `simbiosphere` no provisiona ni escribe en DEMO; link externo del portal. Sin tablas/eventos. Ciclo: ninguno.
- UI: botón «Continuar en SimbioSphere» externo con aviso y retorno manual posible.
- Seguridad: URL HTTPS allowlisted por entorno; no pasar PII/credenciales ni scrape CSRF.
- Criterios de aceptación (Given/When/Then):
  - Given flag habilitado y API no disponible When cliente elige alternativa Then abre `/onboarding` externo sin enviar datos del formulario.
  - Given flag deshabilitado o URL no allowlisted When renderiza Then oculta CTA y no redirige a host arbitrario.
- Tests: TC-SIM-011: UT (flag/allowlist), WT (CTA), SEC (open redirect/no PII).
- Documentación a actualizar: DIARIO + INVENTARIO; deuda técnica en 05
- Estimación: S (≤ 0,5 d)
- Riesgos: DEMO `/onboarding` es MVC y no devuelve IDs (`iam/web/OwnerOnboardingController.java:29,36,46,63-68,74-84`); no simular integración.

### SIM-012 — Diseño y puertos de vinculación de cuenta existente (implementación R2)
- Fase / épica / módulo owner: F5 / SIM / simbiosphere
- Tipo: docs
- Depende de: SIM-001
- Descripción: Cubre: RF-SIM-06; especifica puerto/API para vincular cuenta existente solo tras prueba de posesión; ejecución queda R2.
- Ficheros/paquetes a crear o modificar (aproximado): `simbiosphere.api.AccountLinkPort`, ADR-002, secuencia/contrato en 05; sin flujo productivo MVP.
- Impacto modular: dueño futuro `simbiosphere`; referencia customer por API; no se añade dependencia request ni se ejecuta vínculo. Sin ciclos.
- UI: flujo propuesto de confirmación OTP o login DEMO, no habilitado en MVP.
- Seguridad: nunca match por email; OTP de 6 dígitos, 10 min, 5 intentos, rate limit y audit según contrato.
- Criterios de aceptación (Given/When/Then):
  - Given cuenta DEMO existente When se redacta contrato Then exige prueba de posesión y define estados/errores sin vincular automáticamente.
  - Given solo email coincidente When se intenta asociar Then diseño/port deniega y produce evento auditable sin guardar cuenta vinculada.
- Tests: TC-SIM-012: CT (schema de futuro), revisión STRIDE/SEC; no test de feature activa.
- Documentación a actualizar: DIARIO + INVENTARIO; ADR-002/05; marcar R2
- Estimación: S (≤ 0,5 d)
- Riesgos: cambiar flujo de identidad afecta cuentas; no implementar hasta revisión de seguridad R2.

### SIM-013 — Tests de contrato contra DEMO real en entorno compartido
- Fase / épica / módulo owner: F5 / SIM / simbiosphere
- Tipo: test
- Depende de: SIM-004, DEMO-10, DEMO-11
- Descripción: Cubre: RNF-TST-02; ejecuta pruebas pactadas contra entorno compartido DEMO además de stubs locales.
- Ficheros/paquetes a crear o modificar (aproximado): profile `contractTest`, suite `simbiosphere`, workflow con secreto externo y reporte.
- Impacto modular: tests HTTP del adapter; no escribe BD DEMO por acceso directo; limpieza solo vía API contrato. Sin ciclo productivo.
- UI: n/a (verificación CI/entorno compartido).
- Seguridad: credenciales HMAC desde secret manager, scope mínimo, datos de prueba aislados y red restringida.
- Criterios de aceptación (Given/When/Then):
  - Given API DEMO compatible y cuenta de test aislada When corre contrato Then valida schema, idempotencia y errores sin tocar datos ajenos.
  - Given entorno/secret ausente When workflow se ejecuta Then job queda skipped/blocked explícito, no usa fallback inseguro.
- Tests: TC-SIM-013: CT (DEMO real), IT (limpieza/aislamiento), SEC (scope/secreto).
- Documentación a actualizar: DIARIO + INVENTARIO; matriz CI en 08/09
- Estimación: M (≤ 1 d)
- Riesgos: disponibilidad/latencia de otro repo; separar de build offline y coordinar ventana.

### SIM-014 — Selector de negocio vinculado en la sugerencia de módulo
- Fase / épica / módulo owner: F5 / SIM / request
- Tipo: feat
- Depende de: SIM-006, REQ-006
- Descripción: Cubre: RF-REQ-03; permite elegir negocio ya vinculado al crear sugerencia de módulo SimbioSphere.
- Ficheros/paquetes a crear o modificar (aproximado): `request.web.ModuleSuggestionController`, consumo `simbiosphere.api.LinkedBusinessQuery`, fragmento selector.
- Impacto modular: request→simbiosphere.api permitido desde F5; tabla/vínculos owner simbiosphere, solicitud owner request. Sin ciclo.
- UI: selector solo si hay negocios del titular; tipo y nombre accesibles, fallback sin vínculo.
- Seguridad: `ClientAccount` del actor determina opciones; validar vínculo en servidor en envío, no confiar en publicId del browser.
- Criterios de aceptación (Given/When/Then):
  - Given titular con varios negocios vinculados When carga formulario Then lista solo sus negocios y guarda referencia elegida.
  - Given publicId de otro titular When manipula selector Then responde 403/422 y no asocia el negocio ajeno.
- Tests: TC-SIM-014: WT (selector/propietario), SEC (IDOR), MT (API boundary).
- Documentación a actualizar: DIARIO + INVENTARIO; contrato UI en inventario
- Estimación: S (≤ 0,5 d)
- Riesgos: una CustomerAccount puede agrupar tenants (`iam/application/OwnerOnboardingService.java:50-61`, `tenant/domain/Tenant.java:62`); no limitar vínculo a uno.

### E2E-004 — E2E: registro conjunto y alta de solicitud
- Fase / épica / módulo owner: F5 / E2E / test
- Tipo: test
- Depende de: SIM-008, SIM-009, REQ-005, UIK-009, SIM-013
- Descripción: Cubre: RNF-TST-03; automatiza registro cliente, verificación, aprovisionamiento, activación y creación de solicitud.
- Ficheros/paquetes a crear o modificar (aproximado): `e2e/joint-onboarding.spec.ts`, fixtures WireMock/DEMO compartido, Mailpit y clock.
- Impacto modular: prueba identity/customer/simbiosphere/request por UI/API pública; sin acceso directo a BD DEMO. Sin ciclos.
- UI: recorrido ES/EN, desktop/móvil y estados PENDING/PROVISIONED/ACTION_REQUIRED.
- Seguridad: credenciales/secretos de test efímeros; no guardar activation token ni PII en evidencia.
- Criterios de aceptación (Given/When/Then):
  - Given persona y negocio válidos When verifica email y DEMO responde Then ve tenant, activa contraseña y crea solicitud.
  - Given DEMO retorna SLUG_TAKEN/timeout When E2E continúa Then muestra estado accionable/reintento idempotente sin duplicar usuario/tenant.
- Tests: TC-E2E-004: E2E (Playwright), CT (contrato), SEC (retry/PII), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; README «Recorrido conjunto»
- Estimación: M (≤ 1 d)
- Riesgos: entorno compartido puede no estar listo; stubs son obligatorios para ejecución local.
