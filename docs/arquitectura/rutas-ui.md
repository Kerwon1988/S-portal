# Contrato visual inicial

| Ruta | Respuesta | Acceso actual | Locale/tema | Estado |
|---|---|---|---|---|
| `/` | Redirect server-side a `/es` o `/en` según negociación permitida | Público | Accept-Language; tema técnico | Implementada |
| `/es`, `/en` | HTML Thymeleaf | Público | Locale por URL; tema cookie/sistema | Shell visual F0, sin contenido de negocio |
| `/assets/**` | CSS/JS local con huella | Público | No aplica | Build local F0 |
| `/preferences/locale/{lang}` | POST + redirect local | Público con CSRF | Actualiza cookie técnica; conserva ruta | Implementación base añadida |
| `/preferences/theme` | POST same-origin + 204 | Público con CSRF; persistencia por SPI si autenticado | Cookie anónima / perfil autenticado | Perfil pendiente de módulo identity |
| `/cuenta/**` | `/cuenta/datos` muestra información de derechos; no hay solicitud/exportación ni mutación disponible | Sesión autenticada | Locale del perfil | Parcial; baja suspendida hasta política de retención aprobada |
| `/gestion/clientes`, `/gestion/trabajadores`, `/gestion/usuarios/{id}` | Listados paginados y ficha básica; orden allowlist por fecha/nombre/correo ascendente o descendente | ADMIN con permiso de gestión; validación en servicio | ES/EN, tema del shell | Parcial; auditoría lazy disponible con `AUDIT_VIEW`; request/messaging/SimbioSphere pendientes; tests Java/H2 pasan en la última verificación completa |
| `/gestion/auditoria/usuarios/{id}/panel` | Fragmento HTMX del historial de auditoría de una cuenta | `AUDIT_VIEW` validado en la ruta owner audit | ES/EN, Thymeleaf | Implementado; tests Java/H2 pasan en la última verificación completa |
| `/gestion/clientes/export.csv` | Export limitado de clientes (máximo 1000 filas), respetando filtros y orden de la lista | ADMIN + `USERS_MANAGE` + `CLIENTS_EXPORT` | CSV UTF-8 | Código presente; neutraliza fórmulas y audita; tests Java/H2 pasan en la última verificación completa |
| `/gestion/trabajadores/invitacion`, `/gestion/trabajadores/{id}` | Invitación y edición de perfil laboral | ADMIN + `STAFF_MANAGE`; mutaciones con CSRF | ES/EN, tema del shell | Parcial; tests Java/H2 pasan en la última verificación completa |
| `/gestion/identidad/invitaciones/pendientes/clientes`, `/gestion/identidad/invitaciones/pendientes/trabajadores` | Listados paginados de 25 invitaciones pendientes; POST scoped `/.../{invitationId}/reenviar` rota el enlace | ADMIN + `USERS_MANAGE` (clientes) o `STAFF_MANAGE` (trabajadores); CSRF en POST | ES/EN, Thymeleaf; email enmascarado; caducadas reenviables | Implementado para listado/reenvío; tests focales y verify completo pasan |
| **Propuestas USR-002 pendientes de owner:** `/gestion/solicitudes/usuarios/{publicId}/panel`, `/gestion/mensajes/usuarios/{publicId}/panel`, `/gestion/simbiosphere/usuarios/{publicId}/panel` | Fragmentos Thymeleaf owner-scoped, GET de solo lectura, resumen acotado, fallback accesible | Candidatos: `REQUESTS_VIEW_ALL`, `INBOX_MANAGE`, `PROVISIONING_MANAGE`; cada owner debe aprobar permisos, relación con cuenta, campos y anti-IDOR | HTMX same-origin; sin mensajes libres/notas internas ni acceso browser→SimbioSphere; futura integración DEMO backend→backend con scope tenant/location explícito | **No implementadas.** Propuesta y límites de compatibilidad en [contratos-ui-backend.md](../plan/contratos-ui-backend.md); falta aprobación owners y un contrato HTTP de DEMO |
| `/gestion/**` (resto) | Sin acceso salvo rutas clasificadas explícitamente | Denegación por defecto | — | Resto de acciones USR-003 y E2E-001 pendientes |

Los slugs de contenido no existen todavía; el selector conserva la ruta como fallback, y el mapeo de alternates CMS queda para CMS-004. Los layouts `account` y `backoffice` son infraestructura visual, no habilitan acceso a esas áreas.
