# Contrato visual inicial

| Ruta | Respuesta | Acceso actual | Locale/tema | Estado |
|---|---|---|---|---|
| `/` | Redirect server-side a `/es` o `/en` según negociación permitida | Público | Accept-Language; tema técnico | Implementada |
| `/es`, `/en` | HTML Thymeleaf | Público | Locale por URL; tema cookie/sistema | Shell visual F0, sin contenido de negocio |
| `/assets/**` | CSS/JS local con huella | Público | No aplica | Build local F0 |
| `/preferences/locale/{lang}` | POST + redirect local | Público con CSRF | Actualiza cookie técnica; conserva ruta | Implementación base añadida |
| `/preferences/theme` | POST same-origin + 204 | Público con CSRF; persistencia por SPI si autenticado | Cookie anónima / perfil autenticado | Perfil pendiente de módulo identity |
| `/cuenta/**` | `/cuenta/datos` muestra información de derechos; no hay solicitud/exportación ni mutación disponible | Sesión autenticada | Locale del perfil | Parcial; baja suspendida hasta política de retención aprobada |
| `/gestion/clientes`, `/gestion/trabajadores`, `/gestion/usuarios/{id}` | Listados paginados y ficha básica; orden allowlist por fecha/nombre/correo ascendente o descendente | ADMIN con permiso de gestión; validación en servicio | ES/EN, tema del shell | Parcial; auditoría lazy disponible con `AUDIT_VIEW`; request/messaging/SimbioSphere pendientes; fuentes y tests compilan, JUnit bloqueado por dependencia offline |
| `/gestion/auditoria/usuarios/{id}/panel` | Fragmento HTMX del historial de auditoría de una cuenta | `AUDIT_VIEW` validado en la ruta owner audit | ES/EN, Thymeleaf | Implementación añadida; tests compilados pero no ejecutados |
| `/gestion/clientes/export.csv` | Export limitado de clientes (máximo 1000 filas), respetando filtros y orden de la lista | ADMIN + `USERS_MANAGE` + `CLIENTS_EXPORT` | CSV UTF-8 | Código presente; neutraliza fórmulas y audita; tests compilan pero no se ejecutan por dependencia JUnit ausente en caché |
| `/gestion/trabajadores/invitacion`, `/gestion/trabajadores/{id}` | Invitación y edición de perfil laboral | ADMIN + `STAFF_MANAGE`; mutaciones con CSRF | ES/EN, tema del shell | Parcial; tests Java pendientes de ejecución |
| `/gestion/**` (resto) | Sin acceso salvo rutas clasificadas explícitamente | Denegación por defecto | — | Acciones generales USR-003 y E2E-001 pendientes |

Los slugs de contenido no existen todavía; el selector conserva la ruta como fallback, y el mapeo de alternates CMS queda para CMS-004. Los layouts `account` y `backoffice` son infraestructura visual, no habilitan acceso a esas áreas.
