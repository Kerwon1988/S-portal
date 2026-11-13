# F3 — Presupuestos y portal de empleados (gestión operativa de solicitudes): tareas

- Objetivo: completar presupuestos, vistas de trabajo y asignaciones para el hito solicitud→presupuesto→aceptación (ver [roadmap](../10-roadmap-backlog.md)).
- Módulo(s) owner: `request`; trabajador/cuenta se consultan mediante `staff.api` y `customer.api`.

### QUO-001 — Presupuesto versionado: modelo, estados e IVA
- Fase / épica / módulo owner: F3 / QUO / request
- Tipo: feat
- Depende de: REQ-001
- Descripción: Cubre: RF-EMP-03; modela presupuesto versionado, importe/moneda/impuestos, validez y estado DRAFT/SENT/ACCEPTED/REJECTED/EXPIRED.
- Ficheros/paquetes a crear o modificar (aproximado): `request.domain.Quote`, `request.quote`, `quote_version`, migración e invariantes.
- Impacto modular: tablas owner `request`; relación a Request por ID, sin entidad de otro módulo. Sin eventos en modelo inicial.
- UI: n/a (dominio; emisión en QUO-002 y aceptación en QUO-003).
- Seguridad: `DECIMAL(19,4)`, moneda ISO `CHAR(3)`, evidencia por actor/fecha; no activar pagos.
- Criterios de aceptación (Given/When/Then):
  - Given presupuesto válido When se versiona Then conserva historial, IVA, moneda, validez y estado inicial DRAFT.
  - Given intento de editar versión enviada When se persiste Then crea nueva versión o rechaza, nunca sobrescribe evidencia aceptada.
- Tests: TC-QUO-001: UT (estados/importes/versionado) e IT de constraints/repositorios con H2. No se requieren pruebas SQL Server/Testcontainers.
- Documentación a actualizar: DIARIO + INVENTARIO; modelo ER de presupuesto
- Estimación: M (≤ 1 d)
- Riesgos: fiscalidad depende de país; confirmar reglas antes de emitir presupuesto legal.

### QUO-002 — Emisión y envío del presupuesto por el empleado
- Fase / épica / módulo owner: F3 / QUO / request
- Tipo: feat
- Depende de: QUO-001, EMP-003
- Descripción: Cubre: RF-EMP-03; permite crear versión, revisar, marcar SENT y notificar al cliente con enlace autenticado.
- Ficheros/paquetes a crear o modificar (aproximado): `request.application.QuoteService`, `request.web`, template email/evento `QuoteSent`.
- Impacto modular: publica evento con IDs para notification; tablas quote owner request; consume customer/staff por API. Sin ciclo.
- UI: `/gestion/solicitudes/{id}/presupuesto`, preview y confirmación; documento traducido.
- Seguridad: permiso `QUOTES_MANAGE`, CSRF, validar propietario/destinatario y no incluir PII en evento/log.
- Criterios de aceptación (Given/When/Then):
  - Given EMPLOYEE autorizado y DRAFT válido When envía presupuesto Then pasa a SENT, guarda versión y crea notificación outbox.
  - Given rol sin permiso o plazo/importe inválido When intenta enviar Then recibe 403/422 y cliente no recibe aviso.
- Tests: TC-QUO-002: UT (validaciones), WT (permiso/CSRF), MT (QuoteSent/outbox), SEC.
- Documentación a actualizar: DIARIO + INVENTARIO; evento y permisos
- Estimación: M (≤ 1 d)
- Riesgos: email no sustituye evidencia en portal; estado se consulta desde sesión autenticada.

### QUO-003 — Aceptación o rechazo por el cliente con evidencia
- Fase / épica / módulo owner: F3 / QUO / request
- Tipo: feat
- Depende de: QUO-002, REQ-009, CUS-004
- Descripción: Cubre: RF-REQ-06; permite al titular aceptar/rechazar presupuesto vigente y registra evidencia de decisión.
- Ficheros/paquetes a crear o modificar (aproximado): `request.application.QuoteDecisionService`, endpoints cliente, evidencia/version snapshot y evento.
- Impacto modular: request actualiza quote/request; consume `customer.api` para owner; publica IDs para notificación. Sin ciclos.
- UI: detalle cliente muestra importe/moneda/IVA/validez y botones con confirmación accesible.
- Seguridad: owner+email verificado, CSRF, revalidar versión/validez y evitar doble decisión/replay.
- Criterios de aceptación (Given/When/Then):
  - Given titular y versión SENT vigente When acepta o rechaza Then se guarda decisión, actor, fecha y versión una sola vez.
  - Given cliente ajeno, presupuesto expirado o decisión repetida When envía POST Then 403/409 y no altera evidencia.
- Tests: TC-QUO-003: WT (decisión/CSRF), IT (concurrencia/evidencia), SEC (IDOR/replay).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Presupuestos»
- Estimación: M (≤ 1 d)
- Riesgos: NIF/CIF/NIE obligatorio al aceptar; formatos se validan en CUS-004 sin consulta externa.

### QUO-004 — Caducidad de presupuestos (job)
- Fase / épica / módulo owner: F3 / QUO / request
- Tipo: feat
- Depende de: QUO-002
- Descripción: Cubre: RF-EMP-03; job idempotente expira presupuestos SENT cuya fecha de validez pasó.
- Ficheros/paquetes a crear o modificar (aproximado): `request.application.QuoteExpiryJob`, batch query, métrica y configuración.
- Impacto modular: actualiza solo tablas request y publica evento de estado con IDs; notification consume. Sin ciclo.
- UI: empleado/cliente ven EXPIRED y el historial de presupuesto queda consultable.
- Seguridad: job usa UTC, lotes limitados y actor SYSTEM auditable; no acepta fecha enviada por cliente.
- Criterios de aceptación (Given/When/Then):
  - Given presupuesto SENT vencido When job corre Then cambia a EXPIRED una vez y publica evento.
  - Given presupuesto no vencido o ya decidido When job corre Then no modifica versión ni emite notificación duplicada.
- Tests: TC-QUO-004: UT (corte UTC), IT (job/idempotencia), MT (evento).
- Documentación a actualizar: DIARIO + INVENTARIO; operación del job
- Estimación: S (≤ 0,5 d)
- Riesgos: expiración y fecha local difieren; almacenar UTC y mostrar locale.

### EMP-001 — Panel de empleado por fragmentos
- Fase / épica / módulo owner: F3 / EMP / request
- Tipo: feat
- Depende de: REQ-001, IAM-012
- Descripción: Cubre: RF-EMP-01; resume nuevas, sin asignar, por estado, asignadas a mí, vencidas por SLA y no leídas.
- Ficheros/paquetes a crear o modificar (aproximado): `request.web.EmployeeDashboardController`, query projections, fragmentos `/gestion/inicio`.
- Impacto modular: consulta `request` y `messaging.api`; composición HTMX por URL y permisos; no une repos externos. Sin ciclos.
- UI: `/gestion`, tarjetas-resumen densas, filtros por owner y estado, responsive/teclado.
- Seguridad: `REQUESTS_VIEW_ALL`; conteos limitados a permisos, sin nombres/cuerpos no autorizados.
- Criterios de aceptación (Given/When/Then):
  - Given EMPLOYEE autenticado When abre panel Then ve métricas asignadas y sin asignar con enlaces filtrados.
  - Given CLIENT o sesión ausente When solicita `/gestion` Then recibe 403/redirect login y no accede a conteos.
- Tests: TC-EMP-001: WT (panel/permiso), IT (proyecciones), SEC (filtrado rol), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; README «Gestión»
- Estimación: M (≤ 1 d)
- Riesgos: conteos no deben cargar mensajes; usar proyecciones e índices.

### EMP-002 — Listado de solicitudes: filtros, búsqueda, orden, paginación y CSV
- Fase / épica / módulo owner: F3 / EMP / request
- Tipo: feat
- Depende de: REQ-001, IAM-012, UIK-008
- Descripción: Cubre: RF-EMP-02; consulta todas las solicitudes por tipo, categoría, estado, asignación, cliente, fechas e importe.
- Ficheros/paquetes a crear o modificar (aproximado): `request.web.EmployeeRequestController`, query service, templates/table y export CSV.
- Impacto modular: request consulta su esquema y APIs customer/staff; sin joins JPA cruzados. Ciclo: request→customer/staff permitido.
- UI: `/gestion/solicitudes`; búsqueda/filtros servidor, paginación estable y export compatible con teclado.
- Seguridad: `REQUESTS_VIEW_ALL`; CSV autorizado, paginado/limitado, neutralizar fórmula y enmascarar PII.
- Criterios de aceptación (Given/When/Then):
  - Given EMPLOYEE aplica tipo/estado/asignado When pagina Then la consulta respeta filtros y orden estable en SQL Server.
  - Given CLIENT o filtro malicioso/CSV formula When solicita export Then 403 o dato neutralizado; nunca descarga cuenta ajena.
- Tests: TC-EMP-002: WT (filtros/paginación), IT (índices/CSV), SEC (permisos/inyección), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; índices y rutas
- Estimación: M (≤ 1 d)
- Riesgos: CSV grande bloquea memoria; streaming con límite y auditoría.

### EMP-003 — Detalle de solicitud (empleado) con línea de tiempo auditada
- Fase / épica / módulo owner: F3 / EMP / request
- Tipo: feat
- Depende de: EMP-002, AUD-002
- Descripción: Cubre: RF-EMP-03; presenta solicitud, cliente, vínculos, adjuntos autorizados y timeline de acciones.
- Ficheros/paquetes a crear o modificar (aproximado): `request.web.EmployeeRequestDetailController`, queries, timeline fragments y bundles.
- Impacto modular: request consume customer/staff/files/messaging APIs; auditoría por `audit.api`; cada módulo conserva tablas. Sin ciclo.
- UI: `/gestion/solicitudes/{publicId}`; pestañas/resumen, estado, asignación y tema responsive.
- Seguridad: `REQUESTS_VIEW_ALL`/`AUDIT_VIEW`; comprobar autorización por cada panel, no solo route principal.
- Criterios de aceptación (Given/When/Then):
  - Given EMPLOYEE autorizado When abre solicitud Then ve timeline y adjuntos limpios que tiene permiso para consultar.
  - Given recurso inexistente o actor sin permiso When accede directo Then recibe 403/404 y paneles HTMX no filtran datos.
- Tests: TC-EMP-003: WT (detalle/paneles), SEC (IDOR en cada URL), A11Y (tabs/foco).
- Documentación a actualizar: DIARIO + INVENTARIO; rutas/contratos UI
- Estimación: M (≤ 1 d)
- Riesgos: detalle agregado crea llamadas N+1; usar proyecciones por panel.

### EMP-004 — Asignaciones 1..N (responsable y colaboradores) con notificación
- Fase / épica / módulo owner: F3 / EMP / request
- Tipo: feat
- Depende de: EMP-003, STF-002
- Descripción: Cubre: RF-EMP-05; asigna responsable obligatorio y cero o más colaboradores con auditoría y aviso.
- Ficheros/paquetes a crear o modificar (aproximado): `request.domain.RequestAssignment`, tabla `request.assignment`, servicio y evento.
- Impacto modular: tabla assignment owner request; valida staff vía `staff.api`; evento contiene IDs para notification. Sin ciclo.
- UI: selector de trabajador/especialidad en detalle; estado de asignación y responsable resaltado.
- Seguridad: `REQUESTS_ASSIGN`; solo staff activo; impedir duplicados y self-approval no autorizado.
- Criterios de aceptación (Given/When/Then):
  - Given empleado asignador autorizado When selecciona responsable y colaboradores activos Then se guarda 1..N y sale evento de notificación.
  - Given staff inactivo, ID ajeno o usuario sin `REQUESTS_ASSIGN` When intenta asignar Then recibe 403/422 sin cambios.
- Tests: TC-EMP-004: UT (invariantes), MT (evento), IT (concurrencia), SEC (permiso/IDs).
- Documentación a actualizar: DIARIO + INVENTARIO; esquema de asignación
- Estimación: M (≤ 1 d)
- Riesgos: desactivar staff asignado exige reasignación explícita, no borrado en cascada.

### EMP-005 — Cambio de estado, prioridad y estimación interna
- Fase / épica / módulo owner: F3 / EMP / request
- Tipo: feat
- Depende de: EMP-003, REQ-003, REQ-004
- Descripción: Cubre: RF-EMP-03, RF-EMP-04; permite transición válida, prioridad y estimación en horas con auditoría.
- Ficheros/paquetes a crear o modificar (aproximado): `request.application.EmployeeRequestActions`, prioridad/estimación y command handlers.
- Impacto modular: actualiza request y consume sus state machines; publica `RequestStatusChanged` con IDs. Sin tablas de otros módulos/ciclos.
- UI: acciones según rol/estado en `/gestion/solicitudes/{id}`, ayuda i18n y confirmación accesible.
- Seguridad: `REQUESTS_VIEW_ALL`; transición según máquina, estimación no negativa y prioridad validada; CSRF/auditoría.
- Criterios de aceptación (Given/When/Then):
  - Given EMPLOYEE asignado y transición permitida When cambia estado/prioridad Then persiste historial y emite evento una vez.
  - Given transición no válida, estimación negativa o usuario no autorizado When envía comando Then rechaza sin modificar solicitud.
- Tests: TC-EMP-005: UT (transiciones/estimación), WT (permiso/CSRF), MT (evento/auditoría), SEC.
- Documentación a actualizar: DIARIO + INVENTARIO; tabla de estados 03/07
- Estimación: M (≤ 1 d)
- Riesgos: edición simultánea puede sobrescribir estado; usar optimistic locking/versión.

### EMP-006 — Conversación con el cliente y notas internas en la solicitud
- Fase / épica / módulo owner: F3 / EMP / request
- Tipo: feat
- Depende de: EMP-003, MSG-002
- Descripción: Cubre: RF-EMP-03; integra hilo público con el cliente y notas INTERNAL visibles solo a staff autorizado.
- Ficheros/paquetes a crear o modificar (aproximado): `request.web.RequestConversationPanel`, `messaging.api` adapter/fragment, claves i18n.
- Impacto modular: request consume `messaging.api`; mensajería no depende de request; owner de mensajes `messaging`. Sin ciclo.
- UI: panel por URL HTMX en detalle; composer público e interno claramente diferenciados y teclado accesible.
- Seguridad: `REQUESTS_VIEW_ALL`/permiso de notas; query INTERNAL solo para staff; nunca en email público ni DTO CLIENT.
- Criterios de aceptación (Given/When/Then):
  - Given EMPLOYEE autorizado When añade nota interna Then solo aparece en vista interna y queda auditada.
  - Given CLIENT dueño When carga/consulta hilo por endpoint directo Then ve mensajes PUBLIC y nunca el cuerpo INTERNAL.
- Tests: TC-EMP-006: WT (panel), SEC (notas en HTML/JSON/email), MT (messaging API), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; visibilidad en 07
- Estimación: M (≤ 1 d)
- Riesgos: flag visual no basta; filtrar en API/query y probar todos los formatos de respuesta.

### EMP-007 — Carga de trabajo por trabajador y SLA de primera respuesta
- Fase / épica / módulo owner: F3 / EMP / request
- Tipo: feat
- Depende de: EMP-004
- Descripción: Cubre: RF-EMP-05; calcula asignaciones abiertas/carga y vencimiento de primera respuesta con SLA configurable.
- Ficheros/paquetes a crear o modificar (aproximado): query projections, `request.application.SlaPolicy`, configuración de horario/feriados.
- Impacto modular: consulta request/assignment propios y staff.api; sin nuevas tablas salvo config owner request. Sin ciclos.
- UI: `/gestion/carga`, agrupación por trabajador, vencidas y filtro; resumen sin exponer cliente a roles no autorizados.
- Seguridad: `REQUESTS_VIEW_ALL`; datos agregados mínimos; zona de cálculo y cambio de SLA auditados.
- Criterios de aceptación (Given/When/Then):
  - Given primera respuesta pendiente When panel calcula Then muestra vencimiento según SLA inicial propuesto de 1 día hábil y configuración activa.
  - Given solicitud ya respondida When se recalcula Then deja de figurar vencida y la consulta respeta permisos de trabajador.
- Tests: TC-EMP-007: UT (calendario/SLA), IT (proyecciones), WT (filtro/permiso).
- Documentación a actualizar: DIARIO + INVENTARIO; 13 confirmación de SLA
- Estimación: M (≤ 1 d)
- Riesgos: calendario hábil/feriados no confirmado; mantener SLA configurable y validar opción por defecto.

### E2E-002 — E2E: asignación y presupuesto
- Fase / épica / módulo owner: F3 / E2E / test
- Tipo: test
- Depende de: EMP-004, QUO-003, UIK-009
- Descripción: Cubre: RNF-TST-03; automatiza crear solicitud, asignar empleado, emitir presupuesto y aceptar/rechazar como titular.
- Ficheros/paquetes a crear o modificar (aproximado): `e2e/request-assignment-quote.spec.ts`, fixtures DB/Mailpit y Playwright.
- Impacto modular: prueba HTTP/navegador de request/staff/notification; no agrega API/tabla productiva. Ciclo: ninguno.
- UI: desktop/mobile, ES/EN, cliente y empleado; confirma estado visible y presupuesto versionado.
- Seguridad: identidades/datos efímeros; valida separación entre cliente propietario y empleado asignado.
- Criterios de aceptación (Given/When/Then):
  - Given cliente verificado con solicitud When empleado asigna y envía presupuesto Then cliente acepta y el estado/evidencia se reflejan.
  - Given cliente no propietario o presupuesto expirado When intenta decisión Then servidor deniega sin filtrar ni alterar otra solicitud.
- Tests: TC-E2E-002: E2E (Playwright), SEC (owner/expiración), A11Y (formularios).
- Documentación a actualizar: DIARIO + INVENTARIO; README «E2E»
- Estimación: M (≤ 1 d)
- Riesgos: test dependiente de horarios; controlar clock y expirar por fixture.
