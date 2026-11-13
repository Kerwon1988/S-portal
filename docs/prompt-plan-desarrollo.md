# Prompt: plan maestro de desarrollo de S-portal (monolito modular Thymeleaf)



---

## 1. Rol y objetivo

Actúa como **arquitecto de software principal y tech lead** experto en Java 21, Spring Boot, Spring Security, Spring Modulith, Thymeleaf, HTMX, Alpine.js, Tailwind CSS, SQL Server, seguridad OWASP, RGPD/LOPDGDD/LSSI-CE, accesibilidad WCAG 2.2, SEO técnico y monolitos modulares.

Genera un **plan de desarrollo completo, verificable y ejecutable mediante tareas atómicas** para **S-portal**, un monolito modular server-side (Thymeleaf), nuevo e independiente, que:

1. Promociona el negocio con una web corporativa bilingüe gestionada con un CMS ligero.
2. Da acceso a **SimbioSphere** (SaaS multitenant existente, proyecto DEMO-orders-thymeleaf) y permite el **alta conjunta** portal + SimbioSphere.
3. Gestiona usuarios clientes y trabajadores.
4. Recoge y gestiona solicitudes de desarrollo (sugerencias de módulos para SimbioSphere y desarrollos a medida).
5. Ofrece contacto y mensajería interna.
6. Se gobierna con una skill propia y gates automáticos de arquitectura modular, con README, inventario y diario siempre actualizados.

El resultado es **solo documentación de planificación**: plan, ADR en borrador y contratos en borrador. **No escribas código de producción** ni modifiques DEMO-orders-thymeleaf.

## 2. Reglas de trabajo del agente

1. Lee todas las fuentes de la sección 3 antes de escribir. Verifica en el código cada afirmación sobre SimbioSphere/DEMO y cítala como `ruta:línea`. Lo que no puedas verificar márcalo como **SUPUESTO**.
2. Las decisiones de la sección 5 están cerradas. No las reabras; si detectas un problema grave, documéntalo como riesgo con una alternativa.
3. Si encuentras dudas bloqueantes, agrúpalas en **una sola ronda de ≤ 8 preguntas** con una opción recomendada antes de cerrar el plan. Las no bloqueantes van a `13-preguntas-abiertas.md` con la opción por defecto que asumes.
4. Elige siempre la solución más simple que cumpla los requisitos y justifica cada módulo, dependencia y tecnología. No crees módulos vacíos "por si acaso": lo futuro se documenta, no se construye.
5. Distingue siempre **MVP** y **fases futuras**.
6. Escribe la documentación en español. Los identificadores de código, paquetes, tablas y claves i18n van en inglés.
7. Usa diagramas Mermaid: C4 (contexto, contenedores, componentes), ER, secuencia, estados y DAG de tareas.
8. Mantén la trazabilidad completa: cada punto de `docs/idea.md` → requisito(s) → tarea(s) → test(s).

## 3. Fuentes obligatorias

**S-portal (este repositorio)**
- `docs/idea.md`: idea original (puntos 0º–18º de ambas listas).

**SimbioSphere = DEMO-orders-thymeleaf**. Raíz: `C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\DEMO-orders-thymeleaf`
- `README.md`, `CHANGELOG.md`
- `docs/modules/NORMAS-DESARROLLO-MODULAR.md` (M01–M12), `docs/modules/INVENTARIO-MAESTRO.md`, `docs/arquitectura/constitution.md` (R01–R20)
- `.github/copilot-instructions.md`, `.github/skills/seniorThymeleaf/SKILL.md`
- `docs/decisiones-tecnicas/ADR-009-api-b2b-api-key-csrf-rate-limit.md`, `docs/decisiones-tecnicas/ADR-010-customer-account-onboarding-lifecycle.md`, `docs/integraciones/api-spec.yaml`
- `src/main/java/com/misnegocios/orders/`:
  - `iam/web/OwnerOnboardingController.java`, `iam/web/dto/OwnerOnboardingRequest.java`, `iam/web/RegisterController.java`
  - `shared/api/OwnerOnboardingOrchestratorApi.java`, `shared/api/OwnerOnboardingCommand.java`, `shared/api/OwnerOnboardingResult.java`, `shared/api/BusinessModule.java`
  - `platform/application/PlatformOwnerOnboardingService.java`, `iam/application/OwnerOnboardingService.java`, `iam/application/BusinessModuleSelectionValidator.java`
  - `iam/domain/User.java`, `iam/domain/CustomerAccount.java`, `iam/domain/CustomerAccountMembership.java`, `iam/domain/TenantMembership.java`, `tenant/domain/Tenant.java`, `shared/util/EmailHasher.java`
  - `config/SecurityConfig.java`, `config/I18nConfig.java`, `api/security/ApiKeyAuthenticationFilter.java`
- `webhook-service/src/main/java/com/misnegocios/orders/webhookservice/security/WebhookHmacAuthenticationFilter.java`
- `src/main/resources/templates/fragments/navbar.html`, `src/main/resources/templates/layouts/main.html`, `tailwind.config.js`, `package.json`
- `src/test/java/com/misnegocios/orders/architecture/ModuleArchitectureTest.java`

**Identidad visual**. Raíz: `C:\Users\x455035\1Git\AprendiendoIA\Symbio\symbio-frontend`
- `src/app/globals.css`, `tailwind.config.ts`, `public/resources/fonts/`, `public/resources/img/logo/`

## 4. Contexto de negocio

- **Empresa:** marca provisional **Simbio**. Confirmar razón social, marca y dominio, y parametrizar el nombre en configuración/i18n.
- **Propuesta de valor:** los empresarios dedican demasiado tiempo a la gestión. Simbio simplifica sus procesos para que el negocio rinda más con menos esfuerzo, lo que se traduce en más ingresos y más tranquilidad. En el futuro, agentes de IA automatizarán tareas simples; comunícalo como visión, sin prometer funcionalidades que aún no existen.
- **Slogan:** propón 5 candidatos ES/EN. Candidatos iniciales: "Menos gestión, más negocio." / "Less admin, more business."; "Simplifica hoy. Crece mañana." / "Simplify today. Grow tomorrow."; "Tecnología que trabaja por ti." / "Technology that works for you."
- **Productos:**
  - **SimbioSphere** (escritura exacta): SaaS multitenant existente para gestionar pedidos de negocios, sobre todo de comida a domicilio, y talleres de reparación. Tipos: `RESTAURANT`, `RETAIL`, `SERVICES`, `REPAIR_SHOP`. El plan inicial `DEMO` es gratuito y no existe cobro SaaS por módulo o sede; según el README de DEMO, eso **no puede anunciarse como disponible**.
  - **Desarrollos a medida:** landing pages, webs corporativas, aplicaciones web y otros.
- **Actores:** visitante anónimo, cliente (titular de una cuenta cliente), trabajador (`EMPLOYEE`), administrador (`ADMIN`, que también es trabajador), sistema SimbioSphere (M2M) y, en el futuro, un agente de IA.

## 5. Decisiones cerradas

| ID | Decisión |
|---|---|
| D01 | Nombre del producto multitenant: **SimbioSphere**. Configurable mediante propiedades/i18n. |
| D02 | S-portal es un proyecto **nuevo e independiente**. No sustituye ni migra la web PHP `symbiosi-main`. |
| D03 | Integración en fase 1: **nueva API REST M2M en DEMO**, como adaptador fino sobre el contrato interno existente `OwnerOnboardingOrchestratorApi`, con **logins separados**. El SSO (OIDC) se evalúa en un ADR como fase futura. Prohibido hacer scraping del formulario `/onboarding` o de su token CSRF, y prohibido leer o escribir la BD de DEMO. |
| D04 | Alta en SimbioSphere desde el portal: **todos los tipos** de DEMO, con los **módulos por defecto que decida DEMO**. Selector de módulos en fase 2. |
| D05 | Modelo de cliente: **ClientAccount** (empresa, autónomo o particular) con **1 usuario titular (OWNER) en el MVP**. Modelo preparado para N usuarios con rol en la cuenta. |
| D06 | Visibilidad: el cliente ve **solo sus** solicitudes (estado, presupuesto y mensajes públicos); los empleados ven todas. Las notas internas nunca son visibles para el cliente. |
| D07 | Contacto y mensajería: **bandeja interna propia** como fuente de verdad, avisos por email y WhatsApp *click-to-chat* (`wa.me`). Respuesta por email entrante en fase 2; WhatsApp Business API e IA en fases futuras. |
| D08 | **CMS ligero bilingüe (ES/EN) desde el MVP** para la web pública: home, servicios, sobre nosotros/equipo, textos legales y SEO. |
| D09 | Base de datos: **SQL Server 2022**, como DEMO en producción. |
| D10 | Stack: Java 21 LTS y la última versión estable de Spring Boot con soporte OSS vigente al ejecutar el plan. DEMO usa 3.3.5, sin soporte OSS: verificar y justificar en un ADR. Completan el stack: Spring MVC, Thymeleaf, Thymeleaf Layout Dialect, thymeleaf-extras-springsecurity, HTMX, Alpine.js (build CSP), Tailwind CSS compilado en build (CLI, sin CDN), Spring Security, Spring Data JPA, Flyway, Spring Modulith, ArchUnit y Playwright. H2 es el único motor de pruebas; SQL Server 2022 es el destino productivo, sin compatibilidad validada. Un único desplegable y una única BD. |
| D11 | Documentación viva: `README.md` en la raíz, inventario de todo lo desarrollado y `DIARIO.md` en la raíz. |
| D12 | Todo desarrollo (feature, fix, refactor, plantilla o migración) se hace mediante una **skill propia de S-portal** que impone la arquitectura modular, respaldada por gates automáticos en tests/CI: la skill guía y el test obliga. |

## 6. Hechos verificados de SimbioSphere (re-verificar y citar)

- **Stack de DEMO:** Spring Boot 3.3.5, Java 21, Thymeleaf + Layout Dialect 3.3.0, HTMX, Alpine.js, Tailwind 3.4.17, Flyway, H2 en dev y SQL Server 2022 en prod, Redis/Spring Session en prod. Gobernanza modular con ArchUnit (`ModuleArchitectureTest` 8/8) y registros CSV, normas M01–M12, constitución R01–R20 y skill `seniorThymeleaf`.
- **No existe ninguna API REST de registro o alta.** Los controladores REST son `OrderApiController`, `InventoryApiController` y `RepairApiController` (bajo `/api/v1/**`, con `X-API-Key` limitada a tenant + sede, sin capacidad de crear cuentas), además de `ApiKeyDebugController` (solo dev), `BrandingCssController`, `RepairCatalogApiController` (sesión), `DataExportController` y `StripeWebhookController`.
- **Los endpoints de registro son MVC con formulario + CSRF, no APIs:**
  - `GET/POST /onboarding` (`OwnerOnboardingController`): alta de empresario. Responde `redirect:/login?registered=true` y no devuelve IDs.
  - `GET/POST /t/{tenantSlug}/auth/register` (`RegisterController`): alta de un **cliente final de un tenant** (cliente del restaurante o del taller). **No sirve** para dar de alta a un empresario.
  - `/platform/admin/tenants` (`PlatformAdminController`): requiere sesión con `ROLE_PLATFORM_ADMIN`.
- **Contrato interno reutilizable:** `shared.api.OwnerOnboardingOrchestratorApi#onboard(OwnerOnboardingCommand)` devuelve `OwnerOnboardingResult` con 6 IDs `Long`: usuario, CustomerAccount, tenant, TenantMembership, suscripción y sede inicial.
- **Campos de `OwnerOnboardingCommand`:** `email`, `password`, `firstName`, `lastName`, `accountName`, `businessName`, `businessSlug`, `businessType`, `modules` (`Set<BusinessModule>`), `locationName` y `locationSlug`.
- **Validaciones de `OwnerOnboardingRequest`:**
  - `email` válido y `password` de al menos 8 caracteres, sin máximo.
  - `firstName` y `lastName` de hasta 100 caracteres; `accountName` y `businessName` de hasta 200.
  - `businessSlug` cumple `^[a-z0-9-]{3,50}$` y es único global; `businessType` pertenece a {`RESTAURANT`, `RETAIL`, `SERVICES`, `REPAIR_SHOP`}.
  - `locationName` de hasta 200 caracteres y `locationSlug` cumple `^[a-z0-9-]{2,50}$`.
  - `acceptTerms` debe ser `true`, pero el aprovisionamiento **no lo persiste**.
- **Módulos:**
  - Seleccionables: `MULTI_LOCATION`, `CATALOG`, `CART`, `SALES_ORDERS`, `PAYMENTS`, `INVENTORY`, `WEBHOOKS`, `NOTIFICATIONS`, `RGPD`, `API_B2B` y `BRANDING`.
  - Fijos: `CONTENT`, `TENANT` e `IAM`, más `REPAIRS` en `REPAIR_SHOP`.
  - `CART`, `SALES_ORDERS`, `PAYMENTS` e `INVENTORY` requieren `CATALOG`; `INVENTORY` no aplica a `SERVICES` ni a `REPAIR_SHOP`.
  - **Una selección vacía es válida (cero módulos opcionales).**
- **Resultado del alta:** User (habilitado, email sin verificar) → CustomerAccount `ACTIVE` → CustomerAccountMembership `OWNER` → suscripción `DEMO` `ACTIVE` → Tenant → TenantMembership `ROLE_ADMIN` → BusinessLocation predeterminada.
- **"Propietario del tenant"** es el User con membership `OWNER` activa en la CustomerAccount del tenant. `Tenant` no tiene `ownerUserId` y una CustomerAccount puede agrupar varios tenants.
- **Identidad:** `User` es global. El email se cifra con AES-256-GCM y existe un `emailHash` único (SHA-256 sobre el email con trim y en minúsculas). Contraseñas con BCrypt de fuerza 12. Ninguna entidad tiene `externalId`.
- **Autenticación:** form login con sesión, sin OAuth2/OIDC/JWT. Por defecto hay que verificar el email para iniciar sesión. URLs de login: `/login` y `/t/{tenantSlug}/auth/login`.
- **Reintento del onboarding:** localiza al propietario existente **solo por email**, sin clave de idempotencia ni comprobación de contraseña. **No debe exponerse tal cual** en una API.
- **Patrones reutilizables:** existe el módulo transversal `idempotency`, y `webhook-service` autentica por HMAC (`X-Webhook-Key-Id`, `X-Webhook-Timestamp`, `X-Webhook-Signature`), un patrón válido para M2M.
- **Webhooks salientes:** solo de pedidos y stock. **No hay eventos de ciclo de vida de cuenta o tenant.**
- **UI de referencia:**
  - `fragments/navbar.html`: Alpine, breakpoint fijo `lg`, hamburguesa a la derecha y drawer con bloqueo de scroll.
  - Modo oscuro: Tailwind `darkMode: 'class'` + `localStorage.darkMode` + script anti-FOUC. Por defecto usa el tema claro e ignora la preferencia del sistema.
  - i18n: `CookieLocaleResolver("app-locale")`, `es_ES` por defecto, parámetro `?lang=` y bundles `i18n/messages*.properties`.
  - Un único layout: `layouts/main.html`.
- **Lecciones de DEMO que no deben repetirse:**
  - Dos árboles de migraciones (H2 y SQL Server), con SQL Server sin certificar.
  - Credencial SMTP literal en el perfil dev.
  - OpenAPI estático desincronizado del código.
  - Log de notificaciones con destinatarios en claro.
  - Nombre de producto inconsistente (`app.name=Orders`).

## 7. Requisitos funcionales que el plan debe detallar

Convierte cada punto en requisitos con ID (`RF-<ÁREA>-NN`), prioridad MoSCoW, fase y criterios de aceptación.

### 7.1 Interfaz transversal (idea 0º, 10º y 13º)
1. **Responsive y táctil:** diseño mobile-first para móvil, tablet y escritorio, con objetivos táctiles de al menos 44 px.
2. **Tema claro/oscuro en todas las páginas** (públicas, área de cliente, backoffice y, si procede, emails):
   - **Tokens:** variables CSS semánticas (`--color-bg`, `--color-surface`, `--color-text`, `--color-accent`, ...) con valores para claro y `.dark`. Tailwind `darkMode: 'class'` se mapea a esos tokens. Prohibido escribir colores directamente en las plantillas.
   - **Tema por defecto:** el del sistema (`prefers-color-scheme`); si no hay preferencia, oscuro (identidad de marca).
   - **Persistencia:** `localStorage` + cookie (para renderizar en servidor sin parpadeo) + perfil del usuario autenticado.
   - **Sin parpadeo:** script anti-FOUC inline en `<head>` con nonce CSP y `<meta name="color-scheme">`.
   - **Detalles nativos:** scrollbars e inputs adaptados a cada tema; logos e imágenes con variante para cada tema.
   - **Contraste:** nivel AA en ambos temas; el plan debe incluir una tabla de pares de colores con su ratio.
3. **Paleta de marca** (de `globals.css` y `tailwind.config.ts`):
   - Base: fondo `#232310`, texto `#f5f5e8`, primario lima `#bcc907` y oliva oscuro `#3e4202`.
   - Variantes: brillante `#d0df05`, acento `#93c200`, highlight `#d5e31e` y carbón `#373737`.
   - Superficies `#2a2a12` y `#333318`; track del scrollbar `#1a1a08`.
   - **Tema claro derivado:** fondo `#f5f5e8` o blanco, texto `#232310`, texto de acento y enlaces `#3e4202` (≈ 10:1 sobre blanco). El lima `#bcc907` se usa **solo como relleno con texto oscuro** (≈ 8,7:1) y **nunca como texto sobre fondo claro** (≈ 1,8:1, no cumple AA).
4. **Tipografía:** usar `system-ui, sans-serif` para marca, titulares y cuerpo. No incluir ni solicitar fuentes web externas.
5. **i18n ES/EN:**
   - **Textos:** todos en bundles organizados por módulo, sin literales en plantillas, con un test automático que compruebe que ES y EN tienen las mismas claves.
   - **ADR de URLs:** comparar el idioma en la URL para páginas públicas (`/es/...`, `/en/...`, con `hreflang` y `canonical`, recomendado para SEO) frente a la cookie. El área privada sigue la preferencia del usuario o la cookie.
   - **Detección:** la primera visita usa `Accept-Language`; el selector de idioma conserva la página actual.
   - **Formato:** emails en el idioma del destinatario; fechas, números y moneda según el locale; datos guardados en UTC y mostrados en `Europe/Madrid`.
   - **CMS:** contenido por idioma con una política de fallback definida.
6. **Menú superior** (mejora del `navbar.html` de DEMO):
   - **Por rol:** el plan debe definir el árbol de navegación completo para anónimo, cliente, empleado y admin.
   - **Escritorio:** todas las opciones aparecen en línea **si caben**; si no caben, **todas** se agrupan en el botón hamburguesa (3 barras) a la **derecha**.
   - **Desbordamiento:** se detecta con `ResizeObserver` tras `document.fonts.ready`, no solo con un breakpoint fijo, porque la anchura varía según idioma y rol. Sin JS se aplica un fallback CSS.
   - **Contenido:** selector de idioma, conmutador de tema, acceso/registro o menú de usuario y enlace "Acceder a SimbioSphere".
   - **Accesibilidad:** `aria-expanded`/`aria-controls`, cierre con Esc y clic fuera, *focus trap* en el drawer, bloqueo de scroll, foco visible, elemento activo resaltado y cabecera *sticky*.
7. **Layouts** (Layout Dialect): uno público, uno de área de cliente y uno de backoffice (empleados/admin), con la misma cabecera, tema e i18n.
   - Páginas de error 403/404/500 con la marca.
   - Footer con textos legales, contacto, idioma y redes.
8. **HTMX:** filtros, paginación y acciones parciales con mejora progresiva, token CSRF en las peticiones HTMX y mensajes flash/toasts accesibles.

### 7.2 Web pública (idea 1º, 9º y 12º)
1. **Home:**
   - **Hero:** imagen estática del logotipo como elemento LCP.
   - **Escritorio con puntero fino** (`(hover: hover) and (pointer: fine)`): al pasar el puntero o enfocar con teclado se reproduce un vídeo breve; al salir, se pausa y vuelve al logo.
   - **Móvil/táctil:** reproducción automática tras la carga (`muted`, `playsinline`, sin bloquear el LCP) con botón de pausa.
   - **Movimiento y datos:** respetar `prefers-reduced-motion` y el ahorro de datos (mostrar solo la imagen). Si falla el autoplay (p. ej. modo de ahorro de energía en iOS), se muestra el póster.
   - **Vídeo:** de 5 s como máximo (o con control de pausa, WCAG 2.2.2), sin audio, en MP4 H.264 + WebM, de unos 1,5 MB como máximo y con `preload="none"`/`metadata`. El vídeo y las variantes del logo los aporta el cliente (dependencia).
   - **Secciones:**
     - Propuesta de valor + slogan, y servicios.
     - Bloque SimbioSphere con CTA "Crear mi negocio en SimbioSphere" y CTA "Solicitar un desarrollo".
     - Visión de IA ("próximamente", sin promesas) y CTA final.
2. **Página SimbioSphere:** qué es y para quién (tipos de negocio).
   - Capacidades **realmente disponibles**, contrastadas con el README de DEMO, y plan gratuito provisional.
   - CTA de alta conjunta y de "Sugerir un módulo", y enlace de acceso.
3. **Página de desarrollo a medida:** tipos (landing, web corporativa, app web, otros), proceso de trabajo y CTA al formulario de solicitud.
4. **Sobre nosotros (CMS):** historia, equipo (foto, nombre, rol, bio y enlaces), experiencia y valores.
5. **Contacto:** ver 7.9.
6. **Textos legales (LSSI-CE/RGPD), versionados:**
   - Aviso legal, política de privacidad y política de cookies.
   - Términos y condiciones, incluida la gratuidad provisional y su posible cambio.
7. **SEO:**
   - Por página e idioma: `title`, `description` y OpenGraph.
   - JSON-LD `Organization`, `sitemap.xml` por idioma y `robots.txt`.
   - URLs limpias y redirecciones 301 cuando cambie un slug del CMS.
8. **Cookies:** banner solo si se usan cookies no esenciales; preferir una analítica sin cookies.

### 7.3 CMS ligero bilingüe (D08)
1. **Contenido editable en ES/EN:**
   - Páginas, secciones o bloques (hero, tarjetas, texto, imagen, CTA, FAQ) y miembros del equipo.
   - Documentos legales versionados, metadatos SEO y slugs por idioma.
2. **Ciclo de publicación:**
   - Estados borrador/publicado, vista previa, y fecha y autor de cada modificación.
   - Historial de versiones (al menos restaurar la anterior) en el MVP si cabe; si no, en fase 2.
3. **Editor y sanitización:** Markdown o rich text restringido, con **sanitización en servidor** mediante allowlist (p. ej. OWASP Java HTML Sanitizer) antes de renderizar con `th:utext`.
4. **Biblioteca de medios:**
   - Subida de imágenes validada (sin SVG, o con SVG saneado).
   - Redimensionado y variantes WebP.
   - `alt` obligatorio por idioma.
5. **Traducciones:** indicador de traducciones pendientes y política de fallback.
6. **Permisos y caché:** permiso `CONTENT_EDIT`, auditoría de publicaciones y caché del contenido publicado, invalidada al publicar.
7. **Contenido semilla** en ES/EN, cargado mediante migración o seeder.

### 7.4 Identidad y cuentas (idea 2º, 3º, 4º y 8º)
1. **Roles y permisos:**
   - Roles `ADMIN`, `EMPLOYEE` y `CLIENT`, con permisos granulares (`USERS_MANAGE`, `REQUESTS_VIEW_ALL`, `REQUESTS_ASSIGN`, `QUOTES_MANAGE`, `INBOX_MANAGE`, `CONTENT_EDIT`, ...).
   - Roles dentro de la cuenta cliente: `OWNER` y, en el futuro, `MEMBER`.
   - El plan debe incluir la matriz rol × acción.
2. **Registro de cliente (autoservicio):**
   - Datos personales: nombre, apellidos, email, teléfono E.164 (opcional, útil para WhatsApp), contraseña + confirmación e idioma preferido.
   - Datos de la cuenta: tipo de cliente (empresa, autónomo o particular), nombre comercial o razón social (si no es particular), NIF/CIF (opcional al registrarse, obligatorio al aceptar un presupuesto) y país.
   - Consentimientos: aceptación de términos y privacidad (versión y fecha) y consentimiento comercial separado y no premarcado.
   - Protección anti-bots.
   - Casilla "Crear también mi negocio en SimbioSphere", que despliega los campos de 7.5.
3. **Política de contraseñas:** compatible con DEMO (mínimo 8) pero más estricta en el portal: p. ej. mínimo 10–12 y máximo 64, por el límite de 72 bytes de BCrypt.
4. **Verificación de email:** token de un solo uso, guardado hasheado y con caducidad; reenvío limitado.
5. **Login y sesión:**
   - Mensajes genéricos (sin enumeración de usuarios), bloqueo progresivo, protección contra fijación de sesión y logout.
   - MFA TOTP para `ADMIN`; evaluar si también para `EMPLOYEE`.
   - Cookie de sesión con nombre propio (p. ej. `SPORTAL_SESSION`): en local, DEMO y el portal comparten `localhost` y las cookies no se aíslan por puerto.
6. **Contraseña:** recuperación y cambio. En fase 1, avisar de que la contraseña de SimbioSphere es independiente.
7. **Perfil:** datos, idioma, tema, preferencias de notificación, exportar mis datos y solicitar la baja (RGPD).
8. **Alta de trabajadores**, solo por `ADMIN` y mediante **invitación** (enlace con caducidad para definir la contraseña):
   - Datos: nombre, apellidos, email, teléfono, puesto, rol, especialidades (para la asignación), fecha de alta y estado.
   - Nunca contraseñas definidas por el admin ni enviadas por email.
9. **Alta de clientes por `ADMIN`** mediante invitación. Si se marca SimbioSphere, el aprovisionamiento espera a que el cliente acepte los términos al activar su cuenta.

### 7.5 Alta conjunta y vínculo con SimbioSphere (idea 2º y 11º)
1. **Campos adicionales**, con validaciones idénticas a DEMO (sección 6):
   - Negocio: nombre del negocio, nombre de la cuenta (por defecto, el nombre comercial) y tipo de negocio (los 4 tipos, con descripción traducida).
   - Slug del negocio: autogenerado y editable, con comprobación de disponibilidad.
   - Sede inicial: nombre y slug, con valores por defecto.
   - Aceptación de los términos de SimbioSphere, independiente de los del portal.
2. **Módulos:** el portal **no** replica las reglas de módulos y delega en DEMO los valores por defecto por tipo. Verificar qué significa enviar `modules` vacío frente a nulo: una selección vacía deja al negocio solo con los módulos fijos (p. ej. un restaurante sin catálogo ni pedidos).
3. **Saga de aprovisionamiento** (no es posible una transacción atómica entre dos sistemas):
   - **Transacción local:** crear usuario + cuenta cliente + `SimbioSphereProvisioning` (`PENDING`) + evento en el outbox (Event Publication Registry de Spring Modulith).
   - **Tras verificar el email:** un worker asíncrono llama a la API de DEMO con `Idempotency-Key` = id del aprovisionamiento, con reintentos y backoff exponencial.
   - **Éxito:** guardar los IDs devueltos (customerAccountId, ownerUserId, tenantId, tenantSlug, locationId), pasar a `PROVISIONED`, notificar y mostrar el enlace `/t/{tenantSlug}/auth/login`.
   - **Error de negocio** (email ya registrado, slug ocupado): `ACTION_REQUIRED`, con una UI para corregirlo.
   - **Error técnico persistente:** `FAILED`, alerta a `ADMIN` y reintento manual.
   - Nunca se llama a la API remota dentro de la transacción de BD.
4. **ADR sobre la contraseña en SimbioSphere**, comparando:
   - **A (recomendada):** no enviar contraseña. DEMO crea al propietario y devuelve o envía un enlace de activación de un solo uso para definirla; el portal redirige al usuario a ese enlace al terminar.
   - **B:** llamada síncrona durante el registro, enviando la contraseña en claro por TLS. Duplica credenciales que acabarán divergiendo y es incompatible con aprovisionar tras verificar el email, porque para entonces el portal solo guarda el hash.
   - **C:** importar el hash BCrypt. Acopla los algoritmos de ambos sistemas, y el hash es un dato sensible.
   - **Futuro:** SSO OIDC con un IdP compartido.
5. **Verificación de email:** evitar la doble verificación. DEMO acepta el `emailVerified` de un cliente M2M de confianza y lo audita.
6. **Vínculo de cuentas:**
   - Cardinalidad: ClientAccount 1 ↔ 0..1 CustomerAccount de DEMO; usuario titular 1 ↔ 0..1 User `OWNER`.
   - Una CustomerAccount puede tener varios tenants: no asumir uno solo.
   - Guardar la referencia externa en ambos sistemas.
   - **Nunca vincular solo porque coincida el email** (riesgo de *account takeover*). Vincular una cuenta existente exige probar su posesión (código enviado por DEMO o login en DEMO). El diseño va en el MVP; la implementación, en fase 2 si no cabe.
7. **Panel "Mi SimbioSphere"** en el área de cliente: estado, negocio(s), tipo, enlace de acceso y acciones de corrección o reintento.
8. **Sincronización del ciclo de vida** (suspensión, reactivación, transferencia, borrado): en fase 2, mediante webhooks firmados desde DEMO o consulta bajo demanda con caché.
9. **Capa anticorrupción:**
   - Cliente HTTP tipado con timeouts, circuit breaker y mapeo de errores RFC 7807.
   - Logs sin PII.
   - Tests de contrato con stubs WireMock generados desde el OpenAPI acordado.
10. **Alternativa temporal** si la API de DEMO no está lista: CTA que enlaza a `/onboarding` de DEMO, sin vínculo automático (debe documentarse como deuda técnica).

### 7.6 Gestión de usuarios en el backoffice (idea 5º)
1. **Listados** separados de clientes y trabajadores:
   - Búsqueda y filtros (estado, rol, verificado, vinculado a SimbioSphere, fechas).
   - Orden y paginación en servidor.
   - Exportación CSV (`ADMIN`).
2. **Ficha:**
   - Datos, estado, roles y cuenta cliente.
   - Vínculo con SimbioSphere y estado del aprovisionamiento.
   - Solicitudes, conversaciones y auditoría.
3. **Acciones:**
   - Editar, activar/desactivar y desbloquear.
   - Reenviar la invitación, forzar la recuperación de contraseña (enviando un enlace) y reintentar el aprovisionamiento.
   - Cambiar el rol: no se puede quitar el último `ADMIN` ni degradarse a uno mismo.
   - Baja RGPD por anonimización, conservando solicitudes y mensajes seudonimizados.
   - Todas las acciones piden confirmación y quedan auditadas (quién, qué, cuándo, antes/después).

### 7.7 Solicitudes del cliente (idea 6º, 3º y 4º)
1. **Tipos:** `SIMBIOSPHERE_MODULE` (sugerencia de módulo o funcionalidad) y `CUSTOM_DEVELOPMENT`. Las categorías son configurables y bilingües: `LANDING_PAGE`, `CORPORATE_WEBSITE`, `WEB_APP`, `ECOMMERCE`, `AUTOMATION_AI`, `INTEGRATION`, `MAINTENANCE`, `OTHER`...
2. **Puntos de entrada:** "Sugerir un módulo para SimbioSphere" y el botón "Solicitar desarrollo a medida", que abre un formulario sencillo.
3. **Campos:**
   - Comunes: título, descripción, objetivo de negocio, categoría, presupuesto orientativo (por rangos), fecha deseada o urgencia y adjuntos limitados.
   - URLs de referencia: el servidor no las descarga (riesgo de SSRF).
   - Para `SIMBIOSPHERE_MODULE`: negocio vinculado (si existe), tipo de negocio, módulo relacionado (lista de `BusinessModule`) e impacto esperado.
4. **Acceso:** requiere un cliente autenticado con email verificado. Desde la web pública se redirige a login o registro, conservando el borrador.
5. **Referencia legible:** p. ej. `SOL-2026-000123`.
6. **"Mis solicitudes":**
   - Listado con filtros.
   - Detalle con el estado (con etiquetas simplificadas para el cliente), historial público, conversación (solo mensajes públicos) y adjuntos.
   - Presupuesto: verlo y aceptarlo o rechazarlo, registrando versión, fecha y evidencia.
   - Acciones: aportar información, responder y cancelar si la solicitud no está en curso.
7. **Gratuidad provisional:** el texto sale del CMS. El modelo prevé el cobro futuro (importe, moneda, impuestos, facturable), pero no se implementan pagos.

### 7.8 Portal de empleados (idea 7º y desarrollo 5º–7º)
1. **Panel:** solicitudes nuevas, sin asignar, por estado, asignadas a mí y vencidas por SLA, y mensajes sin leer.
2. **Listado de todas las solicitudes:**
   - Filtros por tipo (SimbioSphere / independiente), categoría, estado, prioridad, asignado (yo / sin asignar / persona), cliente, fechas e importe.
   - Búsqueda de texto, orden, paginación y exportación CSV.
   - Vista kanban por estado en fase 2.
3. **Detalle:**
   - Datos del cliente (con vínculo a SimbioSphere), descripción, adjuntos y línea de tiempo auditada.
   - Asignaciones: 1..N trabajadores, con un responsable y colaboradores.
   - Prioridad y estimación interna (horas).
   - Presupuesto versionado: importe, moneda EUR, IVA, validez, notas y estado (`DRAFT`, `SENT`, `ACCEPTED`, `REJECTED`, `EXPIRED`).
   - Conversación con el cliente + **notas internas**, que nunca son visibles para el cliente (garantizado en consultas y tests).
   - Convertir un mensaje de contacto en solicitud.
4. **Máquinas de estado por tipo.** El plan debe formalizarlas con una tabla de transiciones (origen, destino, rol, precondiciones, efectos, visibilidad para el cliente):
   - `CUSTOM_DEVELOPMENT` (propuesta): RECIBIDA → EN_ANALISIS ↔ PENDIENTE_INFO_CLIENTE → PRESUPUESTO_ENVIADO → ACEPTADA o RECHAZADA → EN_DESARROLLO → EN_REVISION_CLIENTE → ENTREGADA → CERRADA. EN_PAUSA y CANCELADA son accesibles desde los estados permitidos.
   - `SIMBIOSPHERE_MODULE` (propuesta): RECIBIDA → EN_ESTUDIO → PLANIFICADA, DESCARTADA (con motivo) o DUPLICADA (enlazada a la original) → EN_DESARROLLO → PUBLICADA.
5. **Asignación y SLA:** notificación al asignar, vista de carga de trabajo por trabajador y SLA de primera respuesta configurable.

### 7.9 Contacto y bandeja interna (idea 8º y 14º, D07)
1. **Formulario público:**
   - Campos: nombre, email, teléfono opcional, motivo (información, SimbioSphere, presupuesto, soporte, otro) y mensaje.
   - Adjuntos con límites de número, tamaño y tipo.
   - Nota de audio:
     - Grabación con MediaRecorder, de 2 min como máximo, en webm/opus y mp4/aac (para Safari).
     - Previsualización antes de enviar y fallback a subir un fichero.
     - Requiere HTTPS y `Permissions-Policy: microphone=(self)`.
   - Consentimiento de privacidad.
   - Anti-abuso: honeypot, captcha y límite por IP y por email.
   - Acuse de recibo por email.
   - Usuario autenticado: datos precargados y mensaje vinculado a su cuenta.
2. **Botones de canal:** WhatsApp (`wa.me` con texto precargado y número configurable) y email (`mailto:`). Estos mensajes no entran en la bandeja.
3. **Contacto ≠ solicitud:** un mensaje de contacto no es una solicitud, pero un empleado puede convertirlo en una.
4. **Bandeja de los empleados:**
   - Conversaciones con filtros (no leídas, asignadas, categoría, estado `ABIERTA`, `PENDIENTE`, `CERRADA` o `ARCHIVADA`) y búsqueda.
   - Hilo cronológico; respuesta con adjuntos y plantillas de respuesta bilingües.
   - Marcar como leído/no leído, asignar, etiquetar, archivar y desarchivar.
   - Borrar: papelera + purga según la retención; el borrado definitivo es solo para `ADMIN`.
5. **Respuesta a contactos sin cuenta:**
   - **Fase 1:** email con la respuesta + enlace mágico seguro para continuar la conversación en el portal (token de al menos 128 bits, hasheado, con caducidad, limitado al hilo y revocable).
   - **Fase 2:** respuestas por email entrante (dirección `reply+token@` o webhook del proveedor).
6. **Un único módulo de mensajería** para el contacto y las conversaciones de solicitudes (contexto `CONTACT` o `REQUEST`).
7. **Actualización casi en tiempo real:** polling HTMX o SSE.
8. **Preparado para IA**, sin implementarla:
   - Modelo: `authorType` (`CLIENT`, `EMPLOYEE`, `SYSTEM`, `AI_AGENT`), borradores pendientes de aprobación humana, evento `MessageReceived` y puerto `AiReplyAssistant`.
   - ADR futuro:
     - RGPD: transferencias internacionales si se usa DeepSeek u otro proveedor fuera del EEE, contrato de encargado y minimización.
     - *Prompt injection* y humano en el bucle.

### 7.10 Notificaciones
1. **Emails transaccionales** con plantillas Thymeleaf bilingües y de marca, más versión en texto plano:
   - Cuenta: verificación, bienvenida, recuperación de contraseña e invitación.
   - SimbioSphere: aprovisionamiento con éxito, con acción requerida o fallido.
   - Solicitudes: nueva solicitud (a empleados), cambio de estado (al cliente) y presupuesto enviado, aceptado o rechazado.
   - Mensajería: nuevo mensaje (en ambos sentidos) y acuse de contacto.
   - Alertas a `ADMIN`.
2. **Notificaciones in-app** (campana con contador), en el MVP o en fase 2 según decida el plan.
3. **Envío y preferencias:** preferencias por usuario; envío asíncrono mediante outbox con reintentos; logs sin PII en claro.
4. **Entregabilidad:** SPF, DKIM y DMARC del dominio; Mailpit en local.

### 7.11 Documentación viva (idea 16º, 17º y 18º)
1. **`README.md` raíz:**
   - Propósito, arquitectura resumida (Mermaid) y tabla de módulos con enlaces.
   - Arranque local, variables de entorno, tests y convenciones.
   - Enlaces al resto de la documentación.
2. **Inventario** (`docs/INVENTARIO.md` o carpeta `docs/inventario/`):
   - Contenido:
     - Módulos (responsabilidad, owner, estado) y páginas/rutas (URL, plantilla, roles, i18n, tema).
     - Endpoints, entidades/tablas, eventos e integraciones.
     - Propiedades de configuración, jobs, plantillas de email y permisos.
     - Tests por módulo y brechas conocidas.
   - Estados: `PLANIFICADO`, `EN_CURSO`, `HECHO`, `BLOQUEADO`, `OBSOLETO`.
3. **`DIARIO.md` en la raíz:** entradas en orden cronológico inverso con fecha, ID de tarea, módulo, tipo (feat/fix/refactor/docs/chore), resumen, estado, evidencias (tests, commit) y decisiones/ADR. No duplica el inventario: el diario cuenta qué se hizo y cuándo; el inventario, qué existe hoy; y el roadmap, qué falta.
4. **ADR** en `docs/adr/ADR-NNN-titulo.md`.
5. **Ideal:**
   - Un test que compruebe que cada módulo detectado por Spring Modulith figura en el inventario.
   - Diagramas generados con el `Documenter` de Spring Modulith.

### 7.12 Skill y gobernanza modular (idea 15º, D12)
1. **Base:** adaptar de DEMO las normas M01–M12, la constitución, `copilot-instructions.md` y la skill `seniorThymeleaf`. La gobernanza con CSV/ArchUnit se sustituye por Spring Modulith (`ApplicationModules.verify()`, `@ApplicationModuleTest`, named interfaces) más reglas ArchUnit de capas.
2. **Entregables:** `.github/copilot-instructions.md`, `.github/skills/<nombre>/SKILL.md` y `docs/arquitectura/NORMAS-DESARROLLO-MODULAR.md`.
3. **Contenido de la skill:**
   - Cuándo usarla (en todo cambio) y lecturas obligatorias.
   - Análisis de impacto modular: owner, contratos, eventos, tablas, i18n, tema, roles y seguridad.
   - Flujo TDD.
   - Convenciones Thymeleaf/HTMX/Alpine/Tailwind: fragmentos, tokens de tema y claves i18n.
   - Checklist de seguridad.
   - Definition of Done: tests, gate modular y README/INVENTARIO/DIARIO actualizados.
   - Formato de commit y cuándo parar y preguntar.
4. **Gate en CI** que falle si se rompe un límite modular, faltan claves i18n o baja la cobertura del módulo.

## 8. Requisitos no funcionales

**Seguridad (OWASP Top 10)**
- **Control de acceso:**
  - Rol + propiedad del recurso comprobados en cada consulta, con tests IDOR por recurso.
  - CSRF en todas las mutaciones web.
- **Cabeceras:** CSP con nonce (build CSP de Alpine; `htmx.config.allowEval=false`), HSTS, `X-Content-Type-Options`, `frame-ancestors`, `Referrer-Policy` y `Permissions-Policy`.
- **Cookies:** `HttpOnly`, `Secure` y `SameSite=Lax`.
- **Credenciales y cifrado:**
  - Contraseñas con BCrypt/Argon2 mediante `DelegatingPasswordEncoder`; tokens guardados hasheados.
  - Cifrado en reposo de la PII sensible (ADR: cifrado por columna frente a TDE).
- **Subida de ficheros:**
  - Validación: allowlist de tipos verificada por *magic bytes* (Apache Tika) y límites de tamaño.
  - Almacenamiento: nombres aleatorios fuera del webroot y antivirus (ClamAV) con cuarentena.
  - Descarga autorizada con `Content-Disposition: attachment`.
  - Puerto de almacenamiento: disco local en el MVP y almacenamiento de objetos o Blob después.
- **Abuso:** rate limiting (Bucket4j) en login, registro, contacto y recuperación, y captcha respetuoso con la privacidad.
- **Trazabilidad:** auditoría de acciones administrativas y eventos de seguridad; logs estructurados con correlation ID y sin PII.
- **Secretos y dependencias:**
  - Secretos solo en variables de entorno o en un gestor de secretos, nunca en el repositorio ni en los perfiles.
  - Análisis de dependencias (OWASP Dependency-Check o Trivy) en CI.

**RGPD / LOPDGDD / LSSI-CE**
- **Textos y consentimientos:** textos legales versionados y registros de consentimiento (versión, fecha, origen).
- **Datos:** minimización y retención por tipo de dato (tabla en el plan).
- **Derechos:** acceso, rectificación, supresión (por anonimización), oposición, portabilidad y limitación, con exportación de datos.
- **Documentación y encargados:** registro de actividades de tratamiento y contratos de encargado (hosting, email, captcha y, en el futuro, IA).
- **Cookies:** solo técnicas, si es posible.

**Accesibilidad:** WCAG 2.2 AA:
- Navegación por teclado, foco visible, skip link y landmarks.
- Formularios con errores asociados.
- Contraste en ambos temas y movimiento reducido.
- axe-core en los tests E2E.

**Rendimiento:**
- **Objetivos:** Lighthouse ≥ 90 (móvil) en páginas públicas; LCP < 2,5 s, CLS < 0,1 e INP < 200 ms.
- **Assets:** con huella y caché larga, compresión, imágenes responsive (srcset, WebP/AVIF) y lazy loading.
- **Datos:** paginación en servidor e índices definidos.

**SQL Server**
- **Migraciones y entornos:**
  - Un único árbol de migraciones Flyway para SQL Server: no repetir la divergencia H2/SQL Server de DEMO.
  - Todas las pruebas automatizadas de persistencia usan H2 con esquema generado; no se ejecutan Docker, SQL Server ni Testcontainers como gates. H2 no valida la compatibilidad con producción ni ejecuta el árbol Flyway SQL Server.
  - Se mantiene un único árbol de migraciones Flyway dirigido a SQL Server, pero las migraciones, constraints y el comportamiento específico de ese motor no se verifican en los planes de desarrollo.
  - docker-compose describe SQL Server, Mailpit y ClamAV para entornos Docker habilitados; arrancarlo no es requisito de desarrollo.
- **Esquema:**
  - `NVARCHAR` (datos nacionalizados), fechas en UTC (`datetime2`/`datetimeoffset`) e intercalación definida.
  - **Un esquema SQL por módulo**, para reforzar el ownership de las tablas.
- **Claves:** ADR sobre `BIGINT IDENTITY` + identificador público (UUID) frente a UUIDv7. Evitar un UUID aleatorio como clave clúster (fragmentación) y no exponer IDs secuenciales en las URLs.
- **Licencias:** ADR sobre licencia y coste en producción (Express, con límite de 10 GB por BD, frente a Standard o Azure SQL).

**Observabilidad y operación**
- **Monitorización:** Actuator (health/readiness protegidos), métricas Prometheus, logs JSON y alertas (fallos de aprovisionamiento, cola de emails).
- **Datos:** documentar el procedimiento de backup/restauración y RPO/RTO; no se requiere una prueba de restauración SQL Server en los gates de desarrollo.
- **Entornos:** perfiles dev/test/prod, despliegue con Docker (a definir en un ADR) y puertos separados de DEMO. H2 es el único motor de pruebas automatizadas; Compose y SQL Server no son gates de desarrollo.

**Testing**
- **Por capa:**
  - Unitarios (dominio), slices `@WebMvcTest` con seguridad y `@ApplicationModuleTest` por módulo.
  - Pruebas de persistencia automatizadas con H2 en memoria y esquema generado.
  - No se requieren pruebas SQL Server/Flyway con Testcontainers para cerrar tareas. La compatibilidad SQL Server y la ejecución del árbol Flyway quedan sin verificar y se documentan como limitación, no como gate.
  - Contrato con DEMO: WireMock desde el OpenAPI en el portal + tests de proveedor en DEMO.
- **E2E con Playwright** de los recorridos críticos:
  - Registro + alta en SimbioSphere y creación de una solicitud.
  - Asignación y presupuesto, y contacto → respuesta.
  - Invitación de un trabajador y edición en el CMS.
- **Matriz visual** de las páginas clave: claro/oscuro × móvil/escritorio × ES/EN.
- **Transversales:** tests de autorización rol × endpoint y test de completitud de i18n.
- **Cobertura:** objetivo de al menos el 80 % en domain/application.

## 9. Arquitectura objetivo (propuesta que el plan debe validar)

- **Paquete base sugerido:** `com.symbio.portal` (confirmar). Cada módulo tiene `api` (contratos y eventos publicados), `application`, `domain`, `infrastructure` y `web`, con el mismo patrón que DEMO.
- **Módulos candidatos.** El plan debe validarlos, fusionarlos o dividirlos con justificación:
  - **Identidad y cuentas:**
    - `identity`: usuarios, autenticación, verificación, recuperación, invitaciones, MFA y perfil.
    - `customer`: cuenta cliente y sus miembros.
    - `staff`: perfil del trabajador y especialidades.
    - `legal`: documentos legales y consentimientos (o dentro de `content`/`customer`).
  - **Web y contenidos:**
    - `content`: CMS, medios, SEO y redirecciones.
    - `site`: controladores públicos, sitemap y robots (o dentro de `content`).
  - **Operación del negocio:**
    - `request`: solicitudes, categorías, estados, asignaciones y presupuestos.
    - `messaging`: conversaciones, mensajes, contacto y enlaces mágicos.
    - `notification`: emails, in-app, preferencias y outbox.
  - **Soporte e integración:**
    - `files`: almacenamiento, validación y antivirus.
    - `audit`.
    - `simbiosphere`: capa anticorrupción + saga.
    - `shared`: mínimo (value objects, tipos de error, paginación).
  - La configuración transversal (seguridad, web, i18n, tema) queda fuera de los módulos de negocio.
- **Comunicación:**
  - Síncrona, a través de `api`.
  - Asíncrona, con eventos de dominio y el Event Publication Registry (p. ej. `ClientRegistered` → `simbiosphere`; `RequestStatusChanged` y `MessagePosted` → `notification`).
  - Sin ciclos; cada tabla tiene un único owner.
- **Front:**
  - Plantillas por módulo (`templates/<modulo>/...`), fragmentos compartidos (`templates/fragments/`) y layouts (`templates/layouts/`).
  - Bundles i18n por módulo y Tailwind con los tokens de `globals.css`.
  - JS mínimo por componente (`Alpine.data`), con HTMX y Alpine servidos localmente.
- **Mapa de URLs propuesto:**
  - Público: `/{lang}/...`.
  - Área de cliente: `/cuenta/...`.
  - Backoffice: `/gestion/...`.

## 10. Backlog de cambios en DEMO (plan separado, ejecutado con las normas y la skill de DEMO)

- **DEMO-01** ADR "API de aprovisionamiento para partners": autenticación, scopes, idempotencia, errores y versionado.
- **DEMO-02** Autenticación M2M de plataforma, distinta de la API key de tenant:
  - Mecanismo: HMAC como en `webhook-service` (key id, timestamp, firma, ventana anti-replay) u OAuth2 client credentials.
  - Scopes `provisioning:write` y `provisioning:read`.
  - Cadena de filtros propia para `/api/platform/v1/**`, con exención de CSRF solo en esa cadena.
  - Rate limit y allowlist de IP opcional.
- **DEMO-03** `POST /api/platform/v1/owner-onboardings` (JSON) → `OwnerOnboardingOrchestratorApi`:
  - `Idempotency-Key` con el módulo `idempotency`: misma clave con payload distinto → conflicto.
  - Respuesta 201 con los IDs.
  - Errores RFC 7807: 409 `EMAIL_ALREADY_REGISTERED`, 409 `SLUG_TAKEN`, 422 `INVALID_MODULES` y 400 de validación.
- **DEMO-04** `GET /api/platform/v1/tenant-slugs/{slug}/availability`, con sugerencias.
- **DEMO-05** Referencia externa (`source=S_PORTAL`, `externalAccountId`) en una tabla nueva con owner registrado, con migraciones en los dos árboles de DEMO (H2 y SQL Server).
- **DEMO-06** Enlace de activación o definición de contraseña para los propietarios creados por API, y aceptación de `emailVerified` de un cliente de confianza.
- **DEMO-07** Persistir la aceptación de términos (versión, fecha), que hoy no se guarda.
- **DEMO-08** Módulos por defecto según el tipo cuando el portal no envía selección.
- **DEMO-09** (fase 2) Vinculación de cuentas existentes con prueba de posesión, webhooks de ciclo de vida y `GET` del estado de la cuenta.
- **DEMO-10** Registrar contratos (`module-api-contracts.csv`) y owners de entidades, pasar el gate ArchUnit 8/8 y actualizar INVENTARIO-MAESTRO, README, CHANGELOG y `api-spec.yaml`.
- **DEMO-11** Tests de proveedor del contrato y tests de aislamiento y de fallo (M09).

## 11. Riesgos y trampas conocidas (incluir en el registro con mitigación)

1. **El alta conjunta depende de cambios en DEMO** (camino crítico) → pista paralela, contrato OpenAPI primero, stubs WireMock y alternativa temporal (7.5.10).
2. **Credenciales duplicadas que divergen y doble verificación de email** → ADR de identidad; SSO en el futuro.
3. **Account takeover** si se vincula por email → prueba de posesión.
4. **Alta conjunta a medias** → saga, idempotencia, outbox y estados visibles.
5. **Negocio aprovisionado sin módulos útiles** (selección vacía) → valores por defecto en DEMO.
6. **Contraste insuficiente del lima en tema claro** → tokens y tests de contraste.
7. **Vídeo del hero:** autoplay bloqueado, peor LCP, consumo de datos móviles y movimiento no deseado → póster, `prefers-reduced-motion` y tamaño limitado.
8. **Menú que desborda según idioma o rol** → detección de desbordamiento y tests en ES/EN para cada rol.
9. **Spam o malware** en adjuntos y audio de anónimos → captcha, límites, antivirus y retención.
10. **XSS a través del CMS** → sanitización con allowlist.
11. **CSP incompatible con Alpine/HTMX estándar** → build CSP de Alpine y `allowEval=false`.
12. **Cookies de sesión compartidas en `localhost`** entre DEMO y el portal → nombre de cookie propio.
13. **SQL Server:** divergencia entre H2 y el dialecto productivo o coste de licencia → un único árbol de migraciones, compatibilidad no verificada explícitamente y ADR de licencia.
14. **Anunciar capacidades de SimbioSphere que no existen** → revisar los textos contra el README de DEMO.
15. **IA con datos de clientes** (transferencias internacionales, *prompt injection*) → ADR futuro y humano en el bucle.
16. **Fuentes tipográficas web** → fuera del alcance; usar la pila del sistema, sin binarios ni CDN.

## 12. Fuera del alcance del MVP (documentar como fases futuras)

- **Identidad y SimbioSphere:** SSO OIDC y selector de módulos.
- **Canales e IA:** email entrante, WhatsApp Business API y agentes de IA.
- **Negocio:** cobro y facturación (Stripe).
- **Producto y UX:** votación pública de sugerencias y roadmap público, kanban, historial completo del CMS (si no entra en el MVP), PWA y más idiomas.

## 13. Entregables del plan

Crea los entregables en `docs/plan/` de S-portal:

| Fichero | Contenido mínimo |
|---|---|
| `00-INDICE.md` | Índice, resumen ejecutivo y **matriz de trazabilidad** idea → RF → tareas → tests. |
| `01-vision-alcance.md` | Visión, objetivos medibles, actores, alcance del MVP y de fases futuras, supuestos. |
| `02-requisitos.md` | RF y RNF con ID, prioridad MoSCoW, fase y criterios de aceptación Given/When/Then. |
| `03-arquitectura.md` | C4; catálogo de módulos (responsabilidad, API publicada, eventos, tablas, dependencias); grafo de dependencias; capas, paquetes y convenciones; arquitectura front. |
| `04-modelo-datos.md` | ER por módulo; tablas y columnas con tipos SQL Server; restricciones, índices y esquemas; plan de migraciones. |
| `05-integracion-simbiosphere.md` | Hechos verificados con `ruta:línea`; borrador OpenAPI; secuencias (alta conjunta, fallos, reintentos, vinculación); seguridad M2M; backlog DEMO. |
| `06-ux-ui.md` | Mapa del sitio e inventario de páginas por rol; wireframes descritos; menú por rol y comportamiento responsive; tokens de tema con ratios de contraste; escala tipográfica y componentes; estrategia i18n; especificación del hero con vídeo; accesibilidad. |
| `07-seguridad-rgpd.md` | Modelo de amenazas STRIDE de los flujos críticos; matriz de autorización; mapa OWASP; checklist RGPD/LSSI; tabla de retención. |
| `08-calidad-testing.md` | Pirámide de tests, herramientas, gates, Definition of Done y pipeline de CI. |
| `09-operacion-despliegue.md` | Entornos, Docker, SQL Server, backups, monitorización, secretos y releases. |
| `10-roadmap-backlog.md` | Fases, hitos, épicas y tareas (formato de la sección 14); DAG Mermaid; camino crítico; pistas paralelas (portal y DEMO); estimaciones. |
| `11-riesgos.md` | Registro de riesgos: probabilidad, impacto, mitigación y disparador. |
| `12-gobernanza-modular.md` | Borrador de normas, esquema de la skill y de `copilot-instructions.md`, plantillas de README, INVENTARIO y DIARIO. |
| `13-preguntas-abiertas.md` | Preguntas con la opción por defecto asumida. |
| `docs/adr/ADR-NNN-*.md` | ADR en borrador; ver la lista mínima bajo esta tabla. |

**ADR mínimos:**
- Versión de Spring Boot y gobernanza modular con Spring Modulith.
- Identidad y contraseña en SimbioSphere, y autenticación M2M.
- i18n en la URL.
- Estrategia de IDs en SQL Server.
- Almacenamiento de ficheros.
- Despliegue y licencia de SQL Server.
- Email transaccional.
- Editor y sanitización del CMS.

**Orden de fases sugerido** (puedes reordenarlo si lo justificas):
- **Fase 0, fundaciones:**
  - Esqueleto modular + gates, CI y docker con SQL Server.
  - Layouts, tema, i18n y menú.
  - Skill, README, INVENTARIO y DIARIO.
- **Fase 1:** web pública, CMS, textos legales y SEO.
- **Fase 2:** identidad, gestión de usuarios e invitaciones.
- **Fase 3:** solicitudes y portal de empleados.
- **Fase 4:** contacto, bandeja y notificaciones.
- **Fase 5:** alta conjunta en SimbioSphere, con la pista DEMO en paralelo desde la fase 2.
- **Fase 6:** endurecimiento y salida a producción.

**Preguntas abiertas mínimas que debes recoger:**
- **Empresa y dominios:**
  - Razón social, NIF, dirección, marca y dominio(s).
  - URLs de SimbioSphere por entorno.
- **Proveedores y despliegue:**
  - Número de WhatsApp de la empresa y proveedor de email transaccional.
  - Plataforma de despliegue y edición/licencia de SQL Server.
  - Proveedor de captcha.
- **Material gráfico:** vídeo del hero, variantes del logo para tema claro, y fotos y bios del equipo.
- **Seguridad:** opción de contraseña en SimbioSphere (A/B/C) y MFA para `EMPLOYEE` en el MVP.
- **Operación:** plazos de retención y SLA de primera respuesta.

## 14. Formato obligatorio de cada tarea

```markdown
### <ID> — <título>
- Fase / épica / módulo owner:
- Tipo: feat | fix | refactor | docs | infra | test
- Depende de:
- Descripción:
- Ficheros/paquetes a crear o modificar (aproximado):
- Impacto modular: contratos publicados/consumidos, eventos, tablas (owner), riesgo de ciclo
- UI: páginas, claves i18n, tema claro/oscuro, móvil/escritorio, accesibilidad
- Seguridad: roles/permisos, CSRF, validaciones, datos personales
- Criterios de aceptación (Given/When/Then):
- Tests: unitario / módulo / integración / E2E / accesibilidad / visual
- Documentación a actualizar: README / INVENTARIO / DIARIO / ADR
- Estimación: S (≤ 0,5 d) | M (≤ 1 d); divide lo que sea mayor
- Riesgos:
```

Cada tarea equivale a un commit atómico y ninguna supera un día de trabajo.

## 15. Autoverificación antes de entregar

- [ ] Todos los puntos de `docs/idea.md` (0º–18º, en ambas listas) están trazados a requisitos, tareas y tests.
- [ ] Se respetan las decisiones D01–D12.
- [ ] Cada afirmación sobre DEMO está citada como `ruta:línea` o marcada como SUPUESTO.
- [ ] Cada módulo tiene responsabilidad, tablas, API publicada, eventos y dependencias, y el grafo no tiene ciclos.
- [ ] Cada endpoint y cada página definen roles, CSRF, i18n, tema claro/oscuro y comportamiento en móvil/escritorio.
- [ ] Cada tarea tiene criterios de aceptación, tests y la documentación que debe actualizar.
- [ ] Las máquinas de estado y la matriz de autorización están completas.
- [ ] Los riesgos tienen mitigación y las preguntas abiertas tienen opción por defecto.
- [ ] El MVP está bien delimitado y no anuncia funcionalidades que no existen.
- [ ] No se ha escrito ninguna línea de código de producción.
