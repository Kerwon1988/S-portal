# F1 — CMS bilingüe (contenido público, edición, publicación y medios): tareas

- Objetivo: permitir publicar contenido ES/EN con permisos, sanitización y medios validados (hito M1; ver [roadmap](../10-roadmap-backlog.md)).
- Módulo(s) owner: `content`; consume contratos `files.api` y `audit.api`.

### CMS-001 — Modelo de datos del CMS y migraciones
- Fase / épica / módulo owner: F1 / CMS / content
- Tipo: feat
- Depende de: FND-006, FIL-001
- Descripción: Cubre: RF-CMS-01, RNF-DB-02; modela página, traducción, bloque, slug, metadatos y publicación con esquema SQL por owner.
- Ficheros/paquetes a crear o modificar (aproximado): `content.domain`, `content.infrastructure`, migraciones `db/migration/content/V...`, `Page/PageTranslation/ContentBlock`.
- Impacto modular: tablas `content.page`, `page_translation`, `content_block`; `content` consume `files.api`; sin eventos iniciales. Ciclo: ninguno.
- UI: n/a (modelo; editor en CMS-005/006).
- Seguridad: slug normalizado y único por locale; contenido nunca se renderiza sin estado PUBLICADO.
- Criterios de aceptación (Given/When/Then):
  - Given página con traducciones ES/EN When se persiste en el perfil H2 Then cada entidad conserva owner, slug, estado y versionado.
  - Given slug duplicado en el mismo locale When se persiste en H2 Then la restricción única rechaza el duplicado sin sobrescribir la traducción existente.
- Tests: TC-CMS-001: UT (invariantes) e IT de repositorios con H2; revisión estática de migraciones. No se requieren pruebas Flyway/Testcontainers/SQL Server.
- Documentación a actualizar: DIARIO + INVENTARIO; modelo ER
- Estimación: M (≤ 1 d)
- Riesgos: modelo genérico puede crecer sin necesidad; limitar bloques a catálogo acordado.

### CMS-002 — Dominio de publicación, permiso CONTENT_EDIT, caché y auditoría
- Fase / épica / módulo owner: F1 / CMS / content
- Tipo: feat
- Depende de: CMS-001, AUD-001
- Descripción: Cubre: RF-CMS-02, RF-CMS-07; implementa borrador/publicado, vista previa, autoría y evento de invalidación de caché al publicar.
- Ficheros/paquetes a crear o modificar (aproximado): `content.application.PublicationService`, `content.api`, listener/cache adapter y permisos.
- Impacto modular: `content.api` publica `PagePublished` (solo IDs); consume `audit.api`; tablas content owner `content`. Ciclo: ninguno.
- UI: preview autenticado, noindex; el público solo resuelve versión publicada.
- Seguridad: `CONTENT_EDIT` en cada comando; auditar actor/fecha; preview no accesible por URL pública.
- Criterios de aceptación (Given/When/Then):
  - Given borrador autorizado When editor publica Then persiste versión y autoría, audita y expulsa caché.
  - Given rol sin `CONTENT_EDIT` When invoca publicación Then responde 403 y no cambia estado ni caché.
- Tests: TC-CMS-002: UT (transiciones), MT (evento/auditoría), WT (permiso).
- Documentación a actualizar: DIARIO + INVENTARIO; README «CMS»
- Estimación: M (≤ 1 d)
- Riesgos: caché publicada obsoleta; invalidación verificable e idempotente.

### CMS-003 — Sanitización de contenido con allowlist
- Fase / épica / módulo owner: F1 / CMS / content
- Tipo: feat
- Depende de: CMS-001
- Descripción: Cubre: RF-CMS-04; transforma CommonMark/bloques en HTML permitido con OWASP Java HTML Sanitizer en servidor.
- Ficheros/paquetes a crear o modificar (aproximado): `content.application.ContentSanitizer`, política allowlist y `content_sanitizer` fixtures.
- Impacto modular: servicio interno `content`; sin tablas/eventos/API nueva ni dependencia de web. Ciclo: ninguno.
- UI: editor muestra preview saneada, con CSP y tema del sitio.
- Seguridad: quitar scripts, event handlers, URL peligrosa y SVG activo; `th:utext` solo desde salida saneada allowlist.
- Criterios de aceptación (Given/When/Then):
  - Given Markdown con encabezados/enlaces válidos When se sanitiza Then conserva formato permitido y escapa texto.
  - Given `<script>`, `onerror` o `javascript:` When se sanitiza Then elimina carga activa antes de persistir/renderizar.
- Tests: TC-CMS-003: UT (fixtures XSS permitidos/denegados; inválidos DEBEN fallar), SEC.
- Documentación a actualizar: DIARIO + INVENTARIO; README «Contenido seguro»; ADR-008
- Estimación: M (≤ 1 d)
- Riesgos: allowlist demasiado amplia reabre XSS; cambios requieren revisión seguridad.

### CMS-004 — Render público por slug e idioma, fallback y redirecciones 301
- Fase / épica / módulo owner: F1 / CMS / content
- Tipo: feat
- Depende de: CMS-002, CMS-003, UIK-006, UIK-005
- Descripción: Cubre: RF-CMS-01, RF-CMS-06, RF-WEB-07; resuelve contenido publicado por slug/locale, fallback y redirección de slug anterior.
- Ficheros/paquetes a crear o modificar (aproximado): `content.web.PublicPageController`, `content.application`, resolver locale y tabla `content.slug_redirect`.
- Impacto modular: `content` usa `config` por composición MVC; tablas content owner `content`; sin ciclos ni eventos requeridos.
- UI: `/{lang}/{slug}`, canonical/hreflang y tema; fallback de traducción documentado.
- Seguridad: no renderizar borrador; `th:utext` recibe HTML saneado y slugs no admiten path traversal.
- Criterios de aceptación (Given/When/Then):
  - Given página publicada solo en ES When se solicita EN Then aplica fallback definido y la respuesta indica locale canónico.
  - Given slug renombrado When se visita URL antigua Then responde 301 al nuevo slug del mismo idioma; borrador da 404.
- Tests: TC-CMS-004: WT (locale/fallback/301), SEC (sin preview público), SEO.
- Documentación a actualizar: DIARIO + INVENTARIO; mapa de URLs
- Estimación: M (≤ 1 d)
- Riesgos: slug EN puede faltar; evitar alternates que apunten a contenido inexistente.

### CMS-005 — Backoffice CMS: listado y editor de páginas con vista previa
- Fase / épica / módulo owner: F1 / CMS / content
- Tipo: feat
- Depende de: CMS-002, UIK-008, IAM-012
- Descripción: Cubre: RF-CMS-02; da listado paginado y editor de título/slug/estado con preview segura.
- Ficheros/paquetes a crear o modificar (aproximado): `content.web`, `templates/content/admin/`, `i18n/content/messages_{es,en}.properties`.
- Impacto modular: `content.web→application→domain`; consume `identity` autorización y `audit.api`; tablas content owner `content`. Sin ciclos.
- UI: `/gestion/contenido`; filtros ES/EN, tema, móvil, preview no indexable.
- Seguridad: `CONTENT_EDIT` también en POST; CSRF, validación de slug y escapes por defecto.
- Criterios de aceptación (Given/When/Then):
  - Given ADMIN con `CONTENT_EDIT` When guarda borrador Then ve preview autenticada y el público mantiene versión previa.
  - Given EMPLOYEE sin permiso When abre editor o envía POST Then responde 403 y no filtra contenido privado.
- Tests: TC-CMS-005: WT (listado/editor/CSRF/403), A11Y (labels/errores), SEC.
- Documentación a actualizar: DIARIO + INVENTARIO; README «Edición CMS»
- Estimación: M (≤ 1 d)
- Riesgos: preview cacheada públicamente; añadir no-store y autorización en cada petición.

### CMS-006 — Editor de bloques (hero, tarjetas, texto, imagen, CTA, FAQ)
- Fase / épica / módulo owner: F1 / CMS / content
- Tipo: feat
- Depende de: CMS-005, CMS-003
- Descripción: Cubre: RF-CMS-01, RF-CMS-04; permite componer tipos de bloque acotados con contenido y orden por idioma.
- Ficheros/paquetes a crear o modificar (aproximado): `content.web.BlockEditor`, `content.domain.ContentBlock`, fragmentos y bundles.
- Impacto modular: bloques persisten en `content`; referencias de imagen por `files.api`; sin evento nuevo. Ciclo: ninguno.
- UI: editor accesible con reordenación teclado; tema/locale y vista previa responsive.
- Seguridad: validar tipo/propiedades por esquema; sanitizar texto enriquecido y nunca aceptar HTML arbitrario.
- Criterios de aceptación (Given/When/Then):
  - Given editor con bloques permitidos When guarda ES/EN Then orden y datos se recuperan idénticos en preview.
  - Given tipo desconocido o URL `javascript:` When envía bloque Then validación rechaza y no publica HTML.
- Tests: TC-CMS-006: UT (esquema/orden), WT (editor), A11Y (teclado), SEC (payload activo).
- Documentación a actualizar: DIARIO + INVENTARIO; catálogo de bloques
- Estimación: M (≤ 1 d)
- Riesgos: bloque libre amplía superficie XSS; mantener whitelist versionada.

### CMS-007 — Versiones al publicar y restaurar la anterior
- Fase / épica / módulo owner: F1 / CMS / content
- Tipo: feat
- Depende de: CMS-005
- Descripción: Cubre: RF-CMS-03; guarda snapshot inmutable de cada publicación y permite restaurar el inmediatamente anterior.
- Ficheros/paquetes a crear o modificar (aproximado): entidad `content.page_version`, `PublicationService`, migración y acción de restaurar.
- Impacto modular: tabla `content.page_version` owner `content`; evento `PagePublished` con IDs; sin dependencia externa nueva. Ciclo: ninguno.
- UI: historial por página con autor/fecha y confirmación de restauración.
- Seguridad: `CONTENT_EDIT` para restaurar; registrar auditoría y no borrar snapshots.
- Criterios de aceptación (Given/When/Then):
  - Given dos versiones publicadas When se restaura la anterior Then crea nueva versión con snapshot anterior y conserva historial.
  - Given rol sin permiso When restaura por POST sin CSRF Then responde 403 y no cambia contenido.
- Tests: TC-CMS-007: UT (snapshot/versionado), IT (persistencia), WT (permiso/CSRF).
- Documentación a actualizar: DIARIO + INVENTARIO; R2 diff queda en roadmap
- Estimación: M (≤ 1 d)
- Riesgos: restauración destructiva si sobrescribe historial; implementar como nueva publicación.

### CMS-008 — Biblioteca de medios con alt obligatorio por idioma
- Fase / épica / módulo owner: F1 / CMS / content
- Tipo: feat
- Depende de: CMS-005, FIL-003, FIL-004
- Descripción: Cubre: RF-CMS-05; integra medios limpios, variantes y texto alternativo por locale en editor y publicación.
- Ficheros/paquetes a crear o modificar (aproximado): `content.web.MediaLibrary`, `content.api` referencia media, plantillas y bundles ES/EN.
- Impacto modular: consume `files.api` y `audit.api`; tabla de medios pertenece a `files`, referencias/alt a `content`. Sin ciclo.
- UI: `/gestion/contenido/medios`, selector con preview, tema y navegación por teclado.
- Seguridad: permisos CONTENT_EDIT, CSRF, solo medios CLEAN; descarga autorizada y `alt` requerido por idioma publicado.
- Criterios de aceptación (Given/When/Then):
  - Given medio CLEAN y alt ES/EN When se inserta en bloque Then usa variante responsive y alt del locale actual.
  - Given medio en cuarentena o alt vacío When se intenta publicar Then bloquea publicación y no crea URL accesible.
- Tests: TC-CMS-008: WT (biblioteca/alt), IT (Files API), A11Y (alt), SEC (cuarentena).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Biblioteca de medios»
- Estimación: M (≤ 1 d)
- Riesgos: confundir metadatos de contenido con ownership de fichero; mantener tabla files como único owner.

### CMS-009 — Equipo (miembros): CRUD y render
- Fase / épica / módulo owner: F1 / CMS / content
- Tipo: feat
- Depende de: CMS-008
- Descripción: Cubre: RF-WEB-05, RF-CMS-01; gestiona miembros del equipo con nombre, rol, bio, enlaces, foto y traducciones ES/EN.
- Ficheros/paquetes a crear o modificar (aproximado): `content.domain.TeamMember`, migración `content.team_member`, `team_member_translation`, admin templates.
- Impacto modular: tablas owner `content`; foto se referencia vía `files.api`; publica lectura por `content.api`. Sin eventos/ciclos.
- UI: `/gestion/contenido/equipo` y `/es/sobre-nosotros`; foto responsive con alt traducido.
- Seguridad: `CONTENT_EDIT`, CSRF y enlaces validados; no permitir scripts ni URLs no seguras en perfil público.
- Criterios de aceptación (Given/When/Then):
  - Given miembro con bio y foto por locale When se publica Then aparece en Sobre nosotros con rol, enlaces y alt del idioma activo.
  - Given miembro sin traducción o foto CLEAN When se publica Then se aplica fallback o se bloquea, sin imagen rota ni HTML activo.
- Tests: TC-CMS-009: UT (validaciones), WT (CRUD/render/permiso), A11Y (alt/enlaces).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Equipo»
- Estimación: M (≤ 1 d)
- Riesgos: fotos o bios sin consentimiento/licencia; solicitar materiales y autorización al cliente.

### CMS-010 — Documentos legales versionados con vigencia
- Fase / épica / módulo owner: F1 / CMS / content
- Tipo: feat
- Depende de: CMS-007
- Descripción: Cubre: RF-WEB-06, RNF-PRV-01; administra aviso legal, privacidad, cookies y términos por idioma con versión y vigencia.
- Ficheros/paquetes a crear o modificar (aproximado): `content.domain.LegalDocument`, migración `content.legal_document_version`, rutas públicas y admin.
- Impacto modular: contenido y versiones pertenecen a `content`; `legal` consume publicación para consentimientos sin depender de persistencia ajena. Sin ciclo.
- UI: footer y `/es/legal/...`, `/en/legal/...`; histórico solo para editores autorizados.
- Seguridad: `CONTENT_EDIT`, auditoría, HTML sanitizado; no marcar texto legal de ejemplo como aprobado.
- Criterios de aceptación (Given/When/Then):
  - Given versión con fecha futura When se consulta público Then sigue vigente la última versión efectiva del locale.
  - Given editor sin permiso When cambia términos Then responde 403; la versión anterior y sus evidencias permanecen.
- Tests: TC-CMS-010: UT (vigencia/versionado), WT (ruta/permiso), IT (snapshot persistido).
- Documentación a actualizar: DIARIO + INVENTARIO; LGL-001 usa versiones publicadas
- Estimación: M (≤ 1 d)
- Riesgos: publicar términos no aprobados; opción por defecto: contenido pendiente de validación legal.

### CMS-011 — Indicador de traducciones pendientes
- Fase / épica / módulo owner: F1 / CMS / content
- Tipo: feat
- Depende de: CMS-005
- Descripción: Cubre: RF-CMS-06; calcula traducciones faltantes por página/bloque y las muestra en el listado de edición.
- Ficheros/paquetes a crear o modificar (aproximado): `content.application.TranslationStatus`, fragmento de listado y bundles ES/EN.
- Impacto modular: lee tablas `content`; no llama a otros módulos ni publica eventos. Ciclo: ninguno.
- UI: indicador textual/icono con nombre accesible, filtro por locale faltante y tema coherente.
- Seguridad: solo `CONTENT_EDIT` ve borradores y pendientes internos; no exponer slug privado públicamente.
- Criterios de aceptación (Given/When/Then):
  - Given página ES publicada sin EN When carga listado autorizado Then aparece EN pendiente y el filtro la encuentra.
  - Given ambas traducciones completas When se guarda Then desaparece el indicador sin requerir caché de página pública.
- Tests: TC-CMS-011: UT (estado derivado), WT (filtro/permisos), A11Y (nombre accesible).
- Documentación a actualizar: DIARIO + INVENTARIO; política fallback en 02/06
- Estimación: S (≤ 0,5 d)
- Riesgos: contar bloques opcionales como traducción ausente; aplicar reglas por tipo.

### CMS-012 — Contenido semilla ES/EN (incluye eslogan elegido)
- Fase / épica / módulo owner: F1 / CMS / content
- Tipo: docs
- Depende de: CMS-006, CMS-010
- Descripción: Cubre: RF-CMS-08, RF-WEB-02; aporta home y páginas semilla ES/EN con slogan recomendado, dejando datos legales/equipo como pendientes.
- Ficheros/paquetes a crear o modificar (aproximado): seed `content`, bloques iniciales, bundles, prueba de idempotencia y fuentes de contenido.
- Impacto modular: escribe solo tablas `content`; usa migración/seeder idempotente, sin eventos ni dependencias Java nuevas. Ciclo: ninguno.
- UI: home con servicios, CTAs y visión futura de IA; textos CMS, responsive y localizados.
- Seguridad: no introducir razón social, NIF, dominio, fotos ni promesas sin confirmar; sanitizar texto.
- Criterios de aceptación (Given/When/Then):
  - Given DB vacía When corre seed dos veces Then hay una sola copia de bloques ES/EN y el slogan seleccionado está presente.
  - Given textos legales o materiales no aprobados When se inicializa Then quedan en borrador con SUPUESTO/pendiente, nunca publicados como hechos.
- Tests: TC-CMS-012: IT (seed idempotente), WT (home ES/EN y CTAs), SEC (sanitización).
- Documentación a actualizar: DIARIO + INVENTARIO; 13 datos/materiales pendientes
- Estimación: M (≤ 1 d)
- Riesgos: contenido supuesto publicado por error; separar seeds y estado de publicación.

### CMS-013 — Copy CMS bilingüe de gratuidad y condiciones de solicitud
- Fase / épica / módulo owner: F1 / CMS / content
- Tipo: feat
- Depende de: CMS-005, CMS-006, UIK-005
- Descripción: Cubre: RF-REQ-07; administra copy ES/EN de gratuidad y condiciones sin introducir pagos ni facturación en MVP.
- Ficheros/paquetes a crear o modificar (aproximado): bloque/fragmento CMS, claves ES/EN y contrato de lectura publicado para `request`.
- Impacto modular: `content` conserva ownership del texto; `request` consume lectura publicada sin acceder a tablas CMS.
- UI: editor ADMIN y render del copy en el detalle de solicitud; fallback seguro si no hay versión aprobada.
- Seguridad: permiso CONTENT_EDIT, CSRF y auditoría; no publicar contenido legal/comercial no aprobado.
- Criterios de aceptación (Given/When/Then):
  - Given copy ES/EN aprobado When ADMIN publica Then se muestra en el detalle de solicitud según locale y versión vigente.
  - Given contenido pendiente de aprobación When se renderiza una solicitud Then no se presenta como condición oficial ni se activa cobro.
- Tests: TC-CMS-013: UT (estado/aprobación), IT (lectura publicada), WT (permisos/locales/fallback).
- Documentación a actualizar: DIARIO + INVENTARIO; 02/13 y contrato content→request
- Estimación: M (≤ 1 d)
- Riesgos: mostrar condiciones no aprobadas; mantener borrador/fallback hasta aprobación.
