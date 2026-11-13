# F1 — Web pública, SEO y contrato SimbioSphere: tareas

- Objetivo: publicar páginas comerciales bilingües, legales y SEO desde CMS, y acordar temprano el contrato portal↔DEMO (hito M1; ver [roadmap](../10-roadmap-backlog.md)).
- Módulo(s) owner: `content` y `simbiosphere` para el contrato; DEMO se planifica en su pista separada.

### WEB-001 — Home: secciones y hero con imagen LCP
- Fase / épica / módulo owner: F1 / WEB / content
- Tipo: feat
- Depende de: CMS-004, CMS-012
- Descripción: Cubre: RF-WEB-01, RF-WEB-02; compone home pública desde bloques CMS con imagen estática del logo como LCP.
- Ficheros/paquetes a crear o modificar (aproximado): `content.web.HomeController`, `templates/content/home.html`, fragmentos hero/CTA y bundles ES/EN.
- Impacto modular: lee `content.api` publicado; no crea tablas ni eventos. Ciclo: ninguno (no accede directo a repos de otros módulos).
- UI: `/es` y `/en`; propuesta de valor, servicios, CTAs, visión de IA, responsive y tema.
- Seguridad: contenido solo PUBLICADO y saneado; links allowlist y CSRF en formularios de destino.
- Criterios de aceptación (Given/When/Then):
  - Given CMS publicado en ES/EN When se abre home Then aparecen propuesta, slogan, servicios y CTAs traducidos.
  - Given carga en móvil When se mide LCP Then logo estático reserva dimensiones y es el elemento LCP; no espera vídeo.
- Tests: TC-WEB-001: WT (render por locale), E2E/VIS (home y CTA), PERF (LCP).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Sitio público»
- Estimación: M (≤ 1 d)
- Riesgos: fuente provisional de logos indicada en 13; verificar autorización/licencia de cada asset y mantener fallback marcado pendiente si no está aprobado.

### WEB-002 — Hero con vídeo (hover/foco, autoplay móvil, reduced-motion, save-data)
- Fase / épica / módulo owner: F1 / WEB / content
- Tipo: feat
- Depende de: WEB-001
- Descripción: Cubre: RF-WEB-01; añade vídeo breve de marca sin desplazar el logo LCP y con política según dispositivo/preferencias.
- Ficheros/paquetes a crear o modificar (aproximado): fragmento `templates/content/fragments/hero-video.html`, JS estático y assets aprobados desde la fuente indicada en 13.
- Impacto modular: solo presentación `content`; sin APIs, eventos ni tablas. Ciclo: ninguno.
- UI: escritorio hover/foco; móvil muted/playsinline con pausa; póster, controles accesibles y fallback.
- Seguridad: MP4/WebM locales, sin audio ni CDN; licencia del recurso y CSP revisadas.
- Criterios de aceptación (Given/When/Then):
  - Given puntero fino o foco de teclado When entra en hero Then reproduce y al salir pausa/restaura logo.
  - Given `prefers-reduced-motion`, `Save-Data` o autoplay denegado When carga móvil Then mantiene póster y ofrece pausa sin bloquear LCP.
- Tests: TC-WEB-002: E2E (hover/foco/autoplay/fallback), A11Y (controles), PERF (preload y bytes).
- Documentación a actualizar: DIARIO + INVENTARIO; 13 materiales pendientes
- Estimación: M (≤ 1 d)
- Riesgos: video lo entrega el cliente (≤5 s, ~1,5 MB); si no llega, hero estático sigue siendo válido.

### WEB-003 — Página SimbioSphere (solo capacidades reales)
- Fase / épica / módulo owner: F1 / WEB / content
- Tipo: feat
- Depende de: CMS-004, CMS-012
- Descripción: Cubre: RF-WEB-03; publica explicación, tipos y capacidades solo con fuente verificable del README de DEMO.
- Ficheros/paquetes a crear o modificar (aproximado): página CMS `/simbiosphere`, bloques de capacidades/CTA y enlaces a alta/acceso.
- Impacto modular: contenido pertenece a `content`; futura alta enlaza a `simbiosphere` por ruta, sin consulta directa. Sin tablas/eventos. Ciclo: ninguno.
- UI: página ES/EN, CTAs «Crear mi negocio»/«Sugerir un módulo», enlace de acceso y tipos traducidos.
- Seguridad: no afirmar capacidades, precio ni módulos no confirmados; no exponer datos del tenant.
- Criterios de aceptación (Given/When/Then):
  - Given texto publicado When revisión coteja tipos/capacidades Then cada hecho se alinea con DEMO `README.md:11-14,58-66`.
  - Given plan gratuito DEMO When se redacta Then no anuncia cobro por módulo/sede ni selector futuro como disponible.
- Tests: TC-WEB-003: WT (contenido y locale), revisión de evidencia (README rutas citadas).
- Documentación a actualizar: DIARIO + INVENTARIO; 05-integracion
- Estimación: M (≤ 1 d)
- Riesgos: README de DEMO cambia; revalidar evidencia antes de cada publicación.

### WEB-004 — Página de desarrollo a medida
- Fase / épica / módulo owner: F1 / WEB / content
- Tipo: feat
- Depende de: CMS-004, CMS-012
- Descripción: Cubre: RF-WEB-04; presenta categorías y proceso de trabajo con enlace al formulario de solicitud.
- Ficheros/paquetes a crear o modificar (aproximado): página CMS `/servicios/desarrollo`, bloques y claves i18n por locale.
- Impacto modular: `content` renderiza contenido; formulario se resuelve en `request` por URL pública, sin dependencia Java. Sin tablas/eventos.
- UI: landing, web corporativa, aplicación web y otros; CTA a iniciar solicitud, móvil/desktop.
- Seguridad: no prometer precio/plazo; escape/sanitización CMS y enlace interno validado.
- Criterios de aceptación (Given/When/Then):
  - Given locale ES o EN When se visita Then se enumeran tipos, proceso y CTA con destino correcto.
  - Given contenido CMS no publicado When se visita URL Then responde 404 sin mostrar borrador.
- Tests: TC-WEB-004: WT (locale/CTA/404), E2E (navegación a formulario).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Servicios»
- Estimación: S (≤ 0,5 d)
- Riesgos: categorías comerciales deben ser editables y no codificadas en varias vistas.

### WEB-005 — Sobre nosotros
- Fase / épica / módulo owner: F1 / WEB / content
- Tipo: feat
- Depende de: CMS-004, CMS-009
- Descripción: Cubre: RF-WEB-05; renderiza historia, valores y miembros publicados desde CMS.
- Ficheros/paquetes a crear o modificar (aproximado): página `content.web.AboutController`, plantilla `templates/content/about.html`, bundles.
- Impacto modular: consulta `content.api` del mismo owner; fotos por `files.api`; sin tablas ni eventos nuevos. Ciclo: ninguno.
- UI: `/es/sobre-nosotros` y EN equivalente; retratos responsive, bios, roles y enlaces accesibles.
- Seguridad: mostrar solo miembros PUBLICADOS; escapar bio y validar URL externa.
- Criterios de aceptación (Given/When/Then):
  - Given miembros activos When se visita página Then aparecen con alt por locale y enlaces con nombre accesible.
  - Given lista vacía When se visita Then oculta sección de equipo sin inventar personas ni romper layout.
- Tests: TC-WEB-005: WT (publicado/vacío), A11Y (alt/enlaces), VIS (mobile/desktop).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Sobre nosotros»
- Estimación: S (≤ 0,5 d)
- Riesgos: fotos/bios requieren autorización y materiales confirmados.

### WEB-006 — Páginas legales, footer y política de cookies con banner condicional
- Fase / épica / módulo owner: F1 / WEB / content
- Tipo: feat
- Depende de: CMS-010, UIK-006
- Descripción: Cubre: RF-WEB-06, RF-WEB-08, RNF-PRV-05; publica legales versionados y muestra banner solo si hay cookies no esenciales.
- Ficheros/paquetes a crear o modificar (aproximado): rutas CMS legales, `templates/fragments/footer.html`, `CookieNotice`, bundles.
- Impacto modular: lee documentos `content.api`; consentimiento legal se registra en `legal` vía contrato cuando aplica. Sin dependencia directa de tablas.
- UI: enlaces legales/contacto/idioma/redes en footer; banner claro y operable por teclado.
- Seguridad: solo cookies técnicas por defecto; bloquear analítica/cookies no esenciales antes de consentimiento.
- Criterios de aceptación (Given/When/Then):
  - Given solo `sp_theme`/`sp_lang` When visita el sitio Then no aparece banner ni se activa cookie analítica.
  - Given cookie no esencial configurada When aún no hay consentimiento Then permanece bloqueada hasta elección explícita.
- Tests: TC-WEB-006: WT (banner condicional/rutas), A11Y (consentimiento), SEC (no tracking previo).
- Documentación a actualizar: DIARIO + INVENTARIO; 07 privacidad
- Estimación: M (≤ 1 d)
- Riesgos: texto legal pendiente de validación; mantener borrador no publicado.

### WEB-007 — Rendimiento de páginas públicas: presupuesto Lighthouse CI, imágenes responsive y caché
- Fase / épica / módulo owner: F1 / WEB / content
- Tipo: test
- Depende de: WEB-002, SEO-002
- Descripción: Cubre: RNF-PERF-01, RNF-PERF-02; fija presupuesto Lighthouse CI y aplica optimización de imágenes, caché y carga diferida.
- Ficheros/paquetes a crear o modificar (aproximado): workflow Lighthouse, `srcset`/WebP/AVIF, headers/cache policy y reporte de páginas públicas.
- Impacto modular: medición vía HTTP y assets `content`; sin contratos/eventos/tablas. Ciclo: ninguno.
- UI: home, producto, servicios y legales en móvil; preservar layout y tema.
- Seguridad: cachear solo contenido público; preview/autenticación usa `no-store`; no cachear PII.
- Criterios de aceptación (Given/When/Then):
  - Given páginas ES/EN en viewport móvil When corre Lighthouse CI Then cada una alcanza ≥90, LCP <2,5 s, CLS <0,1 e INP <200 ms.
  - Given respuesta autenticada o preview When se inspecciona caché Then no hay `public` ni contenido personal compartido.
- Tests: TC-WEB-007: PERF (Lighthouse/CWV), IT (headers/caché), VIS (capturas responsive).
- Documentación a actualizar: DIARIO + INVENTARIO; 08 calidad/performance
- Estimación: M (≤ 1 d)
- Riesgos: umbral depende de material final/red de CI; versionar baseline y tolerancia.

### SEO-001 — Metadatos por página/idioma, canonical, hreflang y JSON-LD
- Fase / épica / módulo owner: F1 / SEO / content
- Tipo: feat
- Depende de: CMS-004
- Descripción: Cubre: RF-WEB-07; genera metadatos por página/locale y datos estructurados `Organization` desde propiedades confirmadas.
- Ficheros/paquetes a crear o modificar (aproximado): `content.application.SeoMetadata`, fragmento `templates/fragments/seo.html`, tests y bundles.
- Impacto modular: usa `content.api` publicado en el mismo módulo; sin tablas/eventos. Ciclo: ninguno.
- UI: `<title>`, description, OpenGraph, canonical y hreflang para ES/EN; slugs por idioma.
- Seguridad: escapar atributos/JSON-LD y excluir borradores, tokens y datos personales.
- Criterios de aceptación (Given/When/Then):
  - Given página publicada ES con equivalente EN When se renderiza Then canonical es propio y hreflang apunta a ambas URLs válidas.
  - Given entidad Organization sin razón social/dominio confirmados When renderiza Then no inventa datos ni publica placeholders como hechos.
- Tests: TC-SEO-001: UT (canonical/alternates), WT (HTML/JSON-LD), SEC (escape/sin borrador).
- Documentación a actualizar: DIARIO + INVENTARIO; 13 datos de marca pendientes
- Estimación: M (≤ 1 d)
- Riesgos: información corporativa incompleta; configurar solo valores aprobados.

### SEO-002 — sitemap.xml por idioma y robots.txt
- Fase / épica / módulo owner: F1 / SEO / content
- Tipo: feat
- Depende de: SEO-001
- Descripción: Cubre: RF-WEB-07; expone sitemap por locale y robots.txt coherentes con páginas públicas indexables.
- Ficheros/paquetes a crear o modificar (aproximado): `content.web.SitemapController`, `RobotsController`, generación/caché y tests.
- Impacto modular: consulta páginas publicadas de `content`; sin tablas/eventos ni acceso a módulos ajenos. Ciclo: ninguno.
- UI: endpoints `/sitemap.xml` y `/robots.txt`; exclusiones para gestión, login y preview.
- Seguridad: no incluir URLs internas, query de preview ni rutas con datos de usuario.
- Criterios de aceptación (Given/When/Then):
  - Given páginas publicadas ES/EN When se genera sitemap Then cada URL tiene locale/canonical válido y no hay duplicados.
  - Given `/gestion/**`, preview o recurso privado When se genera sitemap/robots Then no se indexa ni expone.
- Tests: TC-SEO-002: UT (URLs/duplicados), WT (XML/robots), SEC (rutas privadas ausentes).
- Documentación a actualizar: DIARIO + INVENTARIO; README «SEO»
- Estimación: S (≤ 0,5 d)
- Riesgos: host/base URL no confirmado; generar desde propiedad validada por entorno.

### SIM-002 — Contrato OpenAPI portal↔DEMO en ambos repos con validación
- Fase / épica / módulo owner: F1 / SIM / simbiosphere
- Tipo: docs
- Depende de: FND-002, DEMO-01
- Descripción: Cubre: RF-SIM-09; acuerda OpenAPI M2M antes de implementar endpoints, con idempotencia, payload sin contraseña, respuestas y problemas RFC 7807.
- Ficheros/paquetes a crear o modificar (aproximado): borrador OpenAPI en `docs/plan/05-integracion-simbiosphere.md`; referencias a contratos de ambos repos (sin cambios en DEMO en esta fase).
- Impacto modular: contrato futuro consumido por `simbiosphere.api`; sin tablas ni eventos de portal. Ciclo: ninguno (contrato externo).
- UI: n/a (contrato de integración).
- Seguridad: HMAC-SHA256, timestamp anti-replay, scopes, `Idempotency-Key`; excluir password, tokens y PII de logs.
- Criterios de aceptación (Given/When/Then):
  - Given alta y comprobación de slug When se valida OpenAPI Then define 201/409/422/429/503, esquemas y ejemplos sin secretos.
  - Given contrato que contiene password o referencia solo por email When corre lint/contract test Then falla y exige activación just-in-time y externalAccountId.
- Tests: TC-SIM-002: CT (lint/schema y ejemplos), revisión de contrato por owners portal/DEMO.
- Documentación a actualizar: DIARIO + INVENTARIO; 05-integracion y ADR-002
- Estimación: M (≤ 1 d)
- Riesgos: desfase entre contrato y DEMO; bloquear implementación hasta acuerdo y tests de proveedor.
