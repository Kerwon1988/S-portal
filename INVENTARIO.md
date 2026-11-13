# Inventario técnico de S-portal

Actualizado: 2026-11-13. Alcance: fundaciones F0, PROMPT 03, captura backend/BD y telemetría frontend parcial del PROMPT 04.

## Build y estructura

- `com.simbio.core:portal`, Java 21, Maven Wrapper/Maven 3.6.3, Spring Boot 4.1.2.
- Aplicación principal: `PortalApplication`; puerto HTTP `8090`, gestión `8091`.
- Paquetes modulares iniciales: `shared` y `config`, ambos abiertos según Modulith. No existen aún módulos de negocio.
- `shared` contiene roles/permisos, usuario autenticado, email, paginación, errores y SPIs para identidad, destinatarios, datos personales y navegación.
- Modulith, DAG de módulos y seis reglas ArchUnit protegen límites y capas.

## Configuración y persistencia

- Perfiles `dev`, `test` y `prod`; `PortalProperties` valida la configuración `portal.*`. No se activa un perfil por defecto.
- H2 y esquema generado se usan en las pruebas. Por defecto H2 está en scope `test`; el perfil optativo `local-h2` lo añade al classpath runtime y desactiva el perfil Maven `frontend-build`, sin incorporar H2 a perfiles de producción.
- Migraciones Flyway bajo `db/migration/<module>/`, con baseline `infra` y comprobaciones estáticas de nombres/convenciones. No se ejecutan contra SQL Server durante desarrollo.
- `docker-compose.dev.yml` declara SQL Server 2022, Mailpit y ClamAV; `DevComposeTest` valida estáticamente YAML, puertos y secretos. Los servicios no se han arrancado.

## Seguridad, observabilidad y calidad

- Spring Security con denegación por defecto, CSRF, cookies propias, cabeceras, HSTS y CSP con nonce.
- Logging JSON en producción; `CorrelationIdFilter` valida `X-Correlation-Id`; `PiiMasker` cubre correos, valores `token=` y trazas. El formatter de producción reemplaza las trazas de excepción con `***`.
- Actuator usa el puerto de gestión; readiness, exposición y rutas no permitidas se prueban mediante integración.
- El SPI `shared.spi.ErrorReporter` se implementa en `config.errortracking.ErrorInventoryService`. El inventario JSON se almacena fuera de webroot y SQL, se carga al arrancar y se reemplaza atómicamente. Su ruta, tamaño y capacidad son configurables; el despliegue actual presupone volumen privado single-writer.
- Backend/MVC y errores de persistencia se capturan con huella SHA-256, categoría, severidad, estado, marcas de tiempo, ocurrencias y contexto saneado. No se guarda el mensaje de la excepción, SQL crudo, query string ni body. El fallo del tracker se registra sin sustituir el error original.
- ERR-003 tiene intake en `POST /api/telemetry/errors` y un reporter local fingerprinted que no envía nada hasta consentimiento explícito. La banda ES/EN deniega por defecto, permite aceptar/rechazar y cambiar/retirar la elección desde el pie de página; la preferencia versionada se guarda solo en localStorage, sin identidad ni evidencia backend, y si falla el almacenamiento la captura permanece desactivada. El navegador solo envía tipos/nombres allowlisted, marcos de assets aprobados, locale, correlación y ruta reducida; el servidor fija `FRONTEND`, vuelve a validar/sanear y limita el body a 4 KiB. CSRF se mantiene activo, se requiere `Origin` same-origin y se aplica un token bucket en memoria de 30 solicitudes de ráfaga y 30/minuto; el cliente limita a cinco/minuto. No se captura el payload rechazado como un error BACKEND. Copy de consentimiento provisional, requiere revisión legal antes de producción.
- Verificación focal inicial del carril: compilación principal, 7 UT de error-tracking, 4 comprobaciones de arquitectura (11 tests Surefire en total) y 1 IT H2 pasaron con JDK 21.
- Validación posterior de PROMPT 04: `npm run build` pasó; `mvn -o -s settings-s-portal.xml -Plocal-h2 verify` pasó con JDK 21 offline: 76 tests Surefire y 3 Failsafe H2/integración, sin fallos ni errores, y gates JaCoCo aprobados. La suite completa detectó que Thymeleaf 3.1 deshabilita `#request`; se corrigió el layout exponiendo `portalCorrelationId` desde `WebViewAdvice`. Los 15 Playwright pasaron contra la instancia local actualizada, incluidos denegación por defecto, aceptación, rechazo/revocación y fallo de persistencia de consentimiento. La captura sobre SQL Server y el escalado multi-réplica no se verificaron; el texto de consentimiento sigue pendiente de revisión legal.
- CI definido en `.github/workflows/ci.yml`: Java 21, Maven `ci`, tests, JaCoCo, Dependency-Check y publicación de informes. Dependency-Check está configurado con umbral CVSS 7 y usa `NVD_API_KEY` si se ejecuta; su ejecución no es requisito de cierre de FND.
- Reglas de plantillas cubren colores directos, `th:utext` y nonce de scripts. `I18nKeyParityTest` valida pares de bundles `messages_es.properties`/`messages_en.properties` cuando existan.
- Última verificación local del perfil CI: 32 tests Surefire y 2 Failsafe, todos aprobados; comprobaciones JaCoCo aprobadas. No se declara resultado de Dependency-Check.

## Límites conocidos

- SQL Server 2022 es el destino productivo, no una plataforma validada; no hay gate MSSQL ni Testcontainers.
- La ejecución de Dependency-Check, la revisión de CVE/licencias y la ejecución remota de GitHub Actions no forman parte del criterio de cierre FND; no se afirma ningún resultado del análisis.
- No se han arrancado los servicios Compose.
- La suite H2 no certifica el dialecto ni la sintaxis de migraciones SQL Server. Flyway advirtió que la versión H2 probada (2.4.240) es posterior a la última versión declarada verificada por Flyway (2.3.232).
- No existen aún páginas de negocio/CMS ni módulos de negocio; el shell visual y el bundle ES/EN transversal de `config` están implementados y validados por MVC y E2E.
- ERR-001..003 están implementadas y validadas localmente con Java/H2, build frontend y Playwright; los prerrequisitos ERR-001, FND-009 y UIK-006 están satisfechos. ERR-004 (visor y resolución ADMIN) no forma parte de este alcance. SQL Server y el escalado operativo permanecen sin validar; la copia legal del consentimiento es provisional.

## UI kit y gobernanza (PROMPT 03)

- UI F0: layouts Thymeleaf `public`, `account` y `backoffice`; home shell ES/EN; errores 403/404/500; skip link, tema claro/oscuro con nonce, navegación basada en `NavContribution`, componentes neutrales y configuración HTMX.
- Locale público se resuelve desde `/{lang}`; `sp_lang` y `sp_theme` son preferencias técnicas. El tema se persiste en cookie/localStorage para usuarios anónimos y autenticados; `shared.spi.ThemePreference` queda como integración opcional futura con Identidad.
- Tailwind/HTMX/Alpine CSP se empaquetan localmente con manifest y fingerprint. El lockfile está sincronizado con `package.json`; `npm run build` limpio pasó y `FrontendAssetPipelineTest` pasó 5/5.
- La tipografía usa `system-ui, sans-serif`; no se incluyen fuentes web, `@font-face` ni hay bloqueo de licencia tipográfica. La autorización de assets de marca que se incorporen sigue sujeta a revisión.
- `npm run e2e` pasó 13/13 con JDK 21 y `local-h2`: matriz ES/EN, claro/oscuro, móvil/escritorio, axe sin violaciones, capturas en `visual/`, persistencia de tema, cambio de idioma, overflow/drawer/foco y HTMX con CSRF/errores accesibles. La matriz comprueba que HTMX no inserta bloques `<style>` sin nonce.
- `npm run build` pasó con las dependencias ya instaladas. `mvn clean verify -Plocal-h2`, con JDK 21 y Maven offline, terminó en BUILD SUCCESS: 73 tests, 0 fallos/errores y JaCoCo aprobado. `ProdProfileTest` quedó corregido con valores sintéticos de error-tracking. El perfil Maven `frontend-build` no se ejecutó localmente; no es un gate del alcance actual de PROMPT 03.
- Normas P01–P12, instrucciones Copilot, skill S-portal, mapa de rutas y plantilla ADR añadidos. Los validadores GOV-001..006 pasan. UIK-002..009 tienen evidencia MVC/Playwright; el pipeline local existente y su perfil Maven de CI se conservan fuera del alcance obligatorio de PROMPT 03.

## Estado de módulos y rutas

| Elemento | Owner | Estado observable |
|---|---|---|
| `shared` | Kernel/SPIs | Implementado; incluye `CurrentUser`, `NavContribution` y el puerto `ThemePreference` |
| `config` | Composición web, seguridad, locale, tema y navegación | Implementación base añadida; build frontend, `mvn clean verify -Plocal-h2` y Playwright/axe 13/13 pasan con JDK 21 y H2 |
| Sitio público `/es`, `/en` | `config` shell; contenido futuro `content` | Solo shell técnico, sin contenido funcional/CMS |
| `/cuenta/**`, `/gestion/**` | Módulos de negocio futuros | Denegación por defecto; no habilitadas |
| Assets frontend compilados | Build/config | CSS/JS y manifest fingerprintados; `npm run build` y `FrontendAssetPipelineTest` pasan; el perfil Maven frontend no se ejecutó localmente y no es gate del alcance actual |
| Fuentes web | `config` | No incluidas; se usa `system-ui, sans-serif`, sin decisión de licencia pendiente |
