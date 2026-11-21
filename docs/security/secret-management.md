# Gestión y rotación de secretos

## Requisitos

- El provider/plataforma está pendiente; las propiedades se enlazan por variables externas.
- DB, SMTP, HMAC M2M y cifrado requieren identidades separadas, mínimo privilegio, auditoría y revocación.
- No hay literales productivos en compose, imagen, logs, artefactos, capturas ni documentación.
- Rotación: provisionar clave nueva, desplegar aceptación dual si el protocolo lo admite, drenar, marcar clave anterior no activa y revocar tras ventana aprobada.
- Para AES-GCM de datos, `PORTAL_ENCRYPTION_KEYS` conserva claves anteriores solo durante re-cifrado/lectura aprobada; la key activa se identifica por ID.
- Tras exposición: revocar primero, rotar, invalidar sesiones/tokens afectados, revisar logs redactados y documentar incidente.

## Estado

Binding real, doble clave, revocación, acceso auditado y secret scan de plataforma: **PREPARADO_NO_EJECUTADO**. El pipeline CI mantiene OWASP Dependency-Check; la selección de secret manager no está aprobada.
