# F2 — Staff y gestión de usuarios (trabajadores, listados, fichas y acciones): tareas

- Objetivo: habilitar alta y gestión segura de trabajadores/clientes por ADMIN y verificar el recorrido principal (hito M2; ver [roadmap](../10-roadmap-backlog.md)).
- Módulo(s) owner: `staff`, `customer`, `identity` y `legal`; UI compuesta por fragmentos de cada owner.

### STF-001 — staff: perfil de trabajador y alta por invitación
- Fase / épica / módulo owner: F2 / STF / staff
- Tipo: feat
- Depende de: IAM-010, IAM-012
- Descripción: Cubre: RF-IAM-09; modela perfil, puesto, rol, especialidades, fecha y estado, activado al aceptar invitación.
- Ficheros/paquetes a crear o modificar (aproximado): `staff.{api,domain,application,infrastructure}`, `staff.staff_profile`, migración y listener.
- Impacto modular: tabla `staff.staff_profile` owner staff; consume `identity.api.InvitationAccepted`; identity no accede a staff. Sin ciclo.
- UI: datos de perfil en `/gestion/trabajadores`; alta inicial vía IAM-010.
- Seguridad: `STAFF_MANAGE`; solo ADMIN invita/cambia rol; no almacenar contraseña ni duplicar email.
- Criterios de aceptación (Given/When/Then):
  - Given invitación de trabajador aceptada When se publica evento Then crea perfil con estado correcto y especialidades vacías.
  - Given invitación pendiente/expirada When se intenta crear perfil Then no activa cuenta ni asigna rol de empleado.
- Tests: TC-STF-001: UT (invariantes), MT (evento aceptación), IT (migración/relación), SEC.
- Documentación a actualizar: DIARIO + INVENTARIO; tabla staff
- Estimación: M (≤ 1 d)
- Riesgos: rol identity y perfil staff desincronizados; transicionar por evento idempotente.

### STF-002 — Gestión de trabajadores (edición, estado, especialidades)
- Fase / épica / módulo owner: F2 / STF / staff
- Tipo: feat
- Depende de: STF-001, UIK-008
- Descripción: Cubre: RF-IAM-09, RF-USR-03; permite editar perfil, estado y especialidades desde backoffice.
- Ficheros/paquetes a crear o modificar (aproximado): `staff.web`, `staff.application.StaffManagementService`, `templates/staff/`, bundles.
- Impacto modular: `staff` actualiza sus tablas y `identity.api` para cambios de cuenta; emite ID de cambio auditado. Sin ciclos.
- UI: `/gestion/trabajadores/{publicId}`; formularios/tablas, tema, móvil y mensajes ES/EN.
- Seguridad: `STAFF_MANAGE`, CSRF, audit; impedir cambiar autorización fuera de matriz IAM-012.
- Criterios de aceptación (Given/When/Then):
  - Given ADMIN autorizado When modifica especialidades/estado Then se guarda y audita antes/después.
  - Given EMPLOYEE sin permiso When envía POST de activación When la acción no está autorizada Then recibe 403 y no cambia datos.
- Tests: TC-STF-002: WT (edición/403/CSRF), SEC (escalado), A11Y (formulario).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Trabajadores»
- Estimación: M (≤ 1 d)
- Riesgos: desactivar responsable asignado requiere política de reasignación; registrar caso en request.

### USR-001 — Listados de clientes y trabajadores (filtros, orden, paginación, CSV)
- Fase / épica / módulo owner: F2 / USR / customer
- Tipo: feat
- Depende de: CUS-001, STF-001, UIK-008
- Descripción: Cubre: RF-USR-01; crea consultas servidoras separadas para clientes y trabajadores con filtros y exportación limitada.
- Ficheros/paquetes a crear o modificar (aproximado): `customer.application.UserDirectory`, `customer.web`, `staff.api`, templates/list bundles.
- Impacto modular: consume `customer.api`/`staff.api`/`identity.api`; cada módulo consulta su tabla owner, sin joins/repos compartidos. Sin ciclos.
- UI: `/gestion/clientes` y `/gestion/trabajadores`; búsqueda, estado, rol, fechas, locale y paginación.
- Seguridad: `USERS_MANAGE`; export CSV solo ADMIN con `CLIENTS_EXPORT`; neutralizar fórmulas CSV.
- Criterios de aceptación (Given/When/Then):
  - Given ADMIN aplica filtro y orden When lista resultados Then SQL pagina, total es consistente y CSV refleja filtro.
  - Given CLIENT o EMPLOYEE sin permiso When solicita listado/export CSV Then recibe 403 y no accede a PII.
- Tests: TC-USR-001: WT (filtros/paginación/403), IT (consulta/index), SEC (CSV injection/PII).
- Documentación a actualizar: DIARIO + INVENTARIO; índices y rutas
- Estimación: M (≤ 1 d)
- Riesgos: filtro global puede degradar SQL; limitar campos y tamaño de página.

### USR-002 — Fichas de cliente y trabajador con paneles por fragmentos
- Fase / épica / módulo owner: F2 / USR / customer
- Tipo: feat
- Depende de: USR-001, AUD-002
- Descripción: Cubre: RF-USR-02; presenta ficha agregada y paneles por URL del módulo owner sin unir repositorios.
- Ficheros/paquetes a crear o modificar (aproximado): `customer.web.UserDetailController`, templates de ficha y contratos UI fragment por módulo.
- Impacto modular: paneles request/messaging/simbiosphere/audit se cargan por HTMX URL con permiso propio; no hay dependencia Java. Sin ciclos.
- UI: `/gestion/usuarios/{publicId}`; datos, estado, cuenta y paneles lazy con fallback si módulo no disponible.
- Seguridad: `USERS_MANAGE`; verificar publicId y autorización en cada panel; ocultar notas internas al cliente.
- Criterios de aceptación (Given/When/Then):
  - Given ADMIN con permiso When abre ficha Then paneles autorizados cargan fragmentos y muestran estados de owner.
  - Given ID de usuario ajeno sin permiso When accede directo a ficha/panel Then recibe 403/404 sin revelar existencia.
- Tests: TC-USR-002: WT (ficha/fragmentos), SEC (IDOR por panel), A11Y (foco/HTMX).
- Documentación a actualizar: DIARIO + INVENTARIO; URLs de panel en inventario
- Estimación: M (≤ 1 d)
- Riesgos: fragmento sin gate propio; cada endpoint UI valida permiso aunque enlace se oculte.

### USR-003 — Acciones administrativas con reglas, confirmación y auditoría
- Fase / épica / módulo owner: F2 / USR / identity
- Tipo: feat
- Depende de: USR-002, IAM-008, IAM-009, IAM-010
- Descripción: Cubre: RF-USR-03; habilita activar/desactivar/desbloquear, reenviar invitación/reset y cambios controlados de rol.
- Ficheros/paquetes a crear o modificar (aproximado): `identity.application.AdminUserActions`, controllers/fragments, `AuditRecorder` y tests.
- Impacto modular: orquesta `identity.api`, `staff.api`, `customer.api`; evento/auditoría con IDs, sin PII. Sin ciclos Java inversos.
- UI: acciones con confirmación, resultado accesible y registro de antes/después en ficha.
- Seguridad: `USERS_MANAGE`; impedir quitar último ADMIN o auto-degradación; contraseñas nunca se envían, links sí.
- Criterios de aceptación (Given/When/Then):
  - Given ADMIN cambia estado permitido When confirma Then la acción queda auditada y notifica al usuario afectado.
  - Given intento de degradar último ADMIN o a sí mismo When confirma Then se rechaza transaccionalmente sin efecto.
- Tests: TC-USR-003: UT (reglas), WT (confirmación/CSRF), SEC (último ADMIN/escalado), MT (auditoría).
- Documentación a actualizar: DIARIO + INVENTARIO; matriz 07
- Estimación: M (≤ 1 d)
- Riesgos: panel SimbioSphere no está disponible hasta F5; no simular reintento temprano.

### USR-004 — Baja RGPD por anonimización (autoservicio y backoffice)
- Fase / épica / módulo owner: F2 / USR / legal
- Tipo: feat
- Depende de: LGL-002, USR-002
- Descripción: Cubre: RF-USR-03, RNF-PRV-03; enlaza baja confirmada a la orquestación de anonimización por owners.
- Ficheros/paquetes a crear o modificar (aproximado): `legal.application.DataSubjectRequestService`, acciones de ficha y `/cuenta/datos`.
- Impacto modular: legal coordina `PersonalDataContributor`; cada módulo anonimizador es owner de tablas. Sin acceso cruzado ni ciclos.
- UI: usuario inicia baja; ADMIN atiende solicitudes con confirmación, estado y motivo.
- Seguridad: identidad reautenticada, CSRF, audit; conservar solicitud/mensaje solo seudonimizado según retención.
- Criterios de aceptación (Given/When/Then):
  - Given titular confirma baja When todos los contributors terminan Then PII se anonimiza y queda recibo sin dato personal.
  - Given contributor falla When proceso parcial ocurre Then solicitud queda pendiente/reintentable y no declara baja completada.
- Tests: TC-USR-004: IT (contributors/persistencia), SEC (owner/confirmación), UT (reintento idempotente).
- Documentación a actualizar: DIARIO + INVENTARIO; 07 retención/derechos
- Estimación: M (≤ 1 d)
- Riesgos: no olvidar datos PII en adjuntos/logs; cada owner aporta prueba de borrado.

### E2E-001 — E2E: invitación de trabajador y edición en el CMS
- Fase / épica / módulo owner: F2 / E2E / test
- Tipo: test
- Depende de: STF-001, CMS-006, UIK-009, IAM-009, IAM-012
- Descripción: Cubre: RNF-TST-03; automatiza ADMIN con MFA invita trabajador, acepta enlace y realiza edición permitida del CMS.
- Ficheros/paquetes a crear o modificar (aproximado): `e2e/invitation-cms.spec.ts`, fixtures de email, datos efímeros y setup TOTP.
- Impacto modular: prueba sistema por navegador/HTTP; sin contratos o tablas de producción adicionales. Ciclo: ninguno.
- UI: desktop/mobile, ES/EN; valida errores, foco y navegación autenticada.
- Seguridad: secretos TOTP efímeros por entorno, no guardar screenshots con credenciales/tokens.
- Criterios de aceptación (Given/When/Then):
  - Given ADMIN con MFA habilitado When invita y trabajador acepta Then token se consume, perfil aparece y sesión tiene permisos esperados.
  - Given invitación vencida o trabajador intenta editar sin CONTENT_EDIT When navega Then se deniega sin cambiar contenido.
- Tests: TC-E2E-001: E2E (Playwright), A11Y (flujo), SEC (caducidad/permiso).
- Documentación a actualizar: DIARIO + INVENTARIO; README «E2E»
- Estimación: M (≤ 1 d)
- Riesgos: depender de email externo vuelve flaky; usar Mailpit y reloj controlable.
