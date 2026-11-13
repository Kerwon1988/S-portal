# S-portal — Plan maestro de desarrollo

**Estado:** plan documental; no es implementación. **Corte:** 2026-11-11. **Alcance:** MVP F0–F6, 170 tareas (156 portal + 14 pista DEMO) y releases posteriores R2/R3+. S-portal es un monolito modular nuevo con Java 21/Boot estable, Thymeleaf, SQL Server 2022 y CMS ES/EN. Integración SimbioSphere por API M2M propuesta; dependencias DEMO no ejecutadas desde este plan.

**Restricción operativa:** Git no se utiliza en ninguna fase de este plan. No se requieren repositorios Git, ramas, worktrees, commits, logs ni `.gitignore`; el avance y las evidencias se registran en los artefactos documentales y resultados observables de build/tests. Las referencias heredadas a Git en este plan quedan sustituidas por esta regla.

## Resumen ejecutivo
El plan cubre web pública, CMS, identidad y cuentas, solicitudes/presupuestos, portal de empleados, contacto/bandeja, alta conjunta de SimbioSphere y registro central de errores backend/frontend/BD. Las tareas son atómicas, ≤1 día, con criterio Given/When/Then, test `TC-<ID>`, owner y dependencias del catálogo. La definición de módulos y el orden de fases están en el [roadmap](10-roadmap-backlog.md); requisitos y prioridades en [02](02-requisitos.md).

Decisiones D01–D12 permanecen cerradas. No se anuncian capacidades/precios no verificados de SimbioSphere; no se escriben contraseñas a DEMO; la activación es just-in-time; no se vincula cuenta solo por email. Los datos sobre DEMO se documentan con evidencia `ruta:línea` en [05](05-integracion-simbiosphere.md); lo externo no verificado queda marcado VERIFICAR y enlazado a tarea.

## Lectura del plan
| Documento | Contenido |
|---|---|
| [01 — Visión y alcance](01-vision-alcance.md) | Objetivos, actores, MVP/R2/R3+, supuestos y decisiones |
| [02 — Requisitos](02-requisitos.md) | 81 RF + 31 RNF, MoSCoW, fase, criterios Given/When/Then |
| [03 — Arquitectura](03-arquitectura.md) | C4, módulos, APIs/eventos, tablas owner, DAG y frontend |
| [04 — Modelo de datos](04-modelo-datos.md) | ER, esquema/columnas, constraints/índices y Flyway |
| [05 — Integración SimbioSphere](05-integracion-simbiosphere.md) | Hechos DEMO citados, contrato propuesto, saga, riesgos y backlog externo |
| [06 — UX/UI](06-ux-ui.md) | Rutas/roles, menú, tema/ratios, i18n, hero y WCAG |
| [07 — Seguridad/RGPD](07-seguridad-rgpd.md) | STRIDE, matriz autorización, OWASP, controles y retención propuesta |
| [08 — Calidad/testing](08-calidad-testing.md) | Pirámide, gates, DoD, CI y test data |
| [09 — Operación/despliegue](09-operacion-despliegue.md) | Entornos, Docker, SQL Server, Actuator, backup y release |
| [10 — Roadmap/backlog](10-roadmap-backlog.md) | Fases, 170 IDs, dependencias, DAG, esfuerzo y R2/R3+ |
| [11 — Riesgos](11-riesgos.md) | R-01..R-16 y riesgos de evidencia/ejecución adicionales |
| [12 — Gobernanza modular](12-gobernanza-modular.md) | P01–P12, esquema de skill/instrucciones y docs vivas |
| [13 — Preguntas abiertas](13-preguntas-abiertas.md) | Defaults, responsables/tareas de cierre y huecos del catálogo |
| [ADRs](../adr/ADR-001-spring-boot-4-1-y-gobernanza-modulith.md) | 12 decisiones en borrador/propuesta, incluida [ADR-012 — Inventario de errores](../adr/ADR-012-inventario-operativo-de-errores.md) |
| [Tareas detalladas](tareas/) | F0–F6 y pista DEMO, una ficha por tarea |

## Matriz de trazabilidad
Convención: cada par `TASK→TC-TASK` enlaza punto de idea a requisitos, tarea del catálogo y test especificado en la ficha. `RF-*` se refiere al ID completo `RF-<área>-NN`; análogamente `RNF-*`. Las decisiones de implementación se detallan en las fichas y no se duplican aquí.

### Idea breve A1–A8
| Idea | Requisitos | Tareas → tests |
|---|---|---|
| A1 Web empresarial/CMS | RF-WEB-01..08, RF-CMS-01..08 | `WEB-001..007→TC-WEB-001..007`; `SEO-001..002→TC-SEO-001..002`; `CMS-001..012→TC-CMS-001..012`; `UIK-005/006/009→TC-UIK-005/006/009` |
| A2 Acceso/alta SimbioSphere | RF-SIM-01..10, RF-IAM-02 | `SIM-001..014→TC-SIM-001..014`; `DEMO-01/02a/02b/03a/03b/03c/04/05/06a/06b/07/08/10/11→TC-DEMO-01/02a/02b/03a/03b/03c/04/05/06a/06b/07/08/10/11`; `CUS-001/002→TC-CUS-001/002` |
| A3 Registro de cliente | RF-IAM-02..05 | `CUS-001/002/004→TC-CUS-001/002/004`; `IAM-002/003/004/005/007→TC-IAM-002/003/004/005/007`; `LGL-001→TC-LGL-001`; `NTF-001/003→TC-NTF-001/003` |
| A4 Alta de trabajadores | RF-IAM-09 | `IAM-010→TC-IAM-010`; `STF-001/002→TC-STF-001/002`; `E2E-001→TC-E2E-001` |
| A5 Gestión de usuarios | RF-USR-01..03 | `USR-001..004→TC-USR-001..004`; `STF-002→TC-STF-002`; `AUD-002→TC-AUD-002`; `LGL-002→TC-LGL-002` |
| A6 Solicitudes cliente | RF-REQ-01..07 | `REQ-001..011→TC-REQ-001..011`; `QUO-001..004→TC-QUO-001..004`; `CMS-013→TC-CMS-013`; `SIM-014→TC-SIM-014` |
| A7 Portal de empleados | RF-EMP-01..05 | `EMP-001..007→TC-EMP-001..007`; `REQ-003/004→TC-REQ-003/004`; `MSG-001/002→TC-MSG-001/002`; `QUO-001..004→TC-QUO-001..004`; `E2E-002→TC-E2E-002` |
| A8 Contacto y comunicación | RF-MSG-01..09, RF-NOT-01 | `MSG-001..013→TC-MSG-001..013`; `NTF-001..004→TC-NTF-001..004`; `E2E-003→TC-E2E-003` |
| Capacidad transversal añadida: registro de errores | RF-ERR-01..02 | `ERR-001..004→TC-ERR-001..004`; extensión solicitada al plan, sin crear un punto nuevo en la idea original |

### Idea detallada I0–I18
| Idea | Requisitos | Tareas → tests |
|---|---|---|
| I0 Responsive, tema, idiomas | RF-UI-01/02/05 | `UIK-002/004/005/006/009→TC-UIK-002/004/005/006/009` |
| I1 Panel y hero/video | RF-WEB-01/02 | `WEB-001/002→TC-WEB-001/002`; `CMS-012→TC-CMS-012` |
| I2 Registro y alta conjunta | RF-IAM-02/09/10, RF-SIM-01..07 | `CUS-001/002→TC-CUS-001/002`; `IAM-006/010→TC-IAM-006/010`; `LGL-001→TC-LGL-001`; `STF-001→TC-STF-001`; `SIM-001/003/004/005/006/007/008/009/010→TC-SIM-001/003/004/005/006/007/008/009/010`; `DEMO-03a/03b/03c/04/05/06a/06b/07/08→TC-DEMO-03a/03b/03c/04/05/06a/06b/07/08` |
| I3 Solicitar desarrollo y sugerencia | RF-REQ-01/02/07 | `REQ-001/005/006/009→TC-REQ-001/005/006/009`; `CMS-013→TC-CMS-013`; `QUO-001→TC-QUO-001` |
| I4 Formulario sencillo de solicitud | RF-REQ-02/03 | `REQ-005/006/007→TC-REQ-005/006/007`; `SIM-014→TC-SIM-014` |
| I5 Listado filtrable de solicitudes | RF-EMP-02, RF-REQ-01 | `EMP-002→TC-EMP-002`; `REQ-001→TC-REQ-001` |
| I6 Panel de empleados | RF-EMP-01/03, RF-UI-07 | `EMP-001/003→TC-EMP-001/003`; `UIK-006→TC-UIK-006`; `AUD-002→TC-AUD-002` |
| I7 Asignar, estado, conversación y presupuesto | RF-EMP-03/04/05, RF-MSG-07 | `EMP-003/004/005/006/007→TC-EMP-003/004/005/006/007`; `REQ-003/004→TC-REQ-003/004`; `MSG-001/002→TC-MSG-001/002`; `QUO-001..004→TC-QUO-001..004` |
| I8 Datos comunes e invitaciones | RF-IAM-02/09 | `CUS-001/002/004→TC-CUS-001/002/004`; `IAM-002/006/010→TC-IAM-002/006/010`; `STF-001→TC-STF-001` |
| I9 Visión de negocio/IA futura | RF-WEB-02 | `CMS-012→TC-CMS-012`; `WEB-001→TC-WEB-001`; IA no activa en MVP |
| I10 Identidad visual, colores y tipografía del sistema | RF-UI-03/04 | `UIK-002/003→TC-UIK-002/003` |
| I11 Relación cliente↔tenant y alta conjunta | RF-SIM-01..07 | `SIM-001/005/006/008/009/012→TC-SIM-001/005/006/008/009/012`; `DEMO-05/06a/06b/07/08→TC-DEMO-05/06a/06b/07/08` |
| I12 About us | RF-WEB-05 | `WEB-005→TC-WEB-005`; `CMS-009→TC-CMS-009` |
| I13 Menú adaptable por rol/viewport | RF-UI-06 | `UIK-007→TC-UIK-007` |
| I14 Contacto y bandeja | RF-MSG-01..09, RF-NOT-01 | `MSG-001..013→TC-MSG-001..013`; `NTF-001..004→TC-NTF-001..004`; `E2E-003→TC-E2E-003` |
| I15 Skill/gobernanza y ADR | RF-GOV-01/02, RF-DOC-04 | `GOV-001..006→TC-GOV-001..006`; `FND-004/008→TC-FND-004/008` |
| I16 README | RF-DOC-01 | `GOV-004→TC-GOV-004` |
| I17 Inventario | RF-DOC-02/05 | `GOV-004/005→TC-GOV-004/005` |
| I18 Diario | RF-DOC-03 | `GOV-004→TC-GOV-004` |

Las tareas DEMO son propuestas para otro repositorio y no se ejecutan con S-portal. Las respuestas del owner y los pendientes externos se registran en [13](13-preguntas-abiertas.md). GOV-006 da cobertura directa a RF-DOC-04; CMS-013 cubre el copy aprobado de condiciones de solicitud.

## Artefactos
- [Requisitos](02-requisitos.md) → [roadmap de 170 tareas](10-roadmap-backlog.md) → [fichas detalladas](tareas/).
- [Riesgos](11-riesgos.md) → [preguntas](13-preguntas-abiertas.md) → [ADRs](../adr/ADR-001-spring-boot-4-1-y-gobernanza-modulith.md).
- [Criterios de calidad y DoD](08-calidad-testing.md) → [gobernanza](12-gobernanza-modular.md) → [seguridad](07-seguridad-rgpd.md).

## Estado de validación del plan
- Requisitos: 112 IDs definidos (81 RF + 31 RNF); cada ID citado por `Cubre` en el catálogo existe en [02](02-requisitos.md).
- Tareas: el validador C debe dar 170/170; IDs/dependencias/títulos/estimaciones/escenarios/TC validados. Longitud de ficha ≤1.700 caracteres.
- Roadmap: validador B debe confirmar 170 nodos, cero ciclos, cero dependencias a fases posteriores y camino crítico recalculado.
- Enlaces: validar rutas Markdown de `docs/plan/**/*.md` y ADR antes de entregar.

## Autoverificación §15
- [x] Todos los puntos de `docs/idea.md` (0º–18º en ambas listas) tienen ruta A/I → requisitos → tareas → tests; R2/R3+ se identifica como futuro.
- [x] Se respetan D01–D12 y no se reabren.
- [x] Las afirmaciones sobre el estado de DEMO tienen `ruta:línea`; lo no verificado está marcado SUPUESTO/VERIFICAR.
- [x] Cada módulo tiene responsabilidad, API/eventos, tablas owner y dependencias; el DAG no tiene ciclos.
- [x] Las familias de páginas/endpoints documentan roles, CSRF, locale, tema y responsive.
- [x] Las 170 tareas incluyen criterios Given/When/Then, `TC-<ID>` y documentación a actualizar.
- [x] Las máquinas de estado y la matriz de autorización están incluidas; supuestos de transición se remiten a 13.
- [x] Los riesgos incluyen probabilidad/impacto/mitigación/disparador y las preguntas tienen opción por defecto.
- [x] El MVP está delimitado y no anuncia capacidades SimbioSphere no verificadas.
- [x] No se escribió código de producción ni se modificó DEMO; los cambios de esta ejecución están en `docs/plan/` y `docs/adr/`.
- [x] El inventario central captura backend, frontend y BD; no depende de SQL Server para registrar errores, no persiste payload sensible y su visor/resolución es ADMIN-only.

La autoverificación confirma cobertura directa GOV-006→RF-DOC-04 y CMS-013→RF-REQ-07; las aprobaciones legales/operativas aún pendientes están identificadas en [13](13-preguntas-abiertas.md).
