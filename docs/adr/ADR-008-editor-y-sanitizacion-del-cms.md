# ADR-008 — Editor y sanitización del CMS

- Estado: Borrador
- Fecha: 2026-11-10

## Contexto
El CMS necesita texto enriquecido acotado y bloques reutilizables; el contenido se sirve en páginas públicas y el equipo edita mediante backoffice. Renderizar HTML arbitrario crea riesgo XSS.

## Decisión recomendada
CommonMark más bloques estructurados (hero, tarjetas, texto, imagen, CTA, FAQ). Sanitizar HTML en servidor con OWASP Java HTML Sanitizer y allowlist antes de persistir/renderizar `th:utext`; SVG subido se rechaza salvo sanitización validada. Guardar snapshots al publicar y permitir restaurar el anterior en MVP.

## Alternativas
- HTML/rich text libre: expresivo, pero amplía XSS y complica preview/CSP; rechazado.
- Markdown sin sanitizador: no elimina URLs/HTML activo; rechazado.
- Editor externo SaaS: proveedor/transferencia/encargado no decidido; no recomendado para MVP.
- Historial diff completo: valor añadido, dejar R2; snapshot/restauración basta en MVP.

## Consecuencias
Cada bloque tiene schema allowlist y validación de propiedades/URL. Solo salida sanitizada puede llegar a `th:utext`; Gate CI lo verifica. Edición/publicación requiere `CONTENT_EDIT`, auditoría y caché invalidada.

## Verificaciones pendientes
- FND-001: compatibilidad/librería y versión aprobada.
- CMS-003/005/006/007: sanitización, editor, bloques y snapshots.
- FND-008: `TemplateRulesTest` y allowlist.
