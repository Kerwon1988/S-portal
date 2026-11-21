# Registro de actividades de tratamiento (preparación)

Estado: **PREPARADO_NO_EJECUTADO** hasta validación DPO/legal y proveedores aprobados. No contiene datos reales.

| Tratamiento | Owner | Finalidad | Categorías | Interesados | Base legal |
|---|---|---|---|---|---|
| Identidad y sesiones | identity | acceso/seguridad | credenciales, perfil técnico | usuarios/trabajadores | pendiente de aprobación legal |
| Registro cliente | customer | alta y gestión del cliente | contacto/fiscal/empresa | clientes | pendiente |
| Solicitudes/presupuestos | request | gestión comercial | solicitud, documentos, estado | clientes/empleados | pendiente |
| Contacto/bandeja | messaging | atención y seguimiento | mensajes, adjuntos, audio | contactos | consentimiento/legítimo a validar |
| Notificaciones | notification | mensajes transaccionales | destinatario técnico, estado | usuarios | ejecución relación a validar |
| Archivos | files | almacenamiento seguro | adjuntos/documentos | usuarios | depende del tratamiento owner |
| Auditoría/errores | audit/config | seguridad e incidentes | IDs técnicos/correlation | usuarios | interés legítimo a validar |

Medidas: TLS, cifrado AES-GCM en PII seleccionada, minimización, RBAC/ownership, CSRF, logs redactados, inventario privado single-writer, retención desactivada hasta aprobación. Hosting/email/captcha/antivirus/DNS y transferencias/regiones quedan pendientes por proveedor/contrato.
