# F4 — Contacto y bandeja (conversación pública, respuesta y notificaciones): tareas

- Objetivo: unificar contacto y conversaciones de solicitudes en bandeja interna con respuesta y trazabilidad (hito M4; ver [roadmap](../10-roadmap-backlog.md)).
- Módulo(s) owner: `messaging`; `request` convierte contacto; `notification` envía avisos.

### MSG-003 — Formulario público de contacto con consentimiento, anti-abuso y acuse
- Fase / épica / módulo owner: F4 / MSG / messaging
- Tipo: feat
- Depende de: MSG-002, IAM-005, IAM-006, LGL-001, NTF-003, CUS-001
- Descripción: Cubre: RF-MSG-01, RF-MSG-04; recibe nombre/email/teléfono/motivo/mensaje, consentimiento y crea conversación CONTACT, no solicitud.
- Ficheros/paquetes a crear o modificar (aproximado): `messaging.web.ContactController`, `messaging.application`, `templates/messaging/contact.html`, bundles.
- Impacto modular: messaging usa `customer.api`, `notification.api`, SPIs `shared`; evidencia de contacto en tabla messaging; sin dependencia Java legal. Ciclo: ninguno.
- UI: `/{lang}/contacto`; datos precargados si autenticado; wa/email y audio son tareas aparte.
- Seguridad: CSRF, honeypot/ALTCHA, rate limit IP/email, límites y URL no descargada; consent/version desde configuración.
- Criterios de aceptación (Given/When/Then):
  - Given anónimo completa formulario y consentimiento When envía Then crea conversación CONTACT y acuse por outbox sin solicitud.
  - Given honeypot, rate excedido o consentimiento ausente When envía Then deniega con respuesta genérica y sin persistir contenido.
- Tests: TC-MSG-003: WT (form/CSRF/consent), IT (conversation/outbox), SEC (abuso/no SSRF), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; política de contacto
- Estimación: M (≤ 1 d)
- Riesgos: retención de contacto propuesta 12 meses; confirmar en 13/07.

### MSG-004 — Adjuntos del contacto con límites
- Fase / épica / módulo owner: F4 / MSG / messaging
- Tipo: feat
- Depende de: MSG-003, FIL-003
- Descripción: Cubre: RF-MSG-01; asocia adjuntos limpios y limitados al hilo CONTACT.
- Ficheros/paquetes a crear o modificar (aproximado): `messaging.application.ContactAttachmentService`, relación `messaging.message_attachment`, UI y límites.
- Impacto modular: tabla relación owner messaging; bytes vía `files.api`; mensajería no almacena fichero. Sin ciclo.
- UI: input con tipos/tamaños permitidos, resumen y eliminación antes de enviar.
- Seguridad: Tika/magic bytes, ClamAV CLEAN, cuota; descargar solo empleado autorizado con `attachment/nosniff`.
- Criterios de aceptación (Given/When/Then):
  - Given adjunto permitido y limpio When se envía Then se vincula a mensaje y queda privado.
  - Given adjunto excesivo, infectado o MIME falso When se procesa Then se rechaza/cuarentena y no se hace descargable.
- Tests: TC-MSG-004: UT (límites), IT (files API), SEC (autorización/virus).
- Documentación a actualizar: DIARIO + INVENTARIO; retención de adjuntos
- Estimación: S (≤ 0,5 d)
- Riesgos: adjuntos de contacto son PII; aplicar propuesta de retención 12 meses.

### MSG-005 — Nota de audio (MediaRecorder, previsualización, fallback, Permissions-Policy)
- Fase / épica / módulo owner: F4 / MSG / messaging
- Tipo: feat
- Depende de: MSG-004
- Descripción: Cubre: RF-MSG-02; permite grabar audio hasta 2 minutos, previsualizar y enviar o elegir fichero alternativo.
- Ficheros/paquetes a crear o modificar (aproximado): JS `ContactRecorder`, fragmento de formulario, configuración `Permissions-Policy` y pruebas de codec.
- Impacto modular: audio se adjunta como fichero vía `files.api`; mensaje owner messaging. Sin nuevas dependencias/eventos.
- UI: MediaRecorder webm/opus, mp4/aac si soportado; HTTPS, estado de permiso y fallback accesible.
- Seguridad: límite estricto 120 s/tamaño; pedir permiso solo al pulsar grabar; antivirus y retención propuesta 6 meses.
- Criterios de aceptación (Given/When/Then):
  - Given navegador compatible con micrófono When usuario graba ≤120 s Then puede escuchar, descartar o enviar audio.
  - Given permiso denegado/codec no compatible When inicia grabación Then explica y permite subir audio permitido sin bloquear contacto.
- Tests: TC-MSG-005: E2E (record/preview/fallback), A11Y, IT (límite), SEC (permission policy).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Audio»
- Estimación: M (≤ 1 d)
- Riesgos: Safari/codec varía; detección de soporte sin asumir formato.

### MSG-006 — Botones de canal WhatsApp y mailto configurables
- Fase / épica / módulo owner: F4 / MSG / messaging
- Tipo: feat
- Depende de: UIK-006
- Descripción: Cubre: RF-MSG-03; genera enlaces externos `wa.me` y `mailto:` con número/texto de contacto configurables.
- Ficheros/paquetes a crear o modificar (aproximado): fragmento `templates/messaging/channel-links.html`, config y bundles ES/EN.
- Impacto modular: links de presentación en messaging/content; sin API, evento ni tabla. Ciclo: ninguno.
- UI: botones externos accesibles con indicación de nueva app; mantienen tema y tamaño táctil.
- Seguridad: normalizar E.164/URI, codificar texto y prohibir `javascript:`; no reportar estos mensajes como recibidos.
- Criterios de aceptación (Given/When/Then):
  - Given número configurado válido When se selecciona WhatsApp Then abre `https://wa.me/<numero>` con texto URL-encoded.
  - Given email ausente/valor inválido When se renderiza Then oculta o deshabilita enlace sin formar URI insegura.
- Tests: TC-MSG-006: UT (URI encoding/config), WT (links ES/EN), A11Y (nombre/contexto).
- Documentación a actualizar: DIARIO + INVENTARIO; 13 número WhatsApp
- Estimación: S (≤ 0,5 d)
- Riesgos: número y proveedor mail pendientes; mantener valores fuera de plantillas.

### MSG-007 — Bandeja: listado, filtros y búsqueda
- Fase / épica / módulo owner: F4 / MSG / messaging
- Tipo: feat
- Depende de: MSG-002, IAM-012, UIK-008
- Descripción: Cubre: RF-MSG-05; lista conversaciones con filtros por no leídas, asignación, categoría, estado y texto.
- Ficheros/paquetes a crear o modificar (aproximado): `messaging.web.InboxController`, query projections, templates y bundles.
- Impacto modular: consulta tablas messaging; owner messaging; destinatarios actuales por shared SPI. Sin dependencia request. Ciclo: ninguno.
- UI: `/gestion/bandeja`; listado paginado y búsqueda servidor, tema, ES/EN y atajos accesibles.
- Seguridad: `INBOX_MANAGE`; excluir papelera salvo filtro autorizado, sin búsqueda sobre PII no permitida.
- Criterios de aceptación (Given/When/Then):
  - Given EMPLOYEE con `INBOX_MANAGE` When filtra asignadas/no leídas Then recibe solo resultados permitidos con paginación estable.
  - Given CLIENT o usuario sin permiso When consulta listado o búsqueda directa Then recibe 403 sin resultados ni conteos.
- Tests: TC-MSG-007: WT (filtros/403), IT (búsqueda/paginación), SEC (privilegios), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; permisos y rutas
- Estimación: M (≤ 1 d)
- Riesgos: búsqueda por texto puede filtrar contenido; acotar campos/retención e índices.

### MSG-008 — Bandeja: hilo, respuesta con adjuntos y plantillas bilingües
- Fase / épica / módulo owner: F4 / MSG / messaging
- Tipo: feat
- Depende de: MSG-007, FIL-003, NTF-003
- Descripción: Cubre: RF-MSG-05; renderiza conversación cronológica y permite respuesta interna/saliente con adjuntos limpios y plantillas.
- Ficheros/paquetes a crear o modificar (aproximado): `messaging.web.ThreadController`, application commands, templates/email y bundles.
- Impacto modular: messaging publica `MessagePosted`; consume `files.api` y `notification.api`; owner de hilo messaging. Sin ciclos.
- UI: `/gestion/bandeja/{id}`; hilo cronológico, responder, adjuntar y plantillas ES/EN; foco al nuevo mensaje.
- Seguridad: `INBOX_MANAGE`, CSRF y visibilidad por audiencia; no usar destinatario arbitrario ni exponer notas a cliente.
- Criterios de aceptación (Given/When/Then):
  - Given empleado asignado When responde con adjunto limpio Then se guarda mensaje, se notifica al destinatario y aparece una vez.
  - Given nota interna o adjunto no limpio When se prepara respuesta cliente Then bloquea envío y no incluye datos internos.
- Tests: TC-MSG-008: WT (hilo/respuesta/CSRF), MT (evento/outbox), SEC (audiencia), IT (adjunto).
- Documentación a actualizar: DIARIO + INVENTARIO; plantillas
- Estimación: M (≤ 1 d)
- Riesgos: retry de email duplica aviso; outbox idempotente y message ID estable.

### MSG-009 — Bandeja: leído/no leído, asignar, etiquetar, archivar, papelera y purga
- Fase / épica / módulo owner: F4 / MSG / messaging
- Tipo: feat
- Depende de: MSG-007, LGL-003
- Descripción: Cubre: RF-MSG-05; implementa acciones operativas y purga diferida de conversaciones según retención.
- Ficheros/paquetes a crear o modificar (aproximado): `messaging.application.InboxActions`, estados/labels, worker de purga y auditoría.
- Impacto modular: muta solo tablas messaging; retención se configura localmente con política legal aprobada. Sin llamadas Java a legal ni ciclos.
- UI: acciones por fila/hilo con confirmación; papelera reversible antes de purga.
- Seguridad: `INBOX_MANAGE`; borrado definitivo/purga `INBOX_PURGE` solo ADMIN; CSRF/auditar.
- Criterios de aceptación (Given/When/Then):
  - Given EMPLOYEE autorizado When archiva/asigna/etiqueta Then estado y actor/fecha se registran y puede desarchivar.
  - Given EMPLOYEE intenta purga definitiva When solicita Then 403; ADMIN purga solo tras plazo y con auditoría mínima.
- Tests: TC-MSG-009: UT (acciones/retención), WT (permisos/confirmación), SEC (purga), IT (job).
- Documentación a actualizar: DIARIO + INVENTARIO; retención/roles en 07
- Estimación: M (≤ 1 d)
- Riesgos: papelera no equivale a supresión inmediata; purga sujeta a plazo legal pendiente.

### MSG-010 — Enlace mágico para continuar la conversación sin cuenta
- Fase / épica / módulo owner: F4 / MSG / messaging
- Tipo: feat
- Depende de: MSG-008, IAM-003
- Descripción: Cubre: RF-MSG-06; emite enlace restringido a un hilo para que contacto anónimo continúe conversación en portal.
- Ficheros/paquetes a crear o modificar (aproximado): `messaging.thread_access_token`, servicio de tokens, controller de enlace y template email.
- Impacto modular: token/hilo owner messaging; usa notification.api; aplica hashing propio alineado con criterios IAM-003, sin dependencia Java a identity. Sin ciclos.
- UI: página segura de continuación/respuesta con locale y aviso de expiración; no habilita navegación a otros hilos.
- Seguridad: ≥128 bits aleatorios, hash, expiración, un uso/revocación, alcance a una conversación y rate limit.
- Criterios de aceptación (Given/When/Then):
  - Given enlace válido When abre y responde Then solo accede al hilo vinculado; token se invalida según política de uso.
  - Given token expirado, revocado o usado en otro hilo When se presenta Then deniega sin enumerar conversación.
- Tests: TC-MSG-010: UT (hash/TTL/alcance), WT (flujo), SEC (replay/IDOR), IT (revocación).
- Documentación a actualizar: DIARIO + INVENTARIO; 07 enlace mágico
- Estimación: M (≤ 1 d)
- Riesgos: reenvío de email puede consumir enlace; decidir comportamiento de un uso antes de producción.

### MSG-011 — Actualización casi en tiempo real por polling HTMX
- Fase / épica / módulo owner: F4 / MSG / messaging
- Tipo: feat
- Depende de: MSG-008
- Descripción: Cubre: RF-MSG-08; refresca mensajes/no leídos por polling HTMX con intervalo acotado y pausa fuera de vista.
- Ficheros/paquetes a crear o modificar (aproximado): fragmento `messaging/inbox/thread`, endpoint incremental y `inbox-poll.js`.
- Impacto modular: consulta solo messaging; sin SSE, tabla nueva o dependencia de otros módulos. Ciclo: ninguno.
- UI: actualiza hilo/contador sin recarga, anuncia cambio discretamente y respeta reduced-motion.
- Seguridad: validar permiso en cada poll, CSRF para mutación y limitar frecuencia/datos retornados.
- Criterios de aceptación (Given/When/Then):
  - Given hilo abierto y mensaje nuevo When vence intervalo Then inserta solo mensajes autorizados y conserva foco/posición.
  - Given pestaña oculta o usuario sin acceso When se ejecuta poll Then pausa o responde 403 sin revelar contenido.
- Tests: TC-MSG-011: WT (poll/permiso), E2E (hilo), PERF (frecuencia/carga), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; configuración de polling
- Estimación: S (≤ 0,5 d)
- Riesgos: polling agresivo crece con usuarios; intervalo configurable y backoff.

### MSG-012 — Modelo preparado para IA (authorType, borradores, MessageReceived, puerto)
- Fase / épica / módulo owner: F4 / MSG / messaging
- Tipo: feat
- Depende de: MSG-002
- Descripción: Cubre: RF-MSG-09; prepara autor AI_AGENT, borradores pendientes de aprobación, evento y puerto sin integrar proveedor.
- Ficheros/paquetes a crear o modificar (aproximado): `messaging.domain.AuthorType`, `AiReplyAssistant` port, `MessageReceived`, estado de borrador.
- Impacto modular: contratos/eventos de messaging con IDs; implementaciones futuras fuera del módulo; sin tablas de proveedor. Ciclo: ninguno.
- UI: empleado puede revisar/aprobar borrador solo si se habilita manualmente; IA desactivada en MVP.
- Seguridad: no enviar datos fuera de plataforma; human-in-the-loop, logs sin PII y validación de prompt injection futura.
- Criterios de aceptación (Given/When/Then):
  - Given mensaje recibido When publica evento Then contiene IDs/contexto mínimo y ninguna llamada externa se ejecuta.
  - Given borrador AI_AGENT When no hay aprobación humana explícita Then no se entrega al cliente ni cambia estado.
- Tests: TC-MSG-012: UT (autor/borrador), MT (evento sin PII), SEC (no envío/publicación automática).
- Documentación a actualizar: DIARIO + INVENTARIO; ADR-011 futuro
- Estimación: S (≤ 0,5 d)
- Riesgos: IA supone transferencias/risgos RGPD y prompt injection; integración R3+ con ADR aprobado.

### MSG-013 — Convertir un contacto en solicitud
- Fase / épica / módulo owner: F4 / MSG / request
- Tipo: feat
- Depende de: MSG-008, REQ-005
- Descripción: Cubre: RF-MSG-04, RF-EMP-03; permite crear solicitud desde contacto existente conservando conversación y autoría.
- Ficheros/paquetes a crear o modificar (aproximado): `request.web.ContactToRequestController`, command service y selector de tipo/categoría.
- Impacto modular: conversión vive en request.web y consume `messaging.api`; bandeja solo enlaza. request→messaging permitido, sin ciclo.
- UI: acción en hilo de empleado, confirmación y navegación al nuevo detalle.
- Seguridad: `REQUESTS_VIEW_ALL`, CSRF, validar conversación accesible, evitar doble conversión y conservar privacidad.
- Criterios de aceptación (Given/When/Then):
  - Given contacto válido When EMPLOYEE convierte Then crea solicitud con vínculo al hilo y registra actor/fecha.
  - Given contacto ya convertido o empleado sin permiso When repite acción Then recibe 409/403 sin duplicar solicitud.
- Tests: TC-MSG-013: WT (conversión/permiso), IT (atomicidad/vínculo), SEC (replay/IDOR).
- Documentación a actualizar: DIARIO + INVENTARIO; flujo en 03/05
- Estimación: M (≤ 1 d)
- Riesgos: datos de contacto no bastan para solicitud completa; completar campos mínimos antes de convertir.

### NTF-004 — Plantillas bilingües de solicitudes y mensajería y alertas a ADMIN
- Fase / épica / módulo owner: F4 / NTF / notification
- Tipo: feat
- Depende de: REQ-010, MSG-008
- Descripción: Cubre: RF-NOT-01; añade plantillas ES/EN de mensajes/solicitudes y alertas de aprovisionamiento a ADMIN.
- Ficheros/paquetes a crear o modificar (aproximado): templates `notification/email/request_*`, `message_*`, `provisioning_*` y tests render.
- Impacto modular: consume eventos/API de request, messaging y simbiosphere; notification no depende de productores. Sin ciclos.
- UI: n/a (contenido de email); incluir enlace seguro al panel correspondiente.
- Seguridad: resolver destinatarios por SPI; no poner PII/token en asunto, URL ni logs; plain text disponible.
- Criterios de aceptación (Given/When/Then):
  - Given evento RequestStatusChanged/MessagePosted When locale es EN Then envía plantilla EN con enlace interno y texto plano.
  - Given aprovisionamiento ACTION_REQUIRED/FAILED When ADMIN recibe alerta Then datos se minimizan y destinatario proviene del SPI autorizado.
- Tests: TC-NTF-004: UT (render/locale), MT (eventos), SEC (PII/destinatario), IT (Mailpit).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Notificaciones»
- Estimación: M (≤ 1 d)
- Riesgos: cobertura de templates crece por evento; catálogo de claves y test de paridad.

### NTF-005 — Preferencias de notificación por usuario
- Fase / épica / módulo owner: F4 / NTF / notification
- Tipo: feat
- Depende de: IAM-011, NTF-003
- Descripción: Cubre: RF-NOT-03; permite preferencias por categorías de email operacional y aplica reglas antes de enviar.
- Ficheros/paquetes a crear o modificar (aproximado): `notification.notification_preference`, API, servicio de filtrado y panel por URL.
- Impacto modular: tabla preference owner notification; `identity` proporciona actor por SPI; sin dependencia Java inversa. Ciclo: ninguno.
- UI: preferencias en `/cuenta/notificaciones`, switches etiquetados ES/EN y accesibles.
- Seguridad: owner-only, CSRF; no convertir consentimiento comercial en preferencia técnica ni desactivar avisos legales esenciales.
- Criterios de aceptación (Given/When/Then):
  - Given usuario desactiva categoría opcional When se encola aviso Then worker lo suprime respetando mensajes obligatorios.
  - Given usuario cambia preference ajena o sin CSRF When guarda Then responde 403 y no altera configuración.
- Tests: TC-NTF-005: WT (preferencias/CSRF), UT (reglas de envío), SEC (IDOR/consentimiento).
- Documentación a actualizar: DIARIO + INVENTARIO; categorías en 13
- Estimación: M (≤ 1 d)
- Riesgos: distinguir obligación transaccional/comercial requiere revisión legal.

### E2E-003 — E2E: contacto → respuesta
- Fase / épica / módulo owner: F4 / E2E / test
- Tipo: test
- Depende de: MSG-010, UIK-009
- Descripción: Cubre: RNF-TST-03; prueba contacto anónimo, entrada en bandeja, respuesta y continuación por enlace mágico.
- Ficheros/paquetes a crear o modificar (aproximado): `e2e/contact-reply.spec.ts`, fixtures Mailpit, enlace temporal y clock controlado.
- Impacto modular: recorre messaging/notification por navegador/HTTP; sin contratos/tablas de producción adicionales. Ciclo: ninguno.
- UI: ES/EN, móvil/escritorio; contacto, respuesta, adjunto y página de continuación.
- Seguridad: email/cuenta sintéticos, token no guardado en vídeo/screenshot, validar acceso al hilo limitado.
- Criterios de aceptación (Given/When/Then):
  - Given anónimo completa formulario When EMPLOYEE responde Then acuse llega por Mailpit y enlace abre hilo correcto.
  - Given enlace expirado o hilo distinto When se usa Then se deniega sin mostrar mensaje/conversación.
- Tests: TC-E2E-003: E2E (Playwright/Mailpit), SEC (token/alcance), A11Y (formularios).
- Documentación a actualizar: DIARIO + INVENTARIO; README «E2E»
- Estimación: M (≤ 1 d)
- Riesgos: polling/correo asíncrono causa flakiness; esperar condiciones por evento y usar reloj fijo.
