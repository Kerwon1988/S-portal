# 08 — Calidad y testing

## Pirámide y evidencia
| Nivel | Herramientas/patrones | Cobertura/criterio | Tareas/evidencia |
|---|---|---|---|
| Unitario dominio/application | JUnit 5, AssertJ; reloj inyectable | Invariantes, estados, permisos, sanitización; ≥80 % por módulo en domain/application | `TC-<ID>` tipo UT |
| Arquitectura/módulo | Spring Modulith `ApplicationModules.verify()`, `@ApplicationModuleTest`, ArchUnit | DAG acíclico, named interfaces, capas y tabla owner | FND-004, GOV-005; MT |
| MVC/seguridad | `@WebMvcTest`, Spring Security test | status/cabeceras/CSRF/rol/owner por ruta; default-deny | IAM-012, WT/SEC |
| Observabilidad/errores | UT de fingerprint/redacción/clasificación; IT JSON/locking/restart; WT handlers e ingesta; SEC PII; E2E JS/HTTP | Errores backend/SQL/JS/red y respuestas fallidas clasificados y deduplicados; stack/cadena causal saneados; tracker no oculta excepción original; diagnóstico útil con SQL caído | ERR-001..004; `TC-ERR-001..004` |
| Persistencia | H2 en memoria como único motor de pruebas automatizadas | Pruebas de repositorios y persistencia ejecutadas con H2; SQL Server/Testcontainers no son requisitos de test | UT/IT |
| Contrato | OpenAPI lint/schema, WireMock en portal; provider tests en DEMO | request/response, HMAC, idempotencia, RFC 7807, compatibilidad | SIM-002/004/013, DEMO-11, CT |
| E2E/accesibilidad | Playwright, axe-core | Registro/invitación/CMS; solicitudes/presupuesto; contacto/respuesta; alta conjunta | UIK-009, E2E-001..004; E2E/A11Y |
| Visual | Playwright screenshots | claro/oscuro × móvil/escritorio × ES/EN en páginas clave | UIK-009, HRD-012; VIS |
| Rendimiento/seguridad | Lighthouse CI, pruebas de carga, ZAP baseline, ASVS L2, Dependency-Check/Trivy | Lighthouse ≥90 móvil; LCP <2,5 s; CLS <0,1; INP <200 ms; thresholds aprobados | WEB-007, HRD-001/003; PERF/SEC |

## Gates obligatorios
1. **Build:** Maven wrapper 3.6.3, Java 21 y dependencias versionadas; `.\mvnw.cmd -B verify` ejecuta unitarios, slices y pruebas de persistencia con H2, Modulith, ArchUnit, CT disponibles e integración de recursos front. No se exige Docker, Testcontainers ni SQL Server para pasar los gates.
2. **Límites modulares:** `ApplicationModules.verify()`; reglas ArchUnit `web→application→domain`, entidades JPA solo en `domain`, controladores sin repositorios, `api` sin `web/infrastructure`, dependencias permitidas; fixtures negativos deben fallar.
3. **i18n:** mismas claves ES/EN por bundle/módulo; resolver rutas/locale y fallback probados.
4. **Plantillas:** sin color directo; `th:utext` únicamente desde allowlist; sin script inline sin nonce; no CDN para HTMX/Alpine/Tailwind/fonts.
5. **Seguridad:** autorización por rol+propiedad; CSRF en mutaciones; cookies/headers/CSP; no secretos; tests de IDOR/notas internas; sanitización, subida y descargas.
	- Ingesta de errores same-origin/CSRF/rate-limit, payload cap, scrub y privilegio ADMIN del visor; el JSON se guarda fuera del webroot y SQL.
6. **Calidad:** cobertura mínima 80 % domain/application por módulo; análisis de dependencias con umbral de severidad acordado (FND-008 propone bloquear CVSS ≥7); SBOM y licencias en HRD-013.
7. **Frontend:** Playwright + axe; Lighthouse CI páginas públicas; snapshots matriz visual; `prefers-reduced-motion`, 44px táctil y keyboard path.
8. **Contrato externo:** stubs WireMock en verificación offline; provider/entorno compartido (SIM-013) como job separado, con secreto efímero y datos aislados. No consultar DEMO ni abrir scope externo desde build normal.

## Definition of Done
Una tarea se considera terminada cuando:
- Cumple criterios Given/When/Then del catálogo y la tarea; título, dependencias, estimación y `Cubre` coinciden con `10-roadmap-backlog.md`.
- Tiene unitarios/módulo/integración/E2E adecuados al riesgo, incluyendo al menos una prueba negativa de la regla clave.
- Las pruebas H2 no demuestran compatibilidad con SQL Server. Esa limitación queda documentada como riesgo de producción, pero la verificación MSSQL no es criterio de aceptación de las tareas de desarrollo.
- Mantiene permisos, owner de tabla, APIs/eventos y DAG; eventos no llevan PII/tokens; migración SQL Server reversible/forward-safe según política; errores registrados están saneados y no generan recursión si falla el tracker.
- Si afecta UI, incluye claves ES/EN, tokens claros/oscuros, responsive, teclado/errores accesibles y tests visuales relevantes.
- Actualiza README, INVENTARIO y DIARIO; ADR si cambia decisión; telemetría/logs sin PII; no deja secretos ni dependencias sin revisión.
- CI requerido pasa; excepción documentada, owner y vencimiento si hubiera gate temporalmente justificado.

## Pipeline CI propuesto
La plataforma CI concreta será la aprobada por la organización, pendiente de configuración en FND-008. El pipeline siguiente es agnóstico al proveedor; Dependency-Check/Trivy son ejemplos de SCA sujetos a la política adoptada:
1. Checkout mínimo/permisos `contents: read`; setup JDK 21 y npm versionado.
2. Lint/compile assets locales; checks de claves i18n, plantillas, enlaces internos y formato.
3. `mvn -B verify`: UT/WT/MT y pruebas de persistencia H2; Modulith/ArchUnit; JaCoCo por módulo; `TemplateRulesTest`. No se ejecutan jobs SQL Server/Testcontainers.
4. Dependency-Check/Trivy, secret scan y SBOM; fallar umbral de dependencia aprobado, sin persistir tokens.
5. Publicar artefactos de test, cobertura, contract, a11y/visual y resumen de gates.
6. Jobs separados: ZAP baseline nocturno contra entorno autorizado; Lighthouse/Playwright nocturnos; contract test real contra DEMO solo en entorno compartido con aprobación/secret manager.

## Test data y aislamiento
- Usuarios, emails, tenants, ficheros, audio y claves de prueba sintéticos; no copiar producción ni usar secretos reales.
- `@ApplicationModuleTest` cubre contratos del módulo; fixtures independientes por test. H2 es el único motor de pruebas automatizadas. La compatibilidad con el dialecto y las migraciones SQL Server queda explícitamente sin verificar y no bloquea el DoD.
- WireMock reproduce el borrador OpenAPI aprobado; los tests no cubren comportamiento no documentado de DEMO.
- Reintentos y jobs usan clock controlable; tests de outbox/EPR verifican post-commit, idempotencia y ausencia de side-effects remotos dentro de transacción.
- Las pruebas negativas cubren CSRF, sesión, rol, propiedad, INTERNAL, SSRF, archivo infectado, token vencido/replay, error M2M y payload de telemetría con PII/oversize; las clases de error configuradas se clasifican y no se descartan silenciosamente.

## Trazabilidad de tests
Cada tarea del catálogo incluye `TC-<ID>` en [tareas](tareas/). El índice final relaciona idea→RF/RNF→tarea→TC; requisitos con huecos de desglose quedan explícitos en [13](13-preguntas-abiertas.md), no se inventan IDs nuevos en el roadmap.
