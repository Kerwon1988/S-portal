# ADR-009 — Cifrado de PII

- Estado: Borrador; cifrado de payload/token/MFA implementado parcialmente, pendiente de validación
- Fecha: 2026-11-10

## Contexto
S-portal almacena identificadores fiscales/teléfono, secreto TOTP, IP si necesaria, outbox y datos de usuario/mensajes. Algunos campos requieren búsqueda, pero secretos/tokens no deben persistirse en claro. SQL Server/TDE y edición no se verifican mediante los gates automatizados y quedan pendientes de decisión de plataforma.

## Decisión recomendada
Cifrado por columna AES-256-GCM con nonce aleatorio y key versioning desde gestor de secretos para `tax_id`, teléfono, TOTP, IP de consentimiento si se retiene y payload outbox. Contraseñas con Argon2id; tokens/códigos se almacenan hashados; no cifrar tokens para recuperarlos. Email/nombres/textos de conversación necesarios para buscar se protegen por cifrado de disco/TDE/backups según edición, permisos y minimización. Índice ciego HMAC solo si búsqueda exacta sobre columna cifrada es requisito aprobado.

## Alternativas
- Cifrar toda columna que contenga PII: maximiza protección, pero rompe búsqueda/orden y añade rotación/operación; aplicar selectivamente.
- Solo TDE: protege archivos/backups frente a acceso físico, no una copia lógica/consulta autorizada; no sustituye cifrado de campo.
- SHA-256 sin sal para email: sirve a deduplicación, pero habilita correlación/diccionario; no copiar patrón externo para datos sensibles.

## Consecuencias
Rotación/recuperación de claves se prueba; backups cifrados y restauración usan versión de clave disponible. Logs y métricas nunca incluyen PII. La base legal, campos y retenciones dependen de responsable legal.

`shared.spi.SensitiveDataProtector` tiene implementación AES-256-GCM con nonce aleatorio y key ID externo; se usa para payload de outbox, secreto TOTP y material anti-bot. Los tokens de un solo uso y códigos de recuperación se guardan como hashes. El índice de invitaciones `identity.one_time_token.email_key` usa HMAC-SHA-256 con clave derivada y separación por propósito; no es un SHA-256 sin clave del correo para registros nuevos. La revocación consulta fingerprints generados con todas las claves del key ring configurado y el hash heredado durante la transición, por lo que las claves anteriores deben conservarse hasta que hayan caducado o se hayan revocado las invitaciones asociadas. El hash heredado solo se consulta para revocar invitaciones previas; su eliminación persistente depende de la política y el mecanismo de purga de tokens, aún pendientes. Los tests unitarios de cifrado/fingerprint se añadieron, pero no se ejecutaron por el bloqueo TLS de Maven. No se ha validado rotación, recuperación de claves, almacenamiento real en gestor de secretos ni política de retención aprobada.

## Verificaciones pendientes
- FND-001: crypto/Argon2/BCrypt adapter y cifrado con Boot 4 (la implementación Prompt 09 sigue sin compilación/tests ejecutados).
- ADR-006/HRD-013: revisar edición, TDE, backups y licencia SQL Server con fuentes oficiales; no se requiere una prueba MSSQL para los gates de desarrollo.
- HRD-008/LGL-003: RAT, campos/retención y aprobación legal.
- HRD-010: gestor, key versioning y rotación operacional.
