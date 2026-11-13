# F0 — UI kit y gobernanza (pipeline front, tema, i18n, layouts, menú): tareas

- Objetivo: establecer la base frontend accesible y los gates documentales/modulares (hito M0; ver [roadmap](../10-roadmap-backlog.md)).
- Módulo(s) owner: `config`, `shared` y documentación; las tareas GOV actualizan raíz y `.github/`.

## Estado de ejecución — PROMPT 03

Implementación iniciada el 2026-11-11. No se declaran tareas completadas hasta superar sus tests y gates.

| ID | Estado actual | Evidencia / bloqueo |
|---|---|---|
| UIK-002 | VALIDADO | `ThemeContrastTest` (2 tests) pasó con JDK 21 y la comprobación aritmética independiente de ratios también pasó. |
| UIK-003 | VALIDADO | Build frontend limpio y `FrontendAssetPipelineTest` (5) confirman `system-ui, sans-serif` sin archivos, `@font-face` ni CDN tipográficos. |
| UIK-004 | VALIDADO | `ThemePreferenceTest` (3), `PreferenceControllerTest` (1) y E2E de cambio/persistencia anónima tras recarga pasan. El SPI opcional para persistencia de perfil queda disponible para la futura integración con Identidad, tal como define el alcance. |
| UIK-005 | VALIDADO | Resolver/paridad (`PortalLocaleResolverTest` 2, `I18nKeyParityTest` 3), `WebViewsTest` (4) y E2E de cambio de idioma conservando la ruta pública pasan. |
| UIK-006 | VALIDADO | `WebViewsTest` (4) y la suite Playwright/axe (13) validan páginas de error, CSRF, drawer, viewport 320/768/1280 px, ES/EN, tema, objetivos táctiles y accesibilidad. |
| UIK-007 | VALIDADO | `NavigationComposerTest` valida permisos para anónimo, CLIENT, EMPLOYEE y ADMIN. E2E confirma overflow a 768 px en ES/EN, ausencia de scroll horizontal, enlaces en drawer, marca del activo y navegación/retorno de foco por teclado. |
| UIK-008 | VALIDADO | `CsrfTest` (2), `TemplateRulesTest` (5) y E2E HTMX pasan; la mutación envía CSRF, el rechazo expone error accesible con foco y no se inyectan estilos HTMX inline incompatibles con CSP. |
| UIK-009 | VALIDADO | `npm run e2e` pasa 13/13 contra la aplicación local: matriz ES/EN, claro/oscuro y móvil/escritorio; axe sin violaciones y capturas en `visual/`. |
| GOV-001 | VALIDADO | `GovernanceArtifactTest` comprueba P01–P12 y que el validador detecte una norma ausente (4 tests pasan). |
| GOV-002 | VALIDADO | `GovernanceArtifactTest` comprueba instrucciones, enlace a INVENTARIO y enlaces locales (4 tests pasan). |
| GOV-003 | VALIDADO | `GovernanceArtifactTest` valida frontmatter, triggers y secciones de la skill (4 tests pasan). |
| GOV-004 | VALIDADO | `GovernanceArtifactTest` valida README, INVENTARIO, DIARIO y enlaces locales (4 tests pasan). |
| GOV-005 | VALIDADO | `DocumentationArchitectureTest` (3 tests) pasó: inventario↔Modulith, rechazo de módulo no inventariado y generación del artefacto Documenter. |
| GOV-006 | VALIDADO | `AdrDocumentRulesTest` (4) pasa contra todos los ADR y comprueba metadatos, plantilla, referencias y enlaces. |

El perfil Maven `frontend-build` no se ejecutó localmente porque ejecuta `npm ci`; esta integración permanece disponible para CI, pero no es un criterio de cierre de PROMPT 03. `npm run build`, `FrontendAssetPipelineTest` (5) y el `mvn clean verify -Plocal-h2` previamente ejecutado pasan; el `verify` completo registró 73 tests, 0 fallos/errores y JaCoCo aprobado. No hay bloqueo de licencia tipográfica: se usa `system-ui, sans-serif`.

### UIK-002 — Tokens de tema claro/oscuro, mapeo Tailwind y test de contraste
- Fase / épica / módulo owner: F0 / UIK / config
- Tipo: feat
- Depende de: FND-002
- Descripción: Cubre: RF-UI-02, RF-UI-03; define variables semánticas para superficies, texto, acento, foco y estados; mapea Tailwind sin colores directos.
- Ficheros/paquetes a crear o modificar (aproximado): `src/main/resources/static/css/tokens.css`, tema Tailwind y `ThemeContrastTest`.
- Impacto modular: `config` posee tokens globales; sin API/eventos/tablas. Ciclo: ninguno (estilos no importan módulos).
- UI: claro/oscuro en todas las vistas; colores del brief y ratios AA por par.
- Seguridad: rechazar colores hard-coded en plantillas; texto normal ≥4,5:1 y componentes ≥3:1.
- Criterios de aceptación (Given/When/Then):
  - Given tokens calculados del brief When corre `ThemeContrastTest` Then ratios exigidos coinciden en ambos temas.
  - Given lima como texto sobre fondo claro When se valida Then falla; acento oliva `#3e4202` y borde del botón primario sí pasan.
- Tests: TC-UIK-002: UT (ratios WCAG; fixtures inválidos DEBEN fallar).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Tema»
- Estimación: M (≤ 1 d)
- Riesgos: cambio de token puede degradar contraste; gate requerido en CI.

### UIK-003 — Tipografía del sistema sin fuentes web externas
- Fase / épica / módulo owner: F0 / UIK / config
- Tipo: feat
- Depende de: FND-002
- Descripción: Cubre RF-UI-04 usando una pila tipográfica disponible en el sistema operativo; no incluye ni descarga fuentes web.
- Ficheros/paquetes a crear o modificar (aproximado): `tailwind.config.cjs`, `src/main/frontend/css/tokens.css`, prueba del stack tipográfico.
- Impacto modular: estáticos de `config`; sin API, eventos ni tablas. Ciclo: ninguno.
- UI: misma pila `system-ui, sans-serif` para marca, titulares y cuerpo; legible en móvil y escritorio.
- Seguridad: no hay archivos de fuentes, `@font-face`, preloads tipográficos ni peticiones a CDN.
- Criterios de aceptación (Given/When/Then):
  - Given cualquier página When se compila el frontend Then las familias de marca, titulares y cuerpo usan `system-ui, sans-serif`.
  - Given los recursos frontend When se inspeccionan Then no existen declaraciones `@font-face`, archivos tipográficos ni referencias externas a fuentes.
- Tests: TC-UIK-003: UT (configuración de la pila y ausencia de fuentes web).
- Documentación a actualizar: DIARIO + INVENTARIO; RF-UI-04 y pregunta 17 de 13
- Estimación: S (≤ 0,5 d)
- Riesgos: métricas pueden variar entre sistemas operativos; verificar el desbordamiento del menú en la matriz responsive.

### UIK-004 — Mecanismo de tema (anti-FOUC con nonce, cookie, conmutador)
- Fase / épica / módulo owner: F0 / UIK / config
- Tipo: feat
- Depende de: UIK-002, FND-009
- Descripción: Cubre: RF-UI-02; aplica `prefers-color-scheme` y persiste en `localStorage` y cookie `sp_theme` para usuarios anónimos y autenticados. La persistencia en perfil autenticado se difiere hasta la integración con Identidad.
- Ficheros/paquetes a crear o modificar (aproximado): `templates/fragments/theme.html`, JS estático, `config/theme/ThemeResolver` y `ThemePreferenceTest`.
- Impacto modular: `config` consume nonce de CSP; conserva `ThemePreference` como SPI opcional para una futura integración, sin importar `identity`. Sin tablas/eventos. Ciclo: ninguno.
- UI: conmutador con estado accesible en los tres layouts; scrollbars e inputs usan `color-scheme`.
- Seguridad: script anti-FOUC único con nonce; cookie técnica sin datos personales.
- Criterios de aceptación (Given/When/Then):
  - Given preferencia del sistema y no hay selección guardada When carga HTML Then aplica el sistema sin parpadeo mediante un script con nonce CSP.
  - Given usuario anónimo o autenticado cambia tema When recarga Then mantiene la selección en cookie/localStorage sin requerir un proveedor de Identidad.
  - Given existe un proveedor `ThemePreference` When un usuario autenticado carga/cambia tema Then el SPI puede leer/guardar la preferencia del perfil.
- Tests: TC-UIK-004: WT (nonce/FOUC/conmutador), IT (cookie/localStorage; fallback autenticado sin proveedor y SPI opcional).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Tema»
- Estimación: M (≤ 1 d)
- Riesgos: CSP o preferencias discordantes causan FOUC; probar ambos estados y navegación parcial.

### UIK-005 — Infraestructura i18n (URL `/{lang}`, resolver, bundles por módulo, test de claves)
- Fase / épica / módulo owner: F0 / UIK / config
- Tipo: feat
- Depende de: FND-002
- Descripción: Cubre: RF-UI-05, RNF-TST-05; resuelve ES/EN, URL pública `/es`/`/en`, selección inicial por `Accept-Language` y preferencia privada.
- Ficheros/paquetes a crear o modificar (aproximado): `config/i18n/LocaleConfig`, bundles `static`/módulo y `I18nKeyParityTest`.
- Impacto modular: `config` expone locale; módulos poseen `i18n/<modulo>/messages_{es,en}.properties`. Sin eventos/tablas. Ciclo: ninguno.
- UI: selector conserva página actual; email usa locale del destinatario; fechas UTC se muestran en `Europe/Madrid`.
- Seguridad: cookie técnica `sp_lang`; validar locale permitido y escapar mensajes dinámicos.
- Criterios de aceptación (Given/When/Then):
  - Given clave ES sin equivalente EN When corre `I18nKeyParityTest` Then el build falla nombrando módulo y clave.
  - Given selector en `/es/servicios` When se cambia a EN Then navega al slug EN equivalente o fallback definido, conservando el contexto.
- Tests: TC-UIK-005: UT (resolver/fallback), IT (paridad de claves por bundle).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Idiomas»; ADR-003
- Estimación: M (≤ 1 d)
- Riesgos: slugs CMS distintos requieren mapeo de alternates (CMS-004).

### UIK-006 — Layouts público/cliente/backoffice, footer, skip link y páginas de error
- Fase / épica / módulo owner: F0 / UIK / config
- Tipo: feat
- Depende de: UIK-002, UIK-004, UIK-005
- Descripción: Cubre: RF-UI-01, RF-UI-07; prepara layouts público, cliente y gestión con cabecera, footer, skip link y errores de marca.
- Ficheros/paquetes a crear o modificar (aproximado): `templates/layouts/{public,account,backoffice}.html`, `templates/errors/{403,404,500}.html`, fragmentos.
- Impacto modular: `config` compone fragmentos; sin importar módulos de negocio. Sin tablas/eventos. Ciclo: ninguno.
- UI: `/es/**`, `/cuenta/**`, `/gestion/**`; mobile-first, foco visible, temas e idiomas compartidos.
- Seguridad: no renderizar navegación protegida sin permiso; escapes Thymeleaf por defecto.
- Criterios de aceptación (Given/When/Then):
  - Given viewport 320 px When se abre cada layout Then no hay scroll horizontal y objetivos táctiles miden ≥44 px.
  - Given rutas inexistente/prohibida/error When responde servidor Then muestra página 404/403/500, locale, tema y skip link.
- Tests: TC-UIK-006: WT (layouts/errores), A11Y (landmarks, skip link y foco).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Navegación»
- Estimación: M (≤ 1 d)
- Riesgos: Layout Dialect debe ser compatible con Boot 4 (VERIFICAR en FND-001).

### UIK-007 — Menú superior con detección de desbordamiento, drawer accesible y NavContribution por rol
- Fase / épica / módulo owner: F0 / UIK / config
- Tipo: feat
- Depende de: UIK-006, FND-003
- Descripción: Cubre: RF-UI-06; compone aportaciones de navegación por `shared.spi.NavContribution` para anónimo, cliente, empleado y ADMIN.
- Ficheros/paquetes a crear o modificar (aproximado): `config/navigation/NavigationComposer`, fragmento `navbar.html`, `menu.js`, bundles ES/EN.
- Impacto modular: `config` consume SPI `shared`; módulos publican contribuciones sin dependencia inversa. Sin tablas/eventos. Ciclo: ninguno.
- UI: `ResizeObserver` tras `document.fonts.ready`; si no cabe, todos los enlaces pasan al botón derecho; fallback CSS sin JS.
- Seguridad: ocultar no sustituye autorización; aria-expanded/controls, Esc, clic fuera, focus trap y bloqueo de scroll.
- Criterios de aceptación (Given/When/Then):
  - Given cualquier rol/idioma y ancho insuficiente When termina carga de fuentes Then todos los enlaces pasan al drawer derecho y se indica el activo.
  - Given teclado abre el drawer When pulsa Esc o tabula Then se cierra/restaura foco o queda contenido dentro del focus trap.
- Tests: TC-UIK-007: UT (composición por rol), E2E/VIS (desbordamiento ES/EN y móvil/escritorio), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; README «Menú»
- Estimación: M (≤ 1 d)
- Riesgos: fuentes o traducciones cambian anchura; recalcular en resize y al completar fuentes.

### UIK-008 — Componentes base (formularios, tablas, paginación, toasts) y HTMX base (CSRF, errores)
- Fase / épica / módulo owner: F0 / UIK / config
- Tipo: feat
- Depende de: UIK-006
- Descripción: Cubre: RF-UI-08, RNF-A11Y-01; define fragmentos reutilizables para formularios, tablas, paginación y avisos HTMX.
- Ficheros/paquetes a crear o modificar (aproximado): `templates/fragments/{form,table,pagination,toast}.html`, `static/js/htmx-config.js`, bundles.
- Impacto modular: fragmentos neutrales de `config`; módulos los insertan por contrato UI URL, sin importación Java. Sin tablas/eventos. Ciclo: ninguno.
- UI: errores asociados, región live para toast, tema claro/oscuro, teclado y fallback de formulario sin JS.
- Seguridad: token CSRF en mutaciones HTMX, escapes Thymeleaf y no confiar en IDs ocultos.
- Criterios de aceptación (Given/When/Then):
  - Given POST HTMX sin CSRF When se procesa Then retorna 403 y no ejecuta la mutación.
  - Given formulario con error When se devuelve fragmento Then el mensaje queda asociado al campo, anunciado y enfocado sin perder contexto.
- Tests: TC-UIK-008: WT (CSRF/errores), A11Y (labels/live region), UT (paginación).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Componentes»
- Estimación: M (≤ 1 d)
- Riesgos: fragmento HTMX puede perder nonce/foco; probar respuesta parcial y completa.

### UIK-009 — Infra E2E: Playwright + axe + matriz visual
- Fase / épica / módulo owner: F0 / UIK / test
- Tipo: test
- Depende de: UIK-006
- Descripción: Cubre: RNF-TST-03, RNF-TST-04, RNF-A11Y-01; crea base Playwright, axe-core y capturas visuales para los recorridos y páginas clave.
- Ficheros/paquetes a crear o modificar (aproximado): `package.json`, `playwright.config.ts`, `e2e/`, `visual/`, datos de prueba sintéticos.
- Impacto modular: tests de extremo a extremo consumen la app por HTTP; sin API de producción ni tablas propias. Ciclo: ninguno.
- UI: matriz claro/oscuro × móvil/escritorio × ES/EN; teclado, menú y formularios principales.
- Seguridad: cuentas/datos sintéticos, secretos solo por entorno; no guardar contraseñas ni tokens en trazas o capturas.
- Criterios de aceptación (Given/When/Then):
  - Given páginas representativas When Playwright recorre viewport/locale/tema Then guarda capturas comparables y axe no informa violaciones bloqueantes.
  - Given E2E sin servicio disponible When corre local Then informa precondición ausente y no marca falsamente el recorrido como aprobado.
- Tests: TC-UIK-009: E2E (arranque/navegación), A11Y (axe), VIS (capturas matriz).
- Documentación a actualizar: DIARIO + INVENTARIO; README «E2E»
- Estimación: M (≤ 1 d)
- Riesgos: navegador/driver o fonts variables causan falsos positivos; fijar versiones y fuentes.

### GOV-001 — Normas de desarrollo modular P01–P12
- Fase / épica / módulo owner: F0 / GOV / docs
- Tipo: docs
- Depende de: FND-004
- Descripción: Cubre: RF-GOV-01; redacta P01–P12 con ownership único, APIs nombradas, DAG, SPIs, UI por URL, datos, eventos y excepciones.
- Ficheros/paquetes a crear o modificar (aproximado): `docs/arquitectura/NORMAS-DESARROLLO-MODULAR.md`, matriz P01–P12 y ejemplos mínimos.
- Impacto modular: contrato documental para módulos y gates; sin APIs, eventos ni tablas. Ciclo: ninguno.
- UI: reglas para fragmentos, i18n, tema, roles y accesibilidad; no crea pantallas.
- Seguridad: incluir IDOR, CSRF, PII y límites de confianza; excepciones deben tener owner y vencimiento.
- Criterios de aceptación (Given/When/Then):
  - Given un cambio de módulo When se revisa contra P01–P12 Then owner, contratos, tablas y dependencias quedan identificados.
  - Given dependencia prohibida o excepción sin caducidad When se revisa Then la norma define gate y rechazo.
- Tests: TC-GOV-001: UT (validador/checklist documental detecta reglas P01–P12 incompletas).
- Documentación a actualizar: DIARIO + INVENTARIO; enlazar desde README
- Estimación: M (≤ 1 d)
- Riesgos: divergencia con `ApplicationModules.verify()`; sincronizar reglas con FND-004.

### GOV-002 — `.github/copilot-instructions.md`
- Fase / épica / módulo owner: F0 / GOV / docs
- Tipo: docs
- Depende de: GOV-001
- Descripción: Cubre: RF-GOV-01; convierte P01–P12 en instrucciones aplicables por defecto a cambios del repositorio.
- Ficheros/paquetes a crear o modificar (aproximado): `.github/copilot-instructions.md`, enlaces a normas y skill del portal.
- Impacto modular: instrucción transversal; no añade contratos, eventos ni tablas. Ciclo: ninguno.
- UI: obliga a revisar tema, i18n, móvil y accesibilidad cuando haya interfaz.
- Seguridad: incluye autorización/IDOR, CSRF, secretos y tests; nunca solicita copiar secretos a prompts.
- Criterios de aceptación (Given/When/Then):
  - Given tarea nueva When se lee el archivo Then exige identificar owner, dependencias, tests y documentación antes de editar.
  - Given regla de P01–P12 When cambia Then la instrucción enlaza la fuente normativa y no duplica versiones contradictorias.
- Tests: TC-GOV-002: UT (comprobación de enlaces y secciones obligatorias del archivo).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Contribuir»
- Estimación: S (≤ 0,5 d)
- Riesgos: instrucciones largas pueden perder prioridad; mantener concisas y enlazar normas.

### GOV-003 — Skill propia del portal
- Fase / épica / módulo owner: F0 / GOV / docs
- Tipo: docs
- Depende de: GOV-001
- Descripción: Cubre: RF-GOV-01; define skill obligatoria para feature/fix/refactor/plantilla con análisis modular, TDD, seguridad y Definition of Done.
- Ficheros/paquetes a crear o modificar (aproximado): `.github/skills/sportal-modular/SKILL.md`, checklist y rutas de lectura.
- Impacto modular: guía el proceso; contratos/eventos/tablas no aplican. Ciclo: ninguno.
- UI: checklist de fragments, claves i18n, tokens, responsive y WCAG cuando el cambio sea frontend.
- Seguridad: revisar roles, CSRF, PII, SSRF/subidas, logs y amenazas; detenerse ante riesgo no resuelto.
- Criterios de aceptación (Given/When/Then):
  - Given cambio de cualquier tipo When se aplica skill Then solicita tests, gate modular y README/INVENTARIO/DIARIO actualizados.
  - Given tarea fuera de alcance o requisito ambiguo When afecta seguridad/datos Then la skill indica no implementar y documentar/preguntar.
- Tests: TC-GOV-003: UT (validador de frontmatter, triggers y secciones obligatorias).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Skill»
- Estimación: M (≤ 1 d)
- Riesgos: skill ignorada si el trigger es ambiguo; validar metadatos con herramienta disponible.

### GOV-004 — README, INVENTARIO y DIARIO iniciales
- Fase / épica / módulo owner: F0 / GOV / docs
- Tipo: docs
- Depende de: FND-002
- Descripción: Cubre: RF-DOC-01, RF-DOC-02, RF-DOC-03; crea documentación inicial, con estados PLANIFICADO y entradas verificables desde las tareas, artefactos y pruebas observables del workspace; no se consulta historial de Git.
- Ficheros/paquetes a crear o modificar (aproximado): `README.md`, `docs/INVENTARIO.md`, `DIARIO.md`, enlaces a arquitectura/roadmap.
- Impacto modular: documentos de repo; cada módulo conserva owner único. Sin APIs/eventos/tablas adicionales. Ciclo: ninguno.
- UI: inventario enumera páginas/rutas previstas, rol, locale y tema; no crea UI.
- Seguridad: no copiar secretos ni datos reales del historial; redactar evidencias sensibles.
- Criterios de aceptación (Given/When/Then):
  - Given clon limpio When se sigue README Then arranque, entorno, tests y mapa documental son navegables.
  - Given módulos planificados When se consulta INVENTARIO/DIARIO Then estados y entradas de FND-001/FND-002 incluyen fuente verificable.
- Tests: TC-GOV-004: UT (enlaces/rutas del inventario y estructura del diario).
- Documentación a actualizar: DIARIO + INVENTARIO; README inicial
- Estimación: M (≤ 1 d)
- Riesgos: registrar como HECHO algo aún no implementado; usar PLANIFICADO hasta evidencia.

### GOV-005 — Test inventario↔Modulith y diagramas con Documenter
- Fase / épica / módulo owner: F0 / GOV / test
- Tipo: test
- Depende de: GOV-004, FND-004
- Descripción: Cubre: RF-DOC-05; compara módulos detectados por Spring Modulith con inventario y genera diagramas revisables.
- Ficheros/paquetes a crear o modificar (aproximado): `src/test/java/.../DocumentationArchitectureTest`, `docs/arquitectura/`, `docs/INVENTARIO.md`.
- Impacto modular: test de arquitectura consulta Modulith; no agrega dependencias de negocio. Sin tablas/eventos. Ciclo: ninguno.
- UI: diagramas Mermaid de módulos y rutas contractuales, sin pantalla nueva.
- Seguridad: excluir valores de configuración, secretos y PII de diagramas/reportes.
- Criterios de aceptación (Given/When/Then):
  - Given módulo Spring Modulith no inventariado When corre test Then falla indicando módulo y sección faltante.
  - Given inventario y código sincronizados When corre Documenter Then diagramas se generan sin credenciales ni datos sensibles.
- Tests: TC-GOV-005: MT (`ApplicationModules`), UT (inventario↔módulos), validación de artefactos Documenter.
- Documentación a actualizar: DIARIO + INVENTARIO; README «Arquitectura»
- Estimación: M (≤ 1 d)
- Riesgos: salida Documenter cambia entre versiones; fijar formato y revisar diff.

### GOV-006 — Registro y validación de ADR versionados
- Fase / épica / módulo owner: F0 / GOV / docs
- Tipo: docs
- Depende de: GOV-001, GOV-004
- Descripción: Cubre: RF-DOC-04; mantiene ADR con nombre acordado, estado/fecha, contexto, decisión, alternativas, consecuencias y tarea de verificación.
- Ficheros/paquetes a crear o modificar (aproximado): `docs/adr/`, plantilla y validador de metadatos/enlaces.
- Impacto modular: documentación transversal; sin API, eventos ni tablas. Ciclo: ninguno.
- UI: no aplica.
- Seguridad: no incluir secretos ni datos personales en ADR ni en reportes del validador.
- Criterios de aceptación (Given/When/Then):
  - Given ADR nuevo When corre el validador Then exige estado, fecha, decisión, alternativas, consecuencias y referencias a tareas.
  - Given ruta/tarea inválida When se valida el conjunto Then el gate falla indicando ADR y referencia; todos los ADR vigentes tienen enlaces válidos.
- Tests: TC-GOV-006: UT (plantilla/metadatos), IT (enlaces y referencias a tareas).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Decisiones»
- Estimación: M (≤ 1 d)
- Riesgos: ADR sin mantener; revisar su vigencia en cada cambio arquitectónico.
