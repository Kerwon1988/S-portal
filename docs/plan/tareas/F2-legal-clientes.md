# F2 — Legal y clientes (consentimientos, derechos y ClientAccount): tareas

- Objetivo: registrar evidencia legal, ejecutar derechos de datos y permitir alta/invitación de clientes (hito M2; ver [roadmap](../10-roadmap-backlog.md)).
- Módulo(s) owner: `legal` y `customer`; `identity` presta autenticación/invitaciones.

### LGL-001 — legal: registro de consentimientos con evidencia
- Fase / épica / módulo owner: F2 / LGL / legal
- Tipo: feat
- Depende de: FND-006, CMS-010, AUD-001
- Descripción: Cubre: RF-IAM-02, RNF-PRV-01; registra consentimiento explícito con versión legal, fecha, origen y contexto mínimo.
- Ficheros/paquetes a crear o modificar (aproximado): `legal.domain.ConsentRecord`, `legal.consent_record`, API de aceptación y migración.
- Impacto modular: tabla `legal.consent_record` owner legal; consume versiones publicadas por `content.api`; `audit.api` registra cambios. Sin ciclo.
- UI: checkboxes separados para términos/privacidad y marketing; no premarcados; ES/EN.
- Seguridad: guardar IP solo cifrada si es necesaria; datos mínimos, CSRF y actor/fecha no falsificables por cliente.
- Criterios de aceptación (Given/When/Then):
  - Given aceptación de versión vigente When se confirma Then persiste tipo, versión, hora UTC y origen ligado al usuario.
  - Given consentimiento comercial omitido When se registra cuenta Then queda false y no bloquea la aceptación legal obligatoria.
- Tests: TC-LGL-001: UT (evidencia/versiones), IT (índices/migración), SEC (no premarcado/manipulación).
- Documentación a actualizar: DIARIO + INVENTARIO; tabla de consentimientos
- Estimación: M (≤ 1 d)
- Riesgos: cambiar texto invalida evidencia previa; versionar, no sobrescribir.

### LGL-002 — Derechos RGPD: exportación y anonimización orquestadas
- Fase / épica / módulo owner: F2 / LGL / legal
- Tipo: feat
- Depende de: LGL-001, IAM-004
- Descripción: Cubre: RF-IAM-08, RNF-PRV-03; orquesta exportación y baja mediante `shared.spi.PersonalDataContributor` implementado por módulos.
- Ficheros/paquetes a crear o modificar (aproximado): `legal.application.DataSubjectRequestService`, `legal.data_subject_request`, export seguro y adapters SPI.
- Impacto modular: legal invoca SPI sin depender de módulos con PII; cada owner exporta/anonimiza sus tablas. Sin ciclo.
- UI: `/cuenta/datos`, solicitud y estado con mensajes ES/EN; confirmación antes de anonimizar.
- Seguridad: validar identidad/propiedad, descarga export cifrada y caducada; conservar solicitudes/mensajes seudonimizados según política.
- Criterios de aceptación (Given/When/Then):
  - Given titular autenticado When solicita exportación Then recibe paquete solo de sus datos mediante contributors registrados.
  - Given usuario distinto o solicitud incompleta When intenta baja Then no anonimiza cuenta ajena y registra motivo mínimo.
- Tests: TC-LGL-002: UT (orquestación SPI), MT (todos contributors), SEC (IDOR/export), IT (anonimización).
- Documentación a actualizar: DIARIO + INVENTARIO; 07 privacidad
- Estimación: M (≤ 1 d)
- Riesgos: contributor omitido deja PII; test exige cobertura de todos los owners.

### LGL-003 — Registro de retención y job de purga genérico
- Fase / épica / módulo owner: F2 / LGL / legal
- Tipo: feat
- Depende de: LGL-001
- Descripción: Cubre: RNF-PRV-02; registra categorías/plazos propuestos y ejecuta purga/anonimización idempotente por owner.
- Ficheros/paquetes a crear o modificar (aproximado): `legal.application.RetentionJob`, tabla `legal.retention_policy`, configuración por entorno.
- Impacto modular: legal gestiona política; módulos dueños borran sus datos vía SPI/puerto. Sin acceso a repos ajenos, sin ciclos.
- UI: n/a (job; política visible en documentación operativa).
- Seguridad: respetar retención legal/hold, dry-run, lote limitado y auditoría sin copiar contenido purgado.
- Criterios de aceptación (Given/When/Then):
  - Given dato vencido bajo política activa When corre job Then anonimiza/purga una vez y guarda conteo agregado.
  - Given dato con retención legal o fallo parcial When corre job Then lo preserva, informa error y puede reintentarse idempotente.
- Tests: TC-LGL-003: UT (plazos/hold), IT (purga y retry), SEC (auditoría sin PII).
- Documentación a actualizar: DIARIO + INVENTARIO; tabla retención en 07
- Estimación: M (≤ 1 d)
- Riesgos: plazos son propuestas del brief; validar con responsable legal antes de producción.

### CUS-001 — customer: ClientAccount y miembro OWNER con migraciones
- Fase / épica / módulo owner: F2 / CUS / customer
- Tipo: feat
- Depende de: IAM-001
- Descripción: Cubre: RF-IAM-02, RF-SIM-06; modela cuenta empresa/autónomo/particular y un OWNER en MVP, preparada para miembros futuros.
- Ficheros/paquetes a crear o modificar (aproximado): `customer.{api,domain,application,infrastructure}`, `customer.client_account`, `account_member`.
- Impacto modular: tablas customer owner; referencia `identity` por IDs/API sin escribir sus tablas; publica `customer.api`. Ciclo: customer→identity permitido.
- UI: n/a (dominio; alta en CUS-002).
- Seguridad: unicidad y estado explícitos; validar que solo hay un OWNER MVP y no usar email como vínculo a DEMO.
- Criterios de aceptación (Given/When/Then):
  - Given cuenta nueva When se crea Then tiene exactamente un miembro OWNER ligado a identity.user ID.
  - Given segundo OWNER incompatible con política MVP When se persiste Then falla sin corromper membresías ni crear vínculo externo por coincidencia de email.
- Tests: TC-CUS-001: UT (invariantes), IT (migración/relación), MT (API publicada).
- Documentación a actualizar: DIARIO + INVENTARIO; modelo ER
- Estimación: M (≤ 1 d)
- Riesgos: duplicar identidad entre módulos; customer referencia IDs y deja autenticación a identity.

### CUS-002 — Registro de cliente (formulario, consentimientos, anti-bot)
- Fase / épica / módulo owner: F2 / CUS / customer
- Tipo: feat
- Depende de: CUS-001, IAM-002, IAM-007, IAM-006, LGL-001, UIK-008
- Descripción: Cubre: RF-IAM-02, RF-IAM-03; registra persona, cuenta, idioma, teléfono E.164 opcional, aceptación y consentimiento comercial separado.
- Ficheros/paquetes a crear o modificar (aproximado): `customer.web.RegistrationController`, application service, templates `/registro`, bundles ES/EN.
- Impacto modular: `customer` consume `identity.api`, `legal.api` y config anti-bot; publica `ClientRegistered` con IDs. Sin ciclo.
- UI: `/registro`, tipo de cuenta y datos condicionales; errores asociados, móvil y fallback anti-bot.
- Seguridad: CSRF, ALTCHA/honeypot, rate limit, NIF opcional, Argon2id y email no enumerado.
- Criterios de aceptación (Given/When/Then):
  - Given datos válidos y términos aceptados When confirma Then crea cuenta OWNER, consentimiento versionado y envía verificación.
  - Given bot/CSRF ausente o datos inválidos When envía Then no crea usuario/cuenta y muestra error accesible sin enumeración.
- Tests: TC-CUS-002: WT (form/CSRF), IT (transacción/outbox), SEC (anti-bot/consentimiento), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; README «Registro»
- Estimación: M (≤ 1 d)
- Riesgos: datos de negocio difieren por país; NIF permanece opcional hasta validar país.

### CUS-003 — Alta de cliente por ADMIN mediante invitación
- Fase / épica / módulo owner: F2 / CUS / customer
- Tipo: feat
- Depende de: CUS-001, IAM-010, IAM-012
- Descripción: Cubre: RF-IAM-10; permite a ADMIN crear cuenta/invitación y asociar cliente tras aceptación, con misma política que autoservicio.
- Ficheros/paquetes a crear o modificar (aproximado): `customer.web.AdminInvitationController`, `customer.application`, templates `/gestion/clientes/invitar`.
- Impacto modular: customer usa `identity.api` invitación y `legal.api` consentimiento; tablas customer owner. Ciclo: customer→identity/legal permitido.
- UI: `/gestion/clientes/nuevo`; confirmación de correo/tipo y estado de invitación.
- Seguridad: `CLIENTS_MANAGE`/permiso acordado, CSRF, auditoría; no fijar contraseña; validar último ADMIN en IAM.
- Criterios de aceptación (Given/When/Then):
  - Given ADMIN autorizado When invita cliente Then genera cuenta/invitación de un uso y no activa acceso antes de aceptación.
  - Given EMPLOYEE sin permiso When intenta invitar por POST Then recibe 403 y no crea invitación.
- Tests: TC-CUS-003: WT (permiso/CSRF), MT (evento aceptación), SEC (escalado de rol).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Gestión de clientes»
- Estimación: M (≤ 1 d)
- Riesgos: permisos deben coincidir con matriz de IAM-012; no inferirlos del menú.

### CUS-004 — Validación de NIF/CIF/NIE y país
- Fase / épica / módulo owner: F2 / CUS / customer
- Tipo: feat
- Depende de: CUS-001
- Descripción: Cubre: RF-IAM-02; valida formato/localización de identificador fiscal según país, sin afirmar comprobación ante autoridad externa.
- Ficheros/paquetes a crear o modificar (aproximado): `customer.domain.TaxIdentifier`, catálogo de países y pruebas de normalización.
- Impacto modular: value object interno customer; datos cifrados en esquema customer; sin APIs/eventos nuevos. Ciclo: ninguno.
- UI: campo opcional al registro, requerido antes de aceptar presupuesto según país; mensajes ES/EN.
- Seguridad: cifrado por columna, enmascarado en logs y vistas; no transmitir a terceros.
- Criterios de aceptación (Given/When/Then):
  - Given NIF/CIF/NIE válido para país When se normaliza Then se conserva cifrado y se muestra enmascarado.
  - Given formato inválido o país no soportado When se valida Then se rechaza con error localizado sin registrar valor.
- Tests: TC-CUS-004: UT (formatos/país), SEC (cifrado/enmascarado), WT (requerido condicional).
- Documentación a actualizar: DIARIO + INVENTARIO; pregunta de países en 13
- Estimación: S (≤ 0,5 d)
- Riesgos: formato no equivale a existencia legal; no integrar validación externa sin decisión.

