# Contrato visual inicial

| Ruta | Respuesta | Acceso actual | Locale/tema | Estado |
|---|---|---|---|---|
| `/` | Redirect server-side a `/es` o `/en` según negociación permitida | Público | Accept-Language; tema técnico | Implementada |
| `/es`, `/en` | HTML Thymeleaf | Público | Locale por URL; tema cookie/sistema | Shell visual F0, sin contenido de negocio |
| `/assets/**` | CSS/JS local con huella | Público | No aplica | Build local F0 |
| `/preferences/locale/{lang}` | POST + redirect local | Público con CSRF | Actualiza cookie técnica; conserva ruta | Implementación base añadida |
| `/preferences/theme` | POST same-origin + 204 | Público con CSRF; persistencia por SPI si autenticado | Cookie anónima / perfil autenticado | Perfil pendiente de módulo identity |
| `/cuenta/**`, `/gestion/**` | Sin vista de negocio en F0 | Denegación por defecto | Cookie/locale cuando exista página | Reservadas; endpoints aún no implementados |

Los slugs de contenido no existen todavía; el selector conserva la ruta como fallback, y el mapeo de alternates CMS queda para CMS-004. Los layouts `account` y `backoffice` son infraestructura visual, no habilitan acceso a esas áreas.
