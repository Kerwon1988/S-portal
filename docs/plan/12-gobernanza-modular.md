# 12 — Gobernanza modular

**Estado:** borrador de planificación, 2026-11-10. Las reglas P01–P12 adaptan las normas verificadas M01–M12 de DEMO; no se copian sus supuestos tenant-scoped. S-portal es un único desplegable/BD; los datos de SimbioSphere viven en otro sistema. Fuente de evidencia M01–M12: `DEMO-orders-thymeleaf/docs/modules/NORMAS-DESARROLLO-MODULAR.md:1-110`.

**Ejecución de pruebas por cambio:** identifica las clases modificadas, añadidas e impactadas y ejecuta solo sus tests relacionados y los gates obligatorios aplicables. No lances la suite completa por defecto; úsala como último recurso si no existe una selección más acotada y fiable o un criterio de aceptación exige el conjunto completo, y registra el motivo.

## Normas P01–P12
| ID | Norma para S-portal | Gate/evidencia |
|---|---|---|
| P01 — Owner único por cambio | Identificar un módulo owner por feature, fix, refactor, plantilla, datos o config. Si afecta varios, lidera owner del caso de uso/dato; no crear módulo sin decisión y alta en inventario. | PR declara módulo owner; test de inventario detecta módulos sin owner. |
| P02 — Contratos, no detalles | Consumir otro módulo solo por `<owner>.api`/`shared.spi`; no importar entidad JPA, repo, controller, DTO web o infrastructure. Contratos publican IDs/DTOs, nunca entidades. Reportar errores por `shared.spi.ErrorReporter`; `config.errortracking` es el servicio central. | Pruebas de Spring Modulith, ArchUnit de `api`, inventario de APIs/consumers. |
| P03 — DAG sin ciclos | No crear dependencias recíprocas, directas o transitivas. Resolver con evento, SPI o puerto query del owner y documentar decisión si cambia frontera. | Pruebas de arquitectura de Modulith y DAG. |
| P04 — Capas limpias | Dentro del módulo: `web→application→domain`; `infrastructure` implementa puertos. `domain` no depende de infraestructura y `application` no depende de web. | ArchUnit; fixtures negativos para cada regla. |
| P05 — Ownership único de datos | Una entidad/tabla pertenece a un módulo y esquema; migración solo modifica esquema owner. No compartir repositorios/entidades. Relaciones externas por ID/contrato; coordinaciones de migración se justifican. | `entity-table-owners.csv` futuro, test de nomenclatura Flyway y EntityOwnershipTest. |
| P06 — Propiedad explícita del recurso | S-portal no tiene `TenantContext` de DEMO. Casos de uso reciben `userId`/`clientAccountId` autorizado y validan propiedad en query/mutación; IDs de tenant remoto son referencias externas, no autoridad de sesión. | Tests IDOR por recurso/owner; ningún lookup solo por email. |
| P07 — Autorización independiente | Autenticar/seleccionar cuenta no equivale a autorizar acción. Exigir permiso granular más propiedad; proteger endpoint aunque enlace/UI esté oculto. Mutaciones web requieren CSRF. | Matriz IAM-012, `@PreAuthorize`/policy tests, WT/SEC. |
| P08 — Transacciones/eventos explícitos | Definir límite transaccional por caso de uso. Eventos cross-module vía EPR/outbox y payload IDs; handlers idempotentes. HTTP DEMO nunca dentro de transacción local. | MT/IT de post-commit, retry, idempotencia y ausencia de side-effect remoto transaccional. |
| P09 — Probar contrato, ownership y fallo | Cada cambio aporta el código, configuración y tests requeridos aunque una herramienta no esté disponible localmente. Identificar las clases modificadas, añadidas e impactadas y ejecutar solo sus tests relacionados y gates obligatorios aplicables; suite completa solo como último recurso justificado. Posponer solo la ejecución que dependa estrictamente del recurso ausente, registrando herramienta/entorno, motivo e instrucciones reproducibles para otra máquina habilitada. Si toca errores, probar deduplicación, redacción y fallo del propio tracker sin ocultar la excepción original. Si toca archivos, probar virus; si toca solicitudes, PUBLIC/INTERNAL. | `TC-<ID>` con UT/MT/WT/IT/CT/E2E/A11Y/VIS/SEC/PERF y estados `POSTPONED` trazables. |
| P10 — Documentar en el mismo cambio | Actualizar DIARIO e INVENTARIO; README si cambia resumen/arranque; ADR si cambia decisión; reflejar rutas, permisos, eventos, tablas, jobs, gaps y estado. | GOV-004/005; test de enlaces/módulos↔inventario. |
| P11 — Cero excepciones silenciosas | No desactivar ni aflojar gates para «hacer pasar» build; un fallo con herramientas disponibles bloquea tareas dependientes hasta corregirse. Una prueba pospuesta por herramienta ausente no se omite ni se marca verde: se registra con motivo e instrucciones de ejecución futura. Cualquier excepción requiere regla exacta, owner, ticket, caducidad y aprobación explícita. Baselines solo registran mejoras aceptadas. | CI falla ante waiver sin metadatos/owner, test omitido o baseline ampliada; los diferidos quedan trazables y no cuentan como PASS. |
| P12 — DoD modular | Owner identificado; API/DAG y owner de tabla correctos; autorización/PII; tests relacionados con clases impactadas y gates obligatorios ejecutados; documentación actualizada; una tarea/commit atómico ≤1 día. Suite completa solo como último recurso justificado. Un ID con validación obligatoria pospuesta permanece parcial/abierto, aunque se continúa el código y el trabajo independiente. | Checklist DoD, pruebas automatizadas y gates de calidad/a11y/security según riesgo; criterios obligatorios pendientes impiden cerrar el ID. |

## Esquema de `.github/copilot-instructions.md`
Archivo de instrucciones raíz, aplicable a todo el workspace S-portal. Debe ser breve y enlazar la norma completa, no duplicarla.

```markdown
# S-portal — instrucciones de desarrollo

Este repositorio es un monolito modular Java/Thymeleaf. Para todo cambio:
1. Leer `docs/arquitectura/NORMAS-DESARROLLO-MODULAR.md` y la skill `.github/skills/sportal-modular/SKILL.md`.
2. Identificar módulo owner, contrato/API, eventos, tablas, rutas, permisos, i18n/tema y riesgos.
3. Escribir/verificar test focal antes de implementar; respetar P01–P12 y el DAG.
4. No leer/escribir la BD de DEMO ni usar scraping/CSRF; integración solo por contrato M2M.
5. Actualizar README/INVENTARIO/DIARIO y ADR aplicable; no registrar PII/secrets.
6. Identificar las clases modificadas, añadidas e impactadas y ejecutar solo sus tests relacionados, además de los gates obligatorios aplicables. No lanzar la suite completa por defecto; usarla solo como último recurso si no existe una selección más acotada y fiable o un criterio de aceptación la exige, registrando el motivo. En Windows, seleccionar clases Java con `-Dtest=<ClaseTest>` (Surefire) o `-Dit.test=<ClaseIT>` (Failsafe).

No introducir ciclos, dependencias internas, módulos vacíos, waivers, `th:utext` sin allowlist ni endpoints sin autorización por owner.
```

La tarea GOV-002 convertirá este esquema en instrucciones finales, con path real de skill y reglas de seguridad alineadas a 07/08.

## Esquema de skill propia
Ruta prevista: `.github/skills/sportal-modular/SKILL.md`. Trigger: cualquier feature, fix, refactor, migración, cambio de plantilla/config o documentación técnica que afecte producto/gobernanza. La skill guía; tests/CI son la autoridad que bloquea.

```yaml
---
name: sportal-modular
description: >-
  Use for every S-portal feature, fix, refactor, migration, template,
  configuration or modular architecture change.
---
```

Secciones obligatorias:
1. Lecturas: instrucciones raíz, P01–P12, roadmap/requisito/ADR relevantes.
2. Impacto: owner, dependencias/contratos, eventos, tablas, permisos/roles, rutas, claves i18n, tema, ficheros, PII y consumidores.
3. Flujo TDD: test focal → implementación mínima → Modulith/ArchUnit → integración/e2e por riesgo.
4. Convenciones: capas/paquetes, `api`/SPI, Thymeleaf/HTMX, tokens, CSP, bundles, migración SQL Server.
5. Checklist seguridad: autorización rol+propiedad, CSRF, SSRF, XSS, upload, secrets/logging, M2M, privacidad.
6. DoD: tests, gates, README/INVENTARIO/DIARIO, ADR, commit atómico ≤1 día.
7. Parar/preguntar: nuevo módulo/edge, owner ambiguo, ciclo irresoluble, dato sensible/acción destructiva o excepción requerida.

## Plantillas documentación viva

### README raíz
```markdown
# S-portal

## Propósito
<estado PLANIFICADO mientras el producto no esté implementado>

## Arquitectura
<resumen + enlace a docs/plan/03-arquitectura.md>

## Módulos
| Módulo | Responsabilidad | Estado | Diseño |

## Inicio local
<requisitos, configuración sin secretos, Compose y puertos>

## Tests y gates
<comandos aprobados por FND-001/008>

## Documentación
<plan, ADR, inventario, diario, operaciones>
```

### INVENTARIO (`docs/INVENTARIO.md` o `docs/inventario/`)
```markdown
| Tipo | Identificador | Owner | Rutas/API/tabla/evento/test | Roles/locale/tema | Estado | Evidencia |
```

Incluir módulos, páginas/rutas, endpoints, entidades/tablas, eventos, integraciones, properties/jobs/emails/permisos, tests y brechas. Estados únicos: `PLANIFICADO`, `EN_CURSO`, `HECHO`, `BLOQUEADO`, `OBSOLETO`. El inventario describe qué existe hoy, no qué falta.

### DIARIO raíz
```markdown
## YYYY-MM-DD
- Tarea: <ID> | Módulo: <owner> | Tipo: feat/fix/refactor/docs/chore
- Resumen: <cambio verificable> | Estado: <estado>
- Evidencia: <test/commit/PR> | ADR: <id o n/a>
```

Orden cronológico inverso; una entrada por cambio integrado. No copiar el inventario ni registrar secretos/PII. Mientras no haya implementación, documentación del plan debe distinguir «planificado» de «hecho».

## Gates y gobierno
- Pruebas de Spring Modulith y reglas ArchUnit de capas/dependencias; un esquema/tablas owner por módulo.
- Test de contrato Inventario↔Modulith y diagramas `Documenter` (GOV-005).
- Pruebas del inventario de errores para persistencia fuera de SQL, payload frontend acotado, redacción y acceso ADMIN (ERR-001..004); el tracker no debe crear recursión ni almacenar PII/raw SQL.
- Test paridad i18n, ratios de contraste, reglas plantillas/`th:utext`/nonce, cobertura ≥80 % domain/application, escaneo deps/secrets.
- No se copia el gate DEMO tenant-isolation/M06; S-portal prueba aislamiento de `ClientAccount`/propiedad y deja la frontera DEMO en API M2M.

## Referencia DEMO y límites de adaptación
M01–M12 verificadas están en `DEMO-orders-thymeleaf/docs/modules/NORMAS-DESARROLLO-MODULAR.md:1-110`. Se adaptan owner, contratos, DAG, capas, datos, autorización, eventos, pruebas, documentación, excepciones y DoD; se sustituyen reglas de tenant por ownership de cliente/recurso. No alterar normas M01–M12 ni archivos del repo DEMO desde esta planificación.
