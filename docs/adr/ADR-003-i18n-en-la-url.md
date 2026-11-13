# ADR-003 — i18n en la URL

- Estado: Borrador
- Fecha: 2026-11-10

## Contexto
El sitio público ES/EN requiere SEO, canonical/hreflang y conservar la página al cambiar idioma. El área privada debe seguir preferencias del usuario y no exponer navegación técnica en cada enlace.

## Decisión recomendada
Usar prefijo `/{lang}` (`/es/...`, `/en/...`) para páginas públicas y slugs CMS por locale; raíz `/` redirige según `Accept-Language`/cookie. El área privada usa perfil autenticado → cookie `sp_lang` → `Accept-Language`. El selector conserva la página equivalente; si no existe traducción, aplica fallback editorial explícito. Cambio de slug CMS registra 301 por locale. Emails se renderizan en locale del destinatario; fechas persisten UTC y se presentan en locale/`Europe/Madrid`.

## Alternativas
- Cookie/`?lang=` solamente en público: URL no identifica idioma y complica indexación/canonical; no recomendado.
- Prefijo de idioma también en privado: posible, pero duplica rutas autenticadas y aporta poco al flujo; reevaluar si hay requisito de producto.

## Consecuencias
Los controladores públicos deben validar locale permitido, generar canonical/hreflang y no enlazar traducciones inexistentes. Bundles por módulo comparten claves ES/EN; CI comprueba paridad.

## Verificaciones
- UIK-005: `PortalLocaleResolverTest` (2), `I18nKeyParityTest` (3) y `WebViewsTest` (4) pasan con JDK 21; el test MVC verifica cambio ES/EN, cookie, preservación de ruta local y rechazo de locale no soportado. El E2E del selector permanece pendiente porque no se pudo arrancar la aplicación.
- CMS-004/SEO-001/SEO-002: slugs/fallback/301/canonical/sitemap.
