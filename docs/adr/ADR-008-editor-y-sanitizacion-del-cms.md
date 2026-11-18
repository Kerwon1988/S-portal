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
- El código inicial usa CommonMark `0.24.0` y OWASP Java HTML Sanitizer `20240325.1` (versiones declaradas en `pom.xml`); `ContentSanitizer` permite solo formato, enlaces HTTPS/mailto y elimina HTML activo. La decisión sigue en borrador, sin aprobación del owner.
- `th:utext` se limita a contenido saneado de páginas/legal y biografías; `TemplateRulesTest` mantiene una allowlist explícita.
- CMS-003/005/006/007 tiene implementación y pruebas focales añadidas, pero no se han compilado/ejecutado por fallo PKIX al resolver CommonMark/OWASP.
- FND-008/`TemplateRulesTest` y la política completa de allowlist requieren ejecución satisfactoria antes de aceptar esta ADR.
