# F2 — Notificaciones e identidad (correo, verificación, recuperación e invitaciones): tareas

- Objetivo: habilitar correo fiable y operaciones de identidad con tokens seguros (hito M2; ver [roadmap](../10-roadmap-backlog.md)).
- Módulo(s) owner: `notification`, `identity` y `config` para protección anti-bot transversal.

### NTF-001 — notification: outbox con payload cifrado y purga
- Fase / épica / módulo owner: F2 / NTF / notification
- Tipo: feat
- Depende de: FND-006, FND-003
- Descripción: Cubre: RF-NOT-03; persiste mensajes pendientes en outbox para entregar después del commit con payload cifrado y purga.
- Ficheros/paquetes a crear o modificar (aproximado): `notification.domain.OutboxMessage`, `notification.outbox_message`, migración, cifrador/retención.
- Impacto modular: tabla `notification.outbox_message` owner notification; consume IDs/eventos publicados, sin dependencia de módulos productores. Ciclo: ninguno.
- UI: n/a (cola interna; preferencias en NTF-005).
- Seguridad: cifrar payload con clave versionada; no guardar secretos en logs; payload de evento solo IDs.
- Criterios de aceptación (Given/When/Then):
  - Given evento confirmado When se crea outbox Then payload se cifra y solo se publica tras commit.
  - Given mensaje enviado o fallido caducado When corre purga Then elimina payload conforme retención y conserva métrica agregada.
- Tests: TC-NTF-001: UT (cifrado/estados), MT (publicación post-commit) e IT de persistencia/purga con H2. No se requieren pruebas SQL Server/Testcontainers.
- Documentación a actualizar: DIARIO + INVENTARIO; ADR-007/009
- Estimación: M (≤ 1 d)
- Riesgos: pérdida de clave impide descifrar; versionar y probar rotación/restauración.

### NTF-002 — SMTP + Mailpit y plantillas base de email bilingües con texto plano
- Fase / épica / módulo owner: F2 / NTF / notification
- Tipo: feat
- Depende de: NTF-001, UIK-005
- Descripción: Cubre: RF-NOT-01, RF-NOT-04; implementa puerto `EmailSender`, SMTP y plantillas Thymeleaf ES/EN con alternativa texto plano.
- Ficheros/paquetes a crear o modificar (aproximado): `notification.api.EmailSender`, `SmtpEmailSender`, templates `notification/email/`, config Mailpit.
- Impacto modular: notification publica envío; consumidores pasan IDs/datos mínimos; sin tabla nueva ni dependencia de negocio. Ciclo: ninguno.
- UI: correo responsive de marca y locale del destinatario; cabecera/texto alternativo y versión plain text.
- Seguridad: credenciales SMTP solo por entorno/secret manager; sanitizar texto y no loguear dirección/cuerpo.
- Criterios de aceptación (Given/When/Then):
  - Given destinatario ES/EN When se envía plantilla Then llega HTML y texto plano con asunto/locale correctos a Mailpit.
  - Given SMTP sin credenciales When arranca perfil prod Then falla validación de configuración sin usar secreto por defecto.
- Tests: TC-NTF-002: UT (locale/render/escape), IT (Mailpit), SEC (sin secreto literal).
- Documentación a actualizar: DIARIO + INVENTARIO; ADR-007
- Estimación: M (≤ 1 d)
- Riesgos: proveedor final y región pendientes; mantener adapter SMTP desacoplado.

### NTF-003 — Worker de envío con reintentos, métricas y alerta de cola
- Fase / épica / módulo owner: F2 / NTF / notification
- Tipo: feat
- Depende de: NTF-002
- Descripción: Cubre: RF-NOT-03, RNF-OPS-01; consume outbox con reintentos acotados, backoff y estado final observable.
- Ficheros/paquetes a crear o modificar (aproximado): `notification.application.OutboxWorker`, `DeliveryPolicy`, métricas/health y job config.
- Impacto modular: actualiza solo tabla notification; destinatarios resueltos por `shared.spi.RecipientDirectory`; eventos sin PII. Ciclo: ninguno.
- UI: n/a (worker; errores visibles a ADMIN mediante alertas operativas).
- Seguridad: reclamar filas atómicamente, límite de concurrencia, logs con IDs/correlation ID y sin destinatario.
- Criterios de aceptación (Given/When/Then):
  - Given SMTP temporalmente caído When worker reclama mensaje Then reintenta con backoff y no duplica el envío tras restart.
  - Given límite de intentos agotado When se supera umbral Then marca fallo, emite métrica/alerta y purga según retención.
- Tests: TC-NTF-003: UT (backoff/claim), IT (restart/idempotencia), SEC (logs sin PII).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Correo»
- Estimación: M (≤ 1 d)
- Riesgos: envío SMTP no garantiza exactly-once; usar Message-ID/idempotencia y documentar duplicados raros.

### IAM-003 — Servicio de tokens de un solo uso hasheados
- Fase / épica / módulo owner: F2 / IAM / identity
- Tipo: feat
- Depende de: IAM-001
- Descripción: Cubre: RF-IAM-04, RNF-SEC-05; implementa emisión, hash, expiración, consumo atómico y revocación de tokens opacos.
- Ficheros/paquetes a crear o modificar (aproximado): `identity.application.OneTimeTokenService`, `identity.token`, migración, clock injectable.
- Impacto modular: tabla token pertenece a `identity`; no expone token en API ni evento. Ciclo: ninguno (usa shared).
- UI: n/a (servicio para verificación, reset e invitación).
- Seguridad: ≥128 bits aleatorios; guardar hash SHA-256, comparar en tiempo constante, TTL y nunca registrar token.
- Criterios de aceptación (Given/When/Then):
  - Given token válido When se consume una vez Then la operación atómica lo marca usado y el siguiente intento falla.
  - Given token expirado/revocado When se presenta Then rechaza sin indicar estado de cuenta ni registrar valor secreto.
- Tests: TC-IAM-003: UT (TTL/one-use/hash), IT (consumo concurrente), SEC (entropía/logs).
- Documentación a actualizar: DIARIO + INVENTARIO; ADR-002
- Estimación: M (≤ 1 d)
- Riesgos: reloj o consumo no atómico habilita replay; usar UTC y update condicional.

### IAM-006 — Anti-bot ALTCHA y honeypot
- Fase / épica / módulo owner: F2 / IAM / config
- Tipo: feat
- Depende de: UIK-008, FND-009
- Descripción: Cubre: RF-IAM-02, RNF-SEC-07; protege registro/contacto con honeypot y desafío ALTCHA autoalojado.
- Ficheros/paquetes a crear o modificar (aproximado): `config/security/AntiBotVerifier`, fragmentos de formulario, propiedades y pruebas.
- Impacto modular: filtro/servicio en `config`; identity/messaging llaman API neutral. Sin tablas/eventos. Ciclo: ninguno.
- UI: control accesible, ES/EN, fallback cuando JS está desactivado; no usar desafío visual como única alternativa.
- Seguridad: token de desafío de corta vida, validar en servidor y rate limit; no usar proveedor externo por defecto.
- Criterios de aceptación (Given/When/Then):
  - Given desafío ALTCHA y formulario válido When se verifica Then permite envío una vez sin almacenar respuesta innecesaria.
  - Given honeypot completo, token inválido o cuota excedida When envía Then deniega con respuesta genérica y registra métrica agregada.
- Tests: TC-IAM-006: UT (verificador/replay), WT (formulario), SEC (bypass/abuso), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; 13 captcha por defecto
- Estimación: M (≤ 1 d)
- Riesgos: proof-of-work afecta móviles; calibrar dificultad y permitir alternativa accesible.

### IAM-007 — Verificación de email: emisión, endpoint y reenvío limitado
- Fase / épica / módulo owner: F2 / IAM / identity
- Tipo: feat
- Depende de: IAM-003, NTF-003, IAM-005
- Descripción: Cubre: RF-IAM-04; emite token tras registro, verifica email y limita reenvíos sin revelar existencia de cuenta.
- Ficheros/paquetes a crear o modificar (aproximado): `identity.application.EmailVerificationService`, controller `/verificar-email`, template, evento `EmailVerified`.
- Impacto modular: publica `identity.api.EmailVerified` con user ID; consume `notification.api`; tabla token owner identity. Ciclo: ninguno.
- UI: página ES/EN con éxito, expirado y solicitud de reenvío; nunca incluir email completo en URL.
- Seguridad: token hashado/de un uso; rate limit por cuenta/IP, antifijación de sesión y no enumeración.
- Criterios de aceptación (Given/When/Then):
  - Given enlace válido When se confirma Then user queda verificado una vez y publica evento con ID sin email/token.
  - Given enlace vencido o reenvío excesivo When se solicita Then muestra respuesta genérica y no altera estado.
- Tests: TC-IAM-007: UT (estado/reenvío), WT (endpoint), MT (evento sin PII), SEC.
- Documentación a actualizar: DIARIO + INVENTARIO; README «Verificar email»
- Estimación: M (≤ 1 d)
- Riesgos: doble verificación en DEMO; no llamar integración hasta RF-SIM-05.

### IAM-008 — Recuperación y cambio de contraseña
- Fase / épica / módulo owner: F2 / IAM / identity
- Tipo: feat
- Depende de: IAM-003, NTF-003, IAM-005
- Descripción: Cubre: RF-IAM-07; habilita solicitud de reset, cambio autenticado e invalidación de sesiones tras cambiar credencial.
- Ficheros/paquetes a crear o modificar (aproximado): `identity.application.PasswordRecoveryService`, controllers/templates `/recuperar`, `/restablecer` y eventos de auditoría.
- Impacto modular: consume `notification.api`, persiste token en identity y registra auditoría; sin dependencia cíclica. Ciclo: ninguno.
- UI: formularios ES/EN, confirmación genérica, campos con error asociado y tema móvil.
- Seguridad: token hashado y de un uso; respuesta anti-enumeración; CSRF; Argon2id y revocar sesiones activas.
- Criterios de aceptación (Given/When/Then):
  - Given cuenta existente o inexistente When solicita reset Then respuesta, tiempo y texto no revelan existencia.
  - Given token válido y contraseña conforme When restablece Then actualiza hash, consume token e invalida sesiones; token repetido falla.
- Tests: TC-IAM-008: UT (política/consumo), WT (anti-enumeración/CSRF), IT (revocación), SEC.
- Documentación a actualizar: DIARIO + INVENTARIO; README «Recuperación»
- Estimación: M (≤ 1 d)
- Riesgos: abuso de correo y robo de sesión; cuotas y notificación de cambio obligatorias.

### IAM-009 — MFA TOTP para ADMIN y opcional para EMPLOYEE
- Fase / épica / módulo owner: F2 / IAM / identity
- Tipo: feat
- Depende de: IAM-004
- Descripción: Cubre: RF-IAM-06; agrega enrolamiento y desafío TOTP RFC 6238, obligatorio para ADMIN y opt-in para EMPLOYEE en MVP.
- Ficheros/paquetes a crear o modificar (aproximado): `identity.application.MfaService`, secret/codes en esquema identity, templates y migración.
- Impacto modular: tabla MFA owner `identity`; solo evento de cambio con user ID; sin dependencias externas de negocio. Ciclo: ninguno.
- UI: `/cuenta/seguridad` y paso de login accesible; códigos de recuperación se muestran una vez.
- Seguridad: secreto cifrado por columna, códigos de recuperación hasheados, rate limit y regeneración revoca anteriores.
- Criterios de aceptación (Given/When/Then):
  - Given ADMIN enrolado When inicia sesión Then requiere código TOTP vigente y deniega replay/código expirado.
  - Given EMPLOYEE sin MFA opt-in When inicia sesión Then no se fuerza segundo factor en MVP; estado se audita sin exponer secreto.
- Tests: TC-IAM-009: UT (ventana/replay), WT (enrolamiento/login), SEC (secret/códigos cifrados/hash).
- Documentación a actualizar: DIARIO + INVENTARIO; 13 MFA EMPLOYEE
- Estimación: M (≤ 1 d)
- Riesgos: reloj del dispositivo desfasado; tolerancia limitada y códigos de recuperación offline.

### IAM-010 — Invitaciones con caducidad y definición de contraseña
- Fase / épica / módulo owner: F2 / IAM / identity
- Tipo: feat
- Depende de: IAM-002, IAM-003, NTF-003
- Descripción: Cubre: RF-IAM-09, RF-IAM-10; emite invitación de un uso para empleado o cliente y permite que destinatario defina su credencial.
- Ficheros/paquetes a crear o modificar (aproximado): `identity.application.InvitationService`, token type, controller/templates y auditoría.
- Impacto modular: `identity` publica aceptación con ID; consume `notification.api`; `staff/customer` reaccionan por contrato. Sin ciclo.
- UI: `/invitacion/aceptar`, ES/EN, explicación de términos y validación de contraseña.
- Seguridad: solo ADMIN invita; hash/TTL/one-use; no aceptar contraseña establecida por admin ni incluirla en email.
- Criterios de aceptación (Given/When/Then):
  - Given invitación válida When destinatario acepta términos y define contraseña Then se activa el tipo/rol previsto y token se consume.
  - Given token expirado, revocado o correo distinto When se acepta Then responde genérico y no crea/eleva usuario.
- Tests: TC-IAM-010: UT (TTL/tipos), WT (aceptación), IT (evento/outbox), SEC (replay/privilege escalation).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Invitaciones»
- Estimación: M (≤ 1 d)
- Riesgos: alta SimbioSphere por admin requiere aceptación previa de términos (SIM-005).

### IAM-011 — Perfil: datos, idioma, tema y aviso de contraseña independiente
- Fase / épica / módulo owner: F2 / IAM / identity
- Tipo: feat
- Depende de: IAM-004, UIK-004
- Descripción: Cubre: RF-IAM-08; permite al usuario actualizar datos mínimos, locale, tema y preferencias, y aclara credencial separada de SimbioSphere.
- Ficheros/paquetes a crear o modificar (aproximado): `identity.web.ProfileController`, `templates/identity/profile.html`, `identity.api` y bundles.
- Impacto modular: actualiza solo `identity`; consume config locale/tema por SPI, sin llamada Java a SimbioSphere. Ciclo: ninguno.
- UI: `/cuenta/perfil`, formularios ES/EN, tema y móvil; paneles de notificación/legal se componen por URL.
- Seguridad: solo sesión propia, CSRF, validar cambios de email y no devolver hashes/secretos.
- Criterios de aceptación (Given/When/Then):
  - Given cliente actualiza idioma/tema When guarda Then preferencia persiste y se aplica en siguiente petición.
  - Given cliente cambia perfil When consulta otra cuenta o SimbioSphere When no hay SSO Then no puede acceder a datos ajenos ni cambiar contraseña externa.
- Tests: TC-IAM-011: WT (perfil/CSRF/owner), SEC (IDOR/sin credenciales), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; README «Perfil»
- Estimación: M (≤ 1 d)
- Riesgos: mezclar contraseñas de portal y SimbioSphere; mantener aviso explícito hasta SSO futuro.

### IAM-013 — Implementación de los SPIs CurrentUser y RecipientDirectory
- Fase / épica / módulo owner: F2 / IAM / identity
- Tipo: feat
- Depende de: IAM-004, FND-003
- Descripción: Cubre: RF-NOT-03; implementa usuario actual y resolución mínima de destinatarios para notificaciones sin acoplar notification a identity.
- Ficheros/paquetes a crear o modificar (aproximado): `identity.infrastructure.spi.CurrentUserAdapter`, `RecipientDirectoryAdapter`, contratos `shared.spi`.
- Impacto modular: identity implementa SPIs de `shared`; notification consume solo `RecipientDirectory`. Ciclo: ninguno (notification no depende de identity).
- UI: n/a (adaptadores de aplicación).
- Seguridad: exigir actor autenticado donde aplique; devolver datos mínimos bajo autorización y nunca incluirlos en logs.
- Criterios de aceptación (Given/When/Then):
  - Given request autenticada When módulos consultan CurrentUser Then reciben ID/roles sin entidad JPA ni PII adicional.
  - Given evento notificable When notification resuelve destinatario Then usa SPI y no importa paquetes `identity`.
- Tests: TC-IAM-013: UT (actor/destinatarios), MT (límite modular), SEC (mínimo privilegio).
- Documentación a actualizar: DIARIO + INVENTARIO; README «SPIs»
- Estimación: S (≤ 0,5 d)
- Riesgos: RecipientDirectory puede convertirse en consulta genérica; limitar por propósito/ID.
