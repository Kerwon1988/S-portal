# Paquete de prompts para desarrollar S-portal en paralelo

**Propósito:** prompts listos para copiar/pegar para construir S-portal por carriles independientes, respetando el backlog aprobado, el monolito modular y la preparación verificable de LOCAL y PRE. Cada bloque `PROMPT` es autónomo: puede copiarse sin conservar el contexto de esta conversación. Ejecútalo con el workspace abierto en el repositorio indicado.

**Fuente de verdad funcional y técnica:** `docs/prompt-plan-desarrollo.md`, `docs/idea.md` y `docs/plan/`. Para tareas, son autoridad `docs/plan/10-roadmap-backlog.md` y la ficha enlazada bajo `docs/plan/tareas/`. Este documento organiza la ejecución; no reemplaza ni modifica esos requisitos.

**Restricción operativa global:** no se usa Git en ninguna fase del plan. No compruebes ni requieras repositorio, rama, worktree, commit, log ni `.gitignore`; registra el avance con evidencia observable en los artefactos y pruebas del workspace. Esta regla prevalece sobre cualquier referencia heredada a Git en las fichas y prompts.

**Regla de Maven:** los agentes solo pueden invocar Maven para compilar (`compile`), ejecutar pruebas (`test`) o arrancar la aplicación (`spring-boot:run`). Usa JDK 21 y las dependencias locales proporcionadas por el usuario. No ejecutes metas de descarga, inspección o gestión de dependencias/caché, metas directas de plugins, `clean`, `install` u otros objetivos. No investigues ni comuniques registros de origen/metadata de la caché local, ni solicites volver a descargar dependencias. Si una operación básica no llega a ejecutarse, registra únicamente que compilación/pruebas/arranque no se ejecutaron y continúa con el trabajo independiente. No omitas ni desactives gates.

**Política transversal de implementación y pruebas:** desarrolla el código, configuración y tests requeridos aunque el servicio/herramienta de validación no esté disponible en esta máquina. Identifica las clases modificadas, añadidas e impactadas y ejecuta solo sus tests relacionados y los gates obligatorios aplicables; no ejecutes la suite completa por defecto. La suite completa es el último recurso si no existe una selección más acotada y fiable o un criterio de aceptación exige todos los tests; documenta por qué se necesitó. Un fallo relacionado bloquea avanzar a tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, registrando caso, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarlo en otra máquina habilitada como `POSTPONED` (o `PREPARADO_NO_EJECUTADO` para PRE), nunca como `PASS`. La ausencia no bloquea implementar ni continuar trabajo que no dependa de ese resultado, pero el ID queda parcial/abierto hasta completar cualquier validación obligatoria. La compatibilidad SQL Server y las comprobaciones externas siguen sujetas a sus gates específicos.

**Estrategia de pruebas:** H2 es el único motor de pruebas automatizadas; no se requieren Docker, SQL Server, Testcontainers ni evidencia de VPS para cerrar tareas o gates de desarrollo. Se mantiene un único árbol Flyway dirigido a SQL Server para producción, pero sus migraciones y la compatibilidad real con ese motor quedan sin verificar y no se declaran certificadas.

## Cómo usar los prompts

1. Ejecuta primero el **Prompt 00** para que el agente coordinador confirme el estado, las dependencias y las colisiones de ficheros. Ejecuta el **Prompt 01** antes de repartir en paralelo trabajo que cree rutas MVC/HTMX, contratos HTTP o formularios.
2. No lances a la vez un prompt cuyo prerrequisito no esté terminado. El agente debe comprobarlo en el backlog y en los cambios integrados, no inferirlo por el número de fase.
3. Asigna un único agente como owner de cada carril; no ejecutes dos copias del mismo prompt simultáneamente. Integra los cambios en el workspace coordinado y pasa gates antes de abrir dependencias a la siguiente oleada.
4. Los prompts F0–F6 operan exclusivamente en S-portal. El prompt DEMO es una pista distinta, opcional y condicionada a autorización explícita y a disponer del workspace DEMO correcto. No mezcles ambos productos en un mismo cambio.
5. El **Prompt 18** es el gate final LOCAL/PRE. El **Prompt 17** es únicamente la pista opcional de cambios en DEMO, en otro repositorio y con autorización. No confundas ambos ni pruebes PRE/producción sin permiso.
6. La estimación de paralelismo no equivale a capacidad real: respeta dependencias, ownership de archivos, pruebas H2 y capacidad de revisión/integración.

## Orden de ejecución recomendado

**Oleada 0:** Prompt 00 → Prompt 01

**Oleada 1:** Prompt 02

**Oleada 2:** Prompt 03, Prompt 04

**Oleada 3:** Prompt 05, Prompt 06, Prompt 08

**Oleada 4:** Prompt 07, Prompt 09, Prompt 10, Prompt 11

**Oleada 5:** Prompt 12

**Oleada 6:** Prompt 13, Prompt 14, Prompt 15

**Oleada 7:** Prompt 16

**Oleada 8:** Prompt 18

**Opcional, en paralelo desde que esté aprobado SIM-002:** Prompt 17 (solo en el repositorio DEMO y con autorización).

> Dentro de cada oleada, respeta las dependencias de las tareas del backlog. Si una tarea aún no está habilitada, espera a que termine su prerequisito; no hace falta esperar para iniciar las demás tareas independientes de esa oleada.

---

## PROMPT 00 — Coordinador de ejecución y paralelismo

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Identifica las clases modificadas, añadidas e impactadas y ejecuta solo sus tests relacionados, además de los gates obligatorios aplicables; no ejecutes la suite completa por defecto. La suite completa es el último recurso si no hay una selección fiable más acotada o el criterio de aceptación la exige expresamente; registra el motivo. Un fallo relacionado bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Actúa como coordinador técnico principal de S-portal. Trabajas en el repositorio S-portal abierto en el workspace; no des por supuesto que una tarea previa está hecha solo porque exista en el plan.

CONTEXTO AUTÓNOMO DEL PRODUCTO
S-portal es una aplicación nueva e independiente de SimbioSphere, implementada como MONOLITO MODULAR server-side. Su objetivo es la web corporativa bilingüe de Simbio, CMS, acceso/registro de clientes y trabajadores, solicitudes/presupuestos, contacto/bandeja e integración M2M con SimbioSphere. El stack objetivo es Java 21, Spring Boot estable soportado al iniciar, Spring MVC, Thymeleaf, HTMX, Alpine CSP y Tailwind local, Spring Security, JPA/Flyway, Spring Modulith, ArchUnit y SQL Server 2022. El `groupId` y paquete raíz son `com.simbio.core`; `artifactId` provisional `portal` debe validarse en FND-002.

MONOLITO MODULAR NO NEGOCIABLE
Un único desplegable y una aplicación/BD, no microservicios ni frontend SPA. Cada módulo posee sus tablas y migraciones/esquema; acceso inter-módulo únicamente por `api`/named interfaces o SPI, nunca repositorios/entidades/plantillas ajenas. Capas `web → application → domain`; infraestructura implementa puertos. Eventos Modulith solo con IDs, sin PII, secretos ni tokens. Thymeleaf genera páginas y fragmentos HTML; HTMX sirve interacciones parciales. Seguridad por defecto, autorización por rol y propiedad, CSRF en mutaciones, validación servidor, i18n ES/EN, accesibilidad WCAG 2.2 AA.

FUENTES Y DECISIONES
Lee `docs/prompt-plan-desarrollo.md`, `docs/idea.md`, `docs/plan/00-INDICE.md`, `02-requisitos.md`, `03-arquitectura.md`, `04-modelo-datos.md`, `05-integracion-simbiosphere.md`, `06-ux-ui.md`, `07-seguridad-rgpd.md`, `08-calidad-testing.md`, `09-operacion-despliegue.md`, `10-roadmap-backlog.md`, `11-riesgos.md`, `12-gobernanza-modular.md`, `13-preguntas-abiertas.md` y las fichas de tareas afectadas. Prevalecen las decisiones registradas; no inventes aprobaciones, URLs, secretos o capacidades externas. La integración DEMO es HTTPS M2M mediante contrato, nunca acceso directo a su base de datos ni scraping.

ALCANCE DE ESTE PROMPT
No implementes una feature de negocio todavía. No se usa Git: no busques ni requieras repositorio, ramas, worktrees, commits o logs. Inspecciona el workspace y clasifica cada tarea del roadmap como pendiente/en curso/hecha/bloqueada con evidencia observable en archivos y pruebas; detecta cambios ajenos y dependencias sin satisfacer, e identifica conflictos de archivos entre carriles. No reviertas trabajo existente ni hagas cambios destructivos.

Entrega una propuesta de oleadas con: IDs listos según DAG, prompt/ficha correspondiente, agente owner, prerrequisitos, rutas/áreas que probablemente modificará y riesgo de colisión. Asigna un solo integrador y coordina los cambios sobre el workspace compartido sin Git. No declares una tarea hecha sin que sus criterios de aceptación y pruebas estén comprobados. Si los contratos UI↔backend aún no están detallados para rutas/acciones del backlog, ordena ejecutar primero el Prompt 01 para los flujos que vayan a paralelizarse. No lances agentes ni cambies de workspace/repositorio sin autorización explícita. Resume bloqueos y el siguiente lote seguro.
```

## PROMPT 01 — Contratos UI↔backend y reparto de ownership

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Actúa como arquitecto/a de aplicación y diseñador/a de contratos de S-portal. Trabajas en el repositorio S-portal; este encargo es de diseño/documentación previo o simultáneo a la implementación, no una autorización para cambiar código de producción.

CONTEXTO AUTÓNOMO DEL PRODUCTO
S-portal es nuevo e independiente y debe construirse como MONOLITO MODULAR Java 21/Spring Boot estable. Su UI usa Spring MVC, Thymeleaf y HTMX; no es una SPA ni necesita una API REST pública interna por defecto. Un formulario/navegación recibe HTML, un redirect o un fragmento HTML; JSON se reserva para los contratos que lo necesitan (p. ej., ingesta de telemetría y API M2M). SQL Server 2022 es la base relacional. La raíz Java es `com.simbio.core`, con módulos bajo esa raíz. Un solo desplegable: no crear microservicios.

REGLAS DE DISEÑO
Lee `docs/prompt-plan-desarrollo.md`, `docs/idea.md`, los requisitos `docs/plan/02-requisitos.md`, arquitectura `03-arquitectura.md`, modelo de datos `04-modelo-datos.md`, UX/UI `06-ux-ui.md`, seguridad `07-seguridad-rgpd.md`, calidad `08-calidad-testing.md`, roadmap `10-roadmap-backlog.md`, preguntas `13-preguntas-abiertas.md` y las fichas de las funcionalidades consideradas. No alteres requisitos aprobados ni repitas decisiones cerradas. Para cada flujo identifica páginas y roles, módulo owner, ruta/método HTTP, respuesta (HTML/redirect/fragmento HTMX/JSON solo si procede), campos/DTO, validaciones, permiso/ownership, CSRF, estados vacíos/errores, claves i18n y pruebas. Evita duplicar endpoints o lógica entre módulos.

MONOLITO MODULAR NO NEGOCIABLE
Controladores de módulo invocan casos de uso; no acceden a repositorios de otros módulos. Los contratos publicados pertenecen a `api`; los eventos contienen solo IDs. El servidor valida toda mutación. HTMX no reduce requisitos de seguridad ni accesibilidad. No expongas entidades JPA en contratos de transporte. La integración SimbioSphere sale desde el backend por cliente HTTP tipado al contrato M2M; el browser no llama a DEMO y el portal nunca accede a su BD.

TAREA
Revisa si ya existe un mapa suficiente y actualizado. Si falta, crea/actualiza `docs/plan/contratos-ui-backend.md` con una fila por ruta/acción incluida en MVP, enlazando RF, tarea, módulo owner, request/response, autorización, validación, error esperado y pruebas. Incluye contratos externos solo desde `05-integracion-simbiosphere.md`. Añade un mapa de ownership por archivo/paquete para minimizar conflictos entre agentes. No inventes datos de producto: etiqueta SUPUESTO o remite al pendiente de `13-preguntas-abiertas.md`. No crees nuevos requisitos ni tareas en el roadmap por decisión unilateral; lista huecos para que el coordinador los resuelva.

Valida enlaces/IDs y comprueba que cada operación corresponde a requisitos y tareas existentes. Informa rutas añadidas/actualizadas, decisiones derivadas, huecos, y qué carriles ya pueden trabajar en paralelo sin editar los mismos contratos o ficheros.
```

## PROMPT 02 — F0 Fundaciones (FND)

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Implementa exclusivamente las tareas asignadas de la ficha `docs/plan/tareas/F0-fundaciones.md`: FND-001..FND-010. Trabaja en S-portal y no modifiques el repositorio DEMO.

CONTEXTO AUTÓNOMO
S-portal es una aplicación nueva de Simbio, un MONOLITO MODULAR server-side; un único despliegue, Java 21, Spring Boot estable soportado (no RC), Spring MVC/Thymeleaf/HTMX, Spring Security, Spring Data JPA/Flyway, Spring Modulith y ArchUnit. H2 es el único motor de pruebas; SQL Server 2022 sigue como destino productivo, sin compatibilidad validada. Paquete raíz `com.simbio.core`, `artifactId` provisional `portal`. La fundación debe permitir módulos con ownership claro y un build reproducible.

REGLAS MODULARES
Lee `docs/prompt-plan-desarrollo.md` y `docs/plan/03-arquitectura.md`, `04-modelo-datos.md`, `08-calidad-testing.md`, `09-operacion-despliegue.md`, `10-roadmap-backlog.md`, ADRs relacionados y la ficha completa. Mantén `web→application→domain`; controladores no acceden a repositorios; módulos se comunican por `api`/SPI, no por entidades/repositorios; no crees microservicios, un segundo árbol H2 ni migraciones destructivas. Todas las pruebas de persistencia usan H2 con esquema generado; no se exige verificar SQL Server/Testcontainers. Un esquema y owner por módulo. Contratos y dependencias han de pasar Modulith y ArchUnit.

SEGURIDAD/LOCAL
No guardes secretos en Git ni en Compose; usa variables de entorno y `.env.example` sin valores reales. Perfil local: app 8090 y H2 para las pruebas; no ejecutar Docker/SQL Server. Compose describe servicios potenciales, pero no se exige arrancarlos. Usa JDK 21 y Maven solo para compilar, ejecutar pruebas o arrancar la aplicación; las dependencias locales las proporciona el usuario. Si Maven no inicia, deja constancia únicamente de que la operación no se ejecutó y continúa lo que no dependa de Maven. No investigues ni informes metadata de la caché. No declares SQL Server como validado. Respeta las preguntas pendientes de licencia/edición SQL Server.

EJECUCIÓN
Comprueba en el roadmap y la ficha las dependencias exactas de cada FND antes de tocarla. Ejecuta las tareas en orden topológico; FND-001 y FND-002 son gates previos a los demás módulos. Define el esqueleto Maven, paquetes y gates; Compose con SQL Server 2022, Mailpit y ClamAV se valida estáticamente, sin levantar servicios. Mantén una sola estrategia Flyway para SQL Server; H2 usa esquema generado y no contiene migraciones. Maven offline solo para compilar, ejecutar pruebas o arrancar la aplicación; no uses Maven para descargar o inspeccionar dependencias. No alteres funcionalidad de otros carriles. Coordina previamente si se tocarán build, configuración, migraciones o archivos compartidos.

Para cada ID: implementa el criterio de aceptación literal, añade el test indicado `TC-<ID>`, ejecuta el test/gate mínimo y actualiza la documentación indicada por la ficha (README/INVENTARIO/DIARIO/ADR). No marques tareas completas por haber compilado parcialmente. No ejecutes limpieza destructiva contra bases persistentes. Al cerrar, informa IDs y evidencias, comandos/resultados, ficheros tocados, migraciones, bloqueos y dependencias liberadas.
```

## PROMPT 03 — F0 UI Kit y gobernanza (UIK/GOV)

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Implementa exclusivamente UIK-002..UIK-009 y GOV-001..GOV-006 de `docs/plan/tareas/F0-ui-kit-y-gobernanza.md`, sujeto a sus dependencias del roadmap. No cambies código del repositorio DEMO.

CONTEXTO AUTÓNOMO
S-portal es un MONOLITO MODULAR de Simbio, no una SPA ni microservicios. Stack objetivo: Java 21, Spring Boot estable, MVC, Thymeleaf, HTMX, Alpine CSP y Tailwind compilado/local; SQL Server 2022. Raíz `com.simbio.core`; un único despliegue. Esta pista establece UI común e instrucciones/gates para que los demás módulos integren sus vistas sin invadir sus datos.

REGLAS
Consulta `docs/prompt-plan-desarrollo.md`, `docs/idea.md`, `docs/plan/03-arquitectura.md`, `06-ux-ui.md`, `07-seguridad-rgpd.md`, `08-calidad-testing.md`, `10-roadmap-backlog.md`, ADRs y la ficha completa. Usa layouts `public`, `account`, `backoffice`; templates por módulo y fragments compartidos; claves ES/EN por módulo. Accesibilidad WCAG 2.2 AA, móvil, claro/oscuro, CSP nonce y mejora progresiva. HTMX debe llevar CSRF, conservar foco y mostrar errores accesibles. Sin CDN ni scripts inline inseguros. No permitas imports cruzados de plantillas/repositorios. `NavContribution` se compone desde config y permisos se verifican en endpoints.

EJECUCIÓN
Comprueba prerrequisitos por tarea; no ejecutes GOV antes de las dependencias FND indicadas. Respeta tokens y licencias pendientes; no inventes ni copies assets sin autorización. El pipeline local de frontend existente se conserva, pero su integración Maven/`npm ci` no forma parte del alcance ni es criterio de cierre de este prompt. Implementa layouts, tema, i18n, menú accesible, componentes básicos, E2E/axe según ficha y reglas documentales. No introduzcas componentes de negocio que pertenecen a otros carriles.

Añade tests y claves bilingües de tu alcance; ejecuta los validadores aplicables de plantillas, i18n, contraste, teclado y pruebas E2E/axe definidas en las fichas. Actualiza solo la documentación asignada en las fichas. Si otra pista debe crear un fragmento o ruta, publica el contrato/ejemplo común sin tomar ownership de sus templates. Reporta IDs completados, evidencia, dependencias liberadas, conflictos y pendientes.
```

## PROMPT 04 — F0 Inventario central y captura de errores (ERR-001..003)

**Estado en este workspace: COMPLETADO el 2026-11-13.** ERR-001..003 y sus criterios de desarrollo quedaron validados localmente. Las comprobaciones contra SQL Server real, despliegue multi-réplica, retención productiva y revisión legal no son gates para cerrar este prompt; sus decisiones siguen pendientes antes de producción.

```text
Si este encargo incluye implementación, desarrolla el código/configuración y prepara los tests requeridos aunque el servicio o herramienta no esté disponible. Identifica las clases modificadas, añadidas e impactadas y ejecuta solo sus tests relacionados y los gates obligatorios aplicables; no ejecutes la suite completa por defecto. La suite completa es el último recurso si no existe una selección más acotada y fiable o un criterio de aceptación exige todos los tests; registra el motivo. Un fallo relacionado bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente: registra caso, herramienta/entorno, motivo y evidencia pendiente como `POSTPONED` (o `PREPARADO_NO_EJECUTADO` para PRE), nunca `PASS`. La ausencia no bloquea implementar ni trabajo que no dependa de ese resultado, pero el ID queda parcial/abierto hasta completar cualquier validación obligatoria.

Implementa exclusivamente ERR-001, ERR-002 y ERR-003 de `docs/plan/tareas/F0-error-inventory.md`, respetando dependencias exactas. No modifiques DEMO ni reemplaces el tracker con CustomErrorController.

CONTEXTO AUTÓNOMO
S-portal es un MONOLITO MODULAR Spring Boot/Thymeleaf, Java 21, SQL Server 2022, raíz `com.simbio.core`, un único despliegue. El inventario central reside en `config.errortracking.ErrorInventoryService`; el puerto publicado es el SPI mínimo `shared.spi.ErrorReporter`. `CustomErrorController`/`/error` atiende o despacha errores servlet y delega; no es el registrador central.

DISEÑO Y SEGURIDAD
Lee `docs/plan/03-arquitectura.md`, `05-integracion-simbiosphere.md`, `07-seguridad-rgpd.md`, `08-calidad-testing.md`, `09-operacion-despliegue.md`, ADR-012, `10-roadmap-backlog.md` y la ficha. Captura backend, frontend y fallos SQL con clasificación, huella estable, severidad, estado, marcas de tiempo, ocurrencias y contexto técnico saneado para diagnóstico. El JSON queda fuera de webroot y SQL en volumen privado/single-writer. El tracker debe seguir funcionando si SQL cae. No almacenar secretos, PII en bruto, tokens, body, headers, cookies, formularios, DOM ni valores SQL; sanitiza/trunca stack y causa. El endpoint frontend es same-origin, CSRF, rate-limited y de tamaño limitado. El fallo del tracker nunca sustituye la excepción original ni entra en recursión.

IMPLEMENTACIÓN
Mantén el diseño en `config` y su SPI; no introduzcas tablas ni dependencias de negocio. Asegura que errores esperados no generen ruido descontrolado y que clasificación/deduplicación no silencien categorías configuradas. No registres datos sensibles al depurar.

Prueba los criterios `TC-ERR-001..003`: UT de huella/redacción; IT JSON/restart/fallos usando H2 y fixtures de indisponibilidad SQL cuando aplique (sin exigir SQL Server real); WT handlers/CSRF/origin/rate-limit; prueba de secretos/PII; E2E de captura frontend según ficha. Docker, Testcontainers, SQL Server real, PRE y aprobaciones legales/operativas de producción no son gates de cierre de ERR-001..003; conserva sus límites como pendientes de producción. Actualiza documentación y ADR solo si la ficha lo pide. Reporta límites, estrategia de volumen y validación de single-writer.
```

## PROMPT 05 — F1 Auditoría, ficheros e identidad núcleo

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Implementa exclusivamente AUD-001, AUD-002, FIL-001, FIL-002, FIL-003, FIL-004, IAM-001, IAM-002, IAM-004, IAM-005, IAM-012 e IAM-014 de `docs/plan/tareas/F1-audit-files-identidad.md`, solo cuando las dependencias del DAG estén satisfechas.

CONTEXTO AUTÓNOMO
S-portal es una aplicación nueva, MONOLITO MODULAR server-side de Simbio: Java 21, Spring Boot estable, MVC/Thymeleaf/HTMX, Spring Security, JPA/Flyway, Modulith/ArchUnit y SQL Server 2022. Raíz `com.simbio.core`, un solo artefacto desplegable. Roles previstos incluyen ADMIN, EMPLOYEE y CLIENT/OWNER; no se confía en ocultar enlaces como control de acceso.

REGLAS MODULARES Y DE DATOS
Consulta `docs/plan/03-arquitectura.md`, `04-modelo-datos.md`, `05-integracion-simbiosphere.md`, `07-seguridad-rgpd.md`, `08-calidad-testing.md`, `10-roadmap-backlog.md`, matriz de permisos y ficha. Cada módulo es owner de sus tablas. `identity`, `audit` y `files` publican contratos mínimos `api`; ningún controlador accede a repositorios ajenos. Producción y migraciones se dirigen a SQL Server/Flyway; todas las pruebas automatizadas usan H2 con esquema generado, sin árbol Flyway propio. No se requieren pruebas SQL Server/Testcontainers.

SEGURIDAD
Auditoría inmutable con datos mínimos; no incluir credenciales, tokens ni payload sensible. Descargas autorizadas y ownership comprobado, nombre aleatorio, validación de tipo/tamaño, almacenamiento fuera de webroot, antivirus/ cuarentena y `Content-Disposition`/`nosniff`. Contraseñas con encoder aprobado; sesión/cookie segura; bloqueo progresivo; permisos default-deny. El bootstrap del primer ADMIN no puede incluir credenciales en repositorio, imagen, logs ni documentación.

EJECUTA TAREA A TAREA
Verifica prerequisitos y limita cambios al ownership del paquete. Implementa APIs y pruebas negativas (autorización/IDOR, descarga cruzada, archivo inválido, bootstrap no autorizado). No cambies las pantallas CMS, salvo contratos mínimos requeridos y acordados con su agente. Ejecuta los TC de la ficha y la suite automatizada con H2; no se requieren tests MSSQL/Testcontainers.

CRITERIO DE CIERRE Y BLOQUEO LOCAL DE MAVEN
Para cerrar Prompt 05 deben estar implementadas las tareas incluidas, cumplidos sus criterios funcionales y de seguridad, y pasar la compilación Java 21 y todos los tests requeridos. Enforcer no es un gate adicional de cierre para este prompt; registra su resultado por separado si se ejecutó. Si una operación básica no llega a compilar o ejecutar los tests, los IDs afectados permanecen abiertos; no investigues ni comuniques detalles de metadata/origen local y continúa el trabajo independiente. Usa Maven únicamente para compilar, ejecutar pruebas o arrancar la aplicación. No inspecciones ni repares la caché ni descargues dependencias; el usuario las proporciona. No afirmes que compilación o tests pasaron sin evidencia.

Actualiza README/INVENTARIO/DIARIO según corresponda. Informa IDs, permisos y migraciones creadas, evidencias, rutas de archivo, dependencias pendientes de proporcionar y todo riesgo no resuelto.
```

## PROMPT 06 — F1 CMS bilingüe (CMS-001..013)

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Implementa exclusivamente CMS-001..CMS-013 de `docs/plan/tareas/F1-cms.md`, respetando dependencias e integrando con los contratos del Prompt 01. No cambies DEMO.

CONTEXTO AUTÓNOMO
S-portal es un MONOLITO MODULAR nuevo de Simbio, no microservicios ni SPA. Java 21, Spring Boot estable, MVC/Thymeleaf/HTMX, SQL Server 2022 y Flyway; raíz `com.simbio.core`; módulo owner `content`. La web pública es ES/EN; CMS ligero para home, servicios, SimbioSphere, sobre nosotros/equipo, legales y SEO. El plan evita anunciar capacidades o cobros de SimbioSphere que no estén verificados.

ARQUITECTURA Y SEGURIDAD
Lee `docs/prompt-plan-desarrollo.md`, `docs/idea.md`, `docs/plan/02-requisitos.md`, `03-arquitectura.md`, `04-modelo-datos.md`, `05-integracion-simbiosphere.md`, `06-ux-ui.md`, `07-seguridad-rgpd.md`, `08-calidad-testing.md`, `10-roadmap-backlog.md` y la ficha. `content` es owner de sus tablas, publicaciones, traducciones, bloques, versión, slug y equipo. Otros módulos solo consumen `content.api`; no acceden a repositorios/entidades ni a templates internos. Validar `CONTENT_EDIT` en cada endpoint, audit trail, preview no público, CSRF, sanitización allowlist antes de `th:utext`, controles de archivo y claves ES/EN.

ALCANCE
Implementa editor, publicación/vista previa, versiones/restauración, render por slug/locale, fallback/redirects, biblioteca de medios con alt, documentos legales, traducciones pendientes, contenido semilla y copy bilingüe de gratuidad/condiciones. Los textos legales siguen en borrador hasta aprobación. El inventario de assets/licencias sigue sus pendientes; no uses recursos no autorizados. No inventes copy factual de capacidades/precios SimbioSphere.

Prueba por ID (`TC-CMS-001..013`), incluidos control de permisos, XSS/sanitización, locale, publicación, restauración y medios. Ejecuta la suite automatizada con H2; no se requieren pruebas SQL Server/Testcontainers ni evidencia de VPS. Integra con WEB/UI solo por contratos acordados. Reporta migraciones, API publicada, pruebas, documentación actualizada y contenidos que requieren revisión humana; deja constancia de que las migraciones SQL Server no se han ejecutado.
```

## PROMPT 07 — F1 Visor ADMIN de errores (ERR-004)

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Implementa exclusivamente ERR-004 de `docs/plan/tareas/F1-error-inventory.md`. Conserva los prerrequisitos ERR-001..003, IAM-012 y UIK-008 tal como están declarados en el DAG; no añadas una fase separada para volver a verificar esas dependencias. Los criterios y pruebas propios de ERR-004 sí son obligatorios para declarar su cierre.

CONTEXTO AUTÓNOMO
S-portal es MONOLITO MODULAR, Java 21/Spring Boot estable, Thymeleaf/HTMX, SQL Server 2022, un solo desplegable. El registro central es `config.errortracking.ErrorInventoryService` con JSON privado fuera de webroot/SQL y SPI `shared.spi.ErrorReporter`. `/error` y `CustomErrorController` no son el inventario; solo despachan y delegan.

SEGURIDAD Y ARQUITECTURA
Consulta `docs/plan/03-arquitectura.md`, `06-ux-ui.md`, `07-seguridad-rgpd.md`, `08-calidad-testing.md`, ADR-012, `10-roadmap-backlog.md` y ficha. La UI vive en el área ADMIN/gestión; solo ADMIN con permiso dedicado puede consultar, resolver o eliminar conforme al contrato. CLIENT/EMPLOYEE no acceden. Cada endpoint comprueba autorización en servidor y audita la resolución. Lista con filtros source/severity/status y detalle saneado; no expongas PII, secretos, body, SQL raw ni stack sin sanitizar. La vista no debe escribir directamente el archivo ni depender de tablas SQL.

EJECUCIÓN
Integra con el servicio/DTO existentes; no dupliques registrador ni cambies formatos sin compatibilidad. Implementa fragmentos Thymeleaf/HTMX con CSRF, i18n ES/EN, estados accesibles y paginación/filtros según ficha. Añade pruebas ADMIN permitida, otros roles denegados, filtrado y redacción. Las dependencias Maven proporcionadas para este workspace están en `C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias`; usa `-Dmaven.repo.local` con esa ruta y Maven offline para compilar o ejecutar tests. Ejecuta pruebas del módulo y gates. Informa evidencia, permisos, archivo JSON y dependencias de despliegue/localidad.
```

## PROMPT 08 — F1 Web pública, SEO y contrato inicial SimbioSphere

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Implementa WEB-001..WEB-007, SEO-001..SEO-002 y SIM-002 de `docs/plan/tareas/F1-web-seo-contrato.md`, respetando cada dependencia. Si el Prompt 01 identificó contratos UI↔backend ausentes, resuélvelos antes de crear rutas.

CONTEXTO AUTÓNOMO
S-portal es el MONOLITO MODULAR nuevo de Simbio, una web corporativa server-side Java 21/Spring Boot estable, Spring MVC, Thymeleaf y HTMX; SQL Server 2022. Un solo despliegue. El sitio público es bilingüe ES/EN, gestionado por el módulo `content`; páginas previstas: Home, SimbioSphere, desarrollo a medida/servicios, sobre nosotros/equipo, contacto y legales. No es SPA ni un segundo frontend con API pública propia.

REGLAS DE CONTENIDO Y MÓDULOS
Lee fuentes maestras, `docs/plan/02-requisitos.md`, `03-arquitectura.md`, `05-integracion-simbiosphere.md`, `06-ux-ui.md`, `07-seguridad-rgpd.md`, `08-calidad-testing.md`, `10-roadmap-backlog.md` y la ficha. Public pages leen solo contenido publicado por `content.api`; nunca consultan tablas ajenas. Implementa responsive, teclado, tema, i18n, SEO, canonical/hreflang, sitemap/robots/JSON-LD conforme a RF. No anuncies módulos/precios/cobros SimbioSphere no verificados ni visión futura de IA como funcionalidad existente. Assets y textos legales requieren las aprobaciones indicadas.

CONTRATO PORTAL↔DEMO
SIM-002 define un OpenAPI M2M futuro bajo `/api/platform/v1/**`, consumido desde el backend. No llames a DEMO desde el navegador, no accedas a su BD, no hagas scraping ni uses sus credenciales. Mantén secretos fuera de YAML/Git; especifica HMAC/idempotencia/Problem Details según ADR/05. La pista DEMO es separada. Si el contrato no puede verificarse en DEMO, marca “propuesto/no publicado” y genera WireMock para portal, sin afirmar integración real.

MAVEN Y VALIDACIÓN
El owner proporciona las dependencias en `C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias`; selecciona esa ruta con `-Dmaven.repo.local`. Usa Maven offline únicamente para compilar, ejecutar tests o arrancar el micro (`spring-boot:run`). Para pruebas de integración H2/Failsafe, usa el job o runner de pruebas configurado para el proyecto; si no está disponible, registra esas pruebas como no ejecutadas y deja las tareas afectadas abiertas. No ejecutes otros goals Maven de descarga, inspección o gestión de dependencias. Si la resolución impide compilar/testear, registra el error y continúa el trabajo independiente sin inspeccionar la caché; deja esos gates sin validar.

DECISIONES CONFIRMADAS DEL OWNER PARA ESTE WORKSPACE
- No hay URL pública de producción aprobada: conserva el fallback local y no inventes un dominio.
- Mantén ocultos los textos legales/comerciales y los materiales del equipo hasta su aprobación explícita.
- SIM-002 sigue siendo una propuesta; no la publiques ni modifiques/invoques DEMO hasta el acuerdo de ambos owners.
- El logo `../../src/main/frontend/images/simbio-logo.png` está autorizado para Home, con alt ES «Logotipo de Simbio» y EN «Simbio logo». Esta aprobación no se extiende a otros recursos de marca.
- Ejecuta Lighthouse/Core Web Vitals solo cuando la aplicación pueda compilar y arrancar; hasta entonces no declares umbrales medidos.

Ejecuta TC de cada ID cuando el comando de test esté disponible, además de pruebas de render ES/EN, accesibilidad, SEO, Lighthouse/Playwright según gates; actualiza documentación. Reporta lo implementado, claims que requieren revisión, contrato generado y si el endpoint externo sigue pendiente.
```

## PROMPT 09 — F2 Notificaciones e identidad extendida

**Estado de este workspace (2026-11-19): COMPLETADO EN LOCAL.** El último `verify -Plocal-h2` completo pasó con 321 tests Surefire, 29 Failsafe aprobados, `SmtpMailpitIT` omitido por opt-in y JaCoCo aprobado. Un intento Maven posterior se detuvo antes de compilar por el POM no disponible `org.sonatype.oss:oss-parent:7`; no ejecutó tests ni invalida el resultado anterior. La prueba real SMTP/Mailpit y la interoperabilidad con el widget npm oficial permanecen `POSTPONED`; no se consideran aprobadas ni impiden registrar el cierre del trabajo local.

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. Registra por separado el estado de desarrollo local (`COMPLETADO_LOCAL` cuando el código y todos los gates disponibles pasan) y el estado global del ID; una validación obligatoria pospuesta mantiene abierto el ID global, pero no bloquea declarar completo el trabajo local.

Implementa NTF-001..NTF-003 e IAM-003/IAM-006..IAM-011/IAM-013 de `docs/plan/tareas/F2-notificaciones-identidad.md`, respetando las dependencias del roadmap. No modifiques DEMO.

CONTEXTO AUTÓNOMO
S-portal es un MONOLITO MODULAR de Simbio: Java 21, Spring Boot estable, Spring MVC/Thymeleaf/HTMX, Spring Security, Modulith/ArchUnit, JPA/Flyway y SQL Server 2022, único desplegable; raíz `com.simbio.core`. Módulos owner `notification` e `identity`. Correo local usa Mailpit; proveedor/región de producción siguen pendientes hasta aprobación. Roles y permisos son server-side y default-deny.

REGLAS
Lee fuentes maestras, `docs/plan/03-arquitectura.md`, `04-modelo-datos.md`, `07-seguridad-rgpd.md`, `08-calidad-testing.md`, `09-operacion-despliegue.md`, `10-roadmap-backlog.md`, ADRs y ficha. Los módulos comunican por named interface/SPI/eventos; no leer repositorios ajenos. Outbox y eventos después de commit, idempotentes, sin secretos/PII innecesaria. Tokens de verificación, recuperación e invitación son aleatorios, de un uso, expirables, guardados con hash y protección contra replay. No enviar ni persistir contraseñas SimbioSphere. Emails bilingües por locale del destinatario. MFA ADMIN obligatorio según los requisitos; no debilitarlo por comodidad.

EJECUCIÓN
Comprueba las dependencias efectivas, incluidas las piezas núcleo F1; no implementes fuera de los IDs. Mailpit y test data sintética. Añade tests de expiración/replay, enumeración, permisos, entrega/outbox/fallo, locale y MFA según fichas. Ejecuta pruebas automatizadas con H2; no se requieren pruebas SQL Server/Testcontainers. Actualiza documentación y deja constancia de que la compatibilidad productiva SQL Server no está verificada. No escribas proveedor ni secretos productivos; deja las propiedades tipadas y el punto de configuración listo. Reporta IDs, colas, migraciones, pruebas y pendientes de proveedor.
```

## PROMPT 10 — F2 Legal y clientes

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Implementa exclusivamente LGL-001..LGL-003 y CUS-001..CUS-004 de `docs/plan/tareas/F2-legal-clientes.md`, conforme al DAG.

CONTEXTO AUTÓNOMO
S-portal es un MONOLITO MODULAR nuevo de Simbio con Java 21, Spring Boot estable, MVC/Thymeleaf/HTMX, JPA/Flyway, Modulith/ArchUnit y SQL Server 2022; `com.simbio.core`, un solo despliegue. Módulos owner `legal` y `customer`; identidad es owner de usuarios/autenticación. En MVP `ClientAccount` puede ser empresa, autónomo o particular y tiene un titular OWNER; el modelo permite ampliar miembros, pero no se debe inventar una asociación por email.

REGLAS Y PRIVACIDAD
Consulta `docs/prompt-plan-desarrollo.md`, requisitos, arquitectura, modelo de datos, seguridad/RGPD, calidad, roadmap, preguntas abiertas y ficha. `legal`/`customer` exponen APIs de módulo; no acceden a entidades/repos ajenas. Consentimiento con versión, finalidad, fecha y evidencia mínima; separar aceptación legal de marketing. Derechos y retención respetan la política pendiente: no borrar irreversiblemente sin aprobación legal. NIF/CIF depende de países y flujo; no consultas externas por defecto. Validar rol+propiedad en cada acceso; no exponer datos a otros clientes.

EJECUTA
Implementa tareas con migraciones SQL Server únicas, constraints/índices, tests de aislamiento de cuenta, autorización/IDOR, aceptación y derechos de datos, según criterios de ficha. Los textos legales son borradores, no asesoramiento ni aprobación. No inventes razón social, fiscalidad ni países; deja pendientes explícitos. Integra con identidad por contrato. Ejecuta TC asignados, actualiza docs y reporta límites/aprobaciones pendientes.
```

## PROMPT 11 — F2 Trabajadores, gestión de usuarios y E2E

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Implementa STF-001..STF-002, USR-001..USR-004 y E2E-001 de `docs/plan/tareas/F2-staff-gestion-usuarios.md`, solo cuando estén satisfechas las dependencias.

CONTEXTO AUTÓNOMO
S-portal es un MONOLITO MODULAR server-side Java 21/Spring Boot estable, MVC/Thymeleaf/HTMX, Security, JPA/Flyway, Modulith/ArchUnit y SQL Server 2022. Un único despliegue; raíz `com.simbio.core`. `identity` administra credenciales/roles, `staff` perfiles laborales, `customer` cuentas y `legal` consentimientos. Roles ADMIN, EMPLOYEE y CLIENT son separados; ADMIN también es trabajador según el plan, pero cada permiso se valida explícitamente.

REGLAS
Lee documentos canónicos y fichas asignadas. No compartas repositorios/entidades entre módulos; consume `api` y eventos con IDs. Para cada ruta, verifica permiso y propiedad en backend; UI no equivale a autorización. Invitaciones usan tokens de un uso, expirables, sin revelar existencia de usuarios; registro y email conforme a módulos base. No expongas notas internas o información de otros clientes.

EJECUCIÓN
Implementa alta/gestión de trabajador, gestión de usuarios y E2E de registro/invitación según la ficha. Usa usuarios/emails sintéticos y Mailpit. Añade pruebas negativas cross-tenant/IDOR, roles, CSRF, expiración y auditoría. Ejecuta MVC/Playwright/axe y las pruebas de persistencia con H2; no se requieren pruebas SQL Server/Testcontainers. Documenta y reporta IDs, pruebas, contratos consumidos, cambios de migración y fallos pendientes.
```

## PROMPT 12 — F3 Modelo, estados, cliente y solicitudes

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Implementa MSG-001..MSG-002 y REQ-001..REQ-011 de `docs/plan/tareas/F3-modelo-estados-cliente.md`, respetando las dependencias y sin adelantar F4.

CONTEXTO AUTÓNOMO
S-portal es un MONOLITO MODULAR nuevo de Simbio, Java 21/Spring Boot estable, MVC/Thymeleaf/HTMX, SQL Server 2022, Modulith/ArchUnit; un desplegable, no microservicios. `request` posee solicitudes/estados y `messaging` posee conversaciones/mensajes. El cliente OWNER ve únicamente sus solicitudes, presupuesto y mensajes públicos; el personal autorizado ve las asignadas/todas conforme al permiso. Las notas internas son invisibles al cliente en consultas, DTOs, plantillas y tests.

REGLAS
Lee `02-requisitos`, `03-arquitectura`, `04-modelo-datos`, `06-ux-ui`, `07-seguridad-rgpd`, `08-calidad-testing`, `10-roadmap-backlog`, máquinas de estado y fichas. Comunicación entre módulos por APIs/eventos; no acceso a repositorios ajenos. Un dueño por tabla y migración Flyway SQL Server. Validar estado y transición en dominio, autorización rol+propiedad en cada caso de uso y consulta, concurrencia/versionado cuando lo pidan las fichas. IDs públicos no sustituye ownership. Solicitudes de SimbioSphere/desarrollo no deben prometer precio/plazos no aprobados.

EJECUCIÓN
Implementa modelo, reglas/transiciones, creación/listado/detalle/adjuntos si están en alcance, mensajería núcleo y vistas de cuenta según IDs asignados. No incluyas contacto/bandeja pública F4. Añade tests exhaustivos de transiciones, ownership, IDOR, visibilidad PUBLIC/INTERNAL, validación, i18n y límites, con persistencia H2. Ejecuta pruebas de UI requeridas; no se requieren tests SQL Server. Actualiza ER/INVENTARIO/DIARIO según fichas; reporta estados y pruebas.
```

## PROMPT 13 — F3 Presupuestos y portal de empleados

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Implementa QUO-001..QUO-004, EMP-001..EMP-007 y E2E-002 de `docs/plan/tareas/F3-presupuestos-empleados.md`, solo tras las dependencias de F3.

CONTEXTO AUTÓNOMO
S-portal es un MONOLITO MODULAR de Simbio, Java 21/Spring Boot estable, MVC/Thymeleaf/HTMX, Spring Security, SQL Server 2022, Modulith/ArchUnit. Un único proceso y despliegue. `request` es owner de presupuesto, asignaciones y solicitudes; `messaging` de conversación. No construir microservicios ni cruzar repositorios. Empleados ven según permisos; clientes solo sus recursos y mensajes públicos; notas internas nunca se renderizan ni se incluyen en respuestas al cliente.

REGLAS
Consulta arquitectura, datos, UX/UI, seguridad, calidad, requisitos, estados y ficha. Sigue las máquinas de estado y separa quién puede crear, enviar, aceptar/rechazar, asignar y cambiar estado. Versiona presupuestos y evidencia de aceptación/rechazo según requisitos. Importes `DECIMAL(19,4)`; EUR y condiciones fiscales siguen provisionales hasta aprobación. No presentar validez, IVA o precio como acordado si el plan los marca pendientes. Permisos se chequean en caso de uso y consultas, no solo en el menú.

EJECUCIÓN
Implementa vistas HTMX por fragmentos con contrato UI aprobado, assignment 1..N, timelines, presupuesto y recorrido E2E asignación→presupuesto. Prueba concurrencia/versiones, permisos/IDOR, ocultación INTERNAL, transición inválida y localización con H2 y pruebas de aplicación. H2 no certifica el dialecto SQL Server; no se exige validación SQL Server/Testcontainers. Evita cambios en el módulo `messaging` fuera del contrato; coordina si hacen falta. Reporta IDs, migraciones, pruebas y aprobaciones comerciales pendientes.
```

## PROMPT 14 — F4 Contacto, bandeja y notificaciones de dominio

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Implementa MSG-003..MSG-013, NTF-004..NTF-005 y E2E-003 de `docs/plan/tareas/F4-contacto-bandeja.md`, conforme a dependencias.

CONTEXTO AUTÓNOMO
S-portal es el MONOLITO MODULAR de Simbio, Java 21/Spring Boot estable, Spring MVC/Thymeleaf/HTMX, SQL Server 2022, un solo despliegue. `messaging` es owner de contacto/conversaciones/mensajes; `notification` de correo/outbox. La bandeja interna es fuente de verdad; email avisa, WhatsApp es click-to-chat; email entrante y WhatsApp Business API son futuros, no MVP.

SEGURIDAD Y PRIVACIDAD
Lee documentos canónicos, ADRs y ficha. APIs de módulo y eventos sin PII innecesaria, nunca acceso a repositorios ajenos. Formularios con CSRF, validación, rate limit/antiabuso y consentimiento aplicable. Archivos pasan por `files` y ClamAV; límites, quarantine y descarga autorizada. Enlaces mágicos: token aleatorio de un uso, hash, expiración, protección replay y alcance mínimo. No capturar formularios/DOM/headers en telemetría. Respuestas públicas y notas internas permanecen segregadas.

EJECUCIÓN
Implementa contacto, canales configurables sin inventar número, bandeja, respuesta con adjuntos/plantillas, polling HTMX y conversión a solicitud conforme a fichas. No habilites email entrante ni WhatsApp API. Pruebas de abuso, permisos, CSRF, link vencido/replay, adjuntos, duplicados, i18n/a11y y recorrido E2E. Configura Mailpit en local; proveedor productivo permanece pendiente. Informa estados, datos retenidos y evidencia.
```

## PROMPT 15 — F5 Integración Portal↔SimbioSphere

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Implementa SIM-001 y SIM-003..SIM-014 más E2E-004 de `docs/plan/tareas/F5-simbiosphere.md`. SIM-002 es contrato previo de F1 y debe estar aprobado; comprueba el DAG antes de iniciar. Trabaja solo en S-portal.

CONTEXTO AUTÓNOMO
S-portal es un MONOLITO MODULAR Java 21/Spring Boot estable, MVC/Thymeleaf/HTMX, SQL Server 2022, un solo desplegable. El módulo `simbiosphere` posee provisioning/vínculos y consume `customer.api`; la saga usa outbox/eventos post-commit y estados recuperables. Integración externa por HTTPS M2M con API REST versionada y HMAC según `docs/plan/05-integracion-simbiosphere.md`; navegador nunca llama a DEMO.

LÍMITES CRÍTICOS
No leer/escribir BD DEMO; no scraping de `/onboarding`; no reutilizar credenciales/login; nunca guardar/enviar contraseña SimbioSphere; no vincular cuentas solo por email. La activación es just-in-time mediante token/url de un uso según contrato aprobado. No afirmar que DEMO publicó una API si solo existe OpenAPI propuesto. No modificar el repo DEMO desde este prompt. El portal opera offline contra WireMock; llamadas de proveedor reales son job separado autorizado.

ARQUITECTURA Y TEST
Lee requisitos, arquitectura, modelo, seguridad, calidad, operación, ADRs y ficha. DTOs/API/eventos en `api`, casos de uso en `application`, adaptador HTTP en `infrastructure`; fuera de transacciones locales para HTTP externo. Outbox/idempotencia/circuit breaker/timeouts/problem details; secretos en variables/secret manager y logs sin PII. Migraciones propiedad de `simbiosphere`; no tabla ajena. Prueba HMAC con vectors, compatibilidad OpenAPI, idempotencia/replay, fallos/timeout, compensación/reintento, roles, estados, WireMock y E2E; nunca uses producción.

Entrega evidencia separando portal local (WireMock) de entorno DEMO real. Si DEMO no está disponible, declara el recorrido real como pendiente y documenta fallback según 05; no hagas una prueba de red no autorizada.
```

## PROMPT 16 — F6 Endurecimiento y salida

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Implementa HRD-001..HRD-014 de `docs/plan/tareas/F6-endurecimiento.md`, tras integrar F0–F5 y sin declarar PRE probado si no existe acceso autorizado.

CONTEXTO AUTÓNOMO
S-portal es un MONOLITO MODULAR Java 21/Spring Boot estable, MVC/Thymeleaf/HTMX, SQL Server 2022, un único artefacto desplegable. No convertirlo en microservicios. LOCAL debe poder levantarse reproduciblemente con Compose; PRE/prod usan configuración y secretos externos. Raíz `com.simbio.core`. Revisa documentación, requisitos, arquitectura, seguridad, testing, operación, roadmap, preguntas abiertas, ADRs y ficha.

CALIDAD/OPERACIÓN
Ejecuta gates acordados: ASVS L2, ZAP solo entorno autorizado, Lighthouse/Playwright/axe/WCAG, carga/baseline, dependencias/SBOM/licencias, Dockerfile multi-stage/no-root, TLS reverse proxy, health y release/rollback. La estrategia de backup/restore se documenta; no se exige una prueba de restauración SQL Server en los gates de desarrollo. App LOCAL 8090; H2 es el único motor de pruebas automatizadas; no se requieren Docker ni SQL Server. SQL Server sigue siendo la única ruta de persistencia productiva; migraciones forward-safe, sin `flyway clean` fuera de una BD desechable de test. Edición SQL, TDE, DNS, correo, secretos, RPO/RTO y dominios pendientes no se inventan.

PREPARACIÓN SIN ACCESO
Deja perfiles y propiedades tipadas, variables documentadas, validación de configuración, checklist de operadores, procedimiento de backup/restore y migración SQL listos para revisión. No escribas secretos ni intentes conectar a PRE/prod sin permiso. Estado de evidencia será `PREPARADO_NO_EJECUTADO` para cualquier comprobación que requiera plataforma/credenciales. Inventario JSON de errores requiere volumen privado, backup y single-writer. No publiques DB ni management port.

Corrige hallazgos solo dentro de IDs asignados y con cambios pequeños; no silencies findings ni rebajes umbrales sin autorización. Ejecuta tests y gates permitidos y documenta evidencia, comandos, limitaciones, hallazgos bloqueantes, rollback y responsable de cada pendiente. HRD-014 no equivale a autorización de despliegue.
```

## PROMPT 17 — Pista DEMO (solo con autorización y workspace DEMO)

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Este prompt SOLO se ejecuta en el repositorio `DEMO-orders-thymeleaf`, nunca desde el workspace S-portal, y requiere autorización explícita para cambiar DEMO. Si el workspace no es el repo DEMO aprobado o no existe permiso, detente sin editar.

CONTEXTO AUTÓNOMO
El producto integrado es S-portal ↔ SimbioSphere. S-portal permanece un MONOLITO MODULAR independiente. DEMO/SimbioSphere es otro producto y workspace; su implementación se rige primero por sus propias instrucciones, skill, normas, constitución, architecture tests y prácticas. La pista de 14 tareas DEMO-01, DEMO-02a, DEMO-02b, DEMO-03a, DEMO-03b, DEMO-03c, DEMO-04, DEMO-05, DEMO-06a, DEMO-06b, DEMO-07, DEMO-08, DEMO-10 y DEMO-11 del plan S-portal especifica el proveedor de una API M2M futura; consulta `docs/plan/tareas/DEMO-backlog.md`, `docs/plan/05-integracion-simbiosphere.md` y SIM-002/OpenAPI aprobado. No mezcles los dos productos en un mismo cambio.

SEGURIDAD/COMPATIBILIDAD
Relee en DEMO las instrucciones/skill vigentes y confirma evidencia actual antes de cambiar. Implementa únicamente operaciones y campos del contrato aprobado bajo `/api/platform/v1/**`, con HMAC anti-replay, key ID/scopes, idempotencia, autorización, rate limits, RFC 7807, auditoría sin secretos y pruebas provider/aislamiento. Adaptador fino al orquestador interno existente; no duplicar lógica, no exponer APIs internas ni ampliar por intuición. No aceptar password de S-portal; activación just-in-time y token de un uso como contrato. BD nunca compartida.

Usa solo datos sintéticos, tests SQL Server requeridos por el repo DEMO y seguridad/arquitectura gates. No llames a producción ni a entornos PRE sin permiso y secretos efímeros aprobados. Informa IDs exactos de DEMO, contrato compatible, pruebas, migraciones, límites y evidencia. Si OpenAPI no está aprobado o fuente actual contradice el plan, detente y solicita resolución; no cambies S-portal desde aquí.
```

## PROMPT 18 — Gate final: checklist LOCAL y preparación para PRE (incluida SQL)

```text
PRUEBAS Y HERRAMIENTAS: si este encargo incluye implementación, crea el código/configuración y los tests aunque falte la herramienta de validación. Ejecuta los gates disponibles; un fallo bloquea avanzar tareas dependientes hasta corregirlo. Pospón solo el test que requiera la herramienta ausente, documentando prueba, herramienta/entorno, motivo, evidencia pendiente e instrucciones reproducibles para ejecutarla en otra máquina habilitada como `POSTPONED` (para PRE: `PREPARADO_NO_EJECUTADO`), nunca `PASS`. La ausencia no bloquea la implementación ni tareas independientes; el ID queda parcial/abierto hasta completar las validaciones obligatorias.

Actúa como agente final independiente de verificación e integración de S-portal. Tu objetivo es comprobar rigurosamente que el MVP funciona y está listo para revisión en LOCAL, y dejar documentado todo lo necesario para que otra persona pueda revisar PRE cuando exista entorno. NO afirmes que PRE funciona si no puedes probarlo con autorización y evidencia.

CONTEXTO AUTÓNOMO
S-portal es la aplicación nueva de Simbio, un MONOLITO MODULAR Java 21/Spring Boot estable, Spring MVC, Thymeleaf/HTMX, Spring Security, JPA/Flyway, Modulith/ArchUnit y SQL Server 2022. Un solo artefacto desplegable y una BD; no microservicios ni SPA. Paquete raíz `com.simbio.core`, `artifactId` provisional `portal`. Lee las fuentes de verdad `docs/prompt-plan-desarrollo.md`, `docs/idea.md` y la documentación completa bajo `docs/plan/`, especialmente 02–13, roadmap, ADRs, fichas y `docs/plan/tareas/F6-endurecimiento.md`.

REGLAS DE VERIFICACIÓN
NO borres datos ni uses `flyway clean` sobre una BD persistente. No uses credenciales reales, no conectes a PRE/prod sin autorización, no cambies servicios externos ni DEMO. No marques un gate por inferencia: registra comando exacto, entorno, resultado, fecha y evidencia. Distingue PASS, FAIL, BLOCKED, NOT_RUN y POSTPONED; una prueba pospuesta debe indicar la herramienta/entorno pendiente y nunca se presenta como PASS. Si faltan prerequisitos, explica causa y responsable. Puedes corregir un defecto pequeño directamente atribuible a IDs ya planificados, pero no cambies arquitectura, alcance, esquema o datos sensibles sin aprobación; repite las pruebas tras cada corrección. No enmascares fallos con skips o reducción de umbrales.

CHECKLIST LOCAL OBLIGATORIO
1. Estado de integración: verifica que las tareas F0–F6 aplicables están integradas, dependencias satisfechas, no hay cambios ajenos perdidos, tests/TC y documentación vinculada. Comprueba los gates de Spring Modulith/ArchUnit y las capas/owners.
2. Build y calidad local: con JDK 21, selecciona el repositorio Maven local proporcionado por el usuario en `C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias` mediante `-Dmaven.repo.local` y usa Maven offline solo para compilar o ejecutar pruebas: `.\mvnw.cmd -o -B "-Dmaven.repo.local=C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias" -Plocal-h2 compile` y `.\mvnw.cmd -o -B "-Dmaven.repo.local=C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias" -Plocal-h2 test`. Si el alcance exige pruebas Failsafe/H2, ejecútalas mediante el job o runner de pruebas configurado; si no está disponible, regístralas como no ejecutadas y deja las tareas afectadas abiertas. No ejecutes metas de descarga/inspección de dependencias ni otros objetivos Maven. Ejecuta los scripts reales para assets, i18n y plantillas; registra selectores omitidos y motivo.
3. Arranque local: valida `.env.example` sin secretos y configuración; no se requiere ejecutar Docker/SQL Server. Perfil local app 8090 y H2 para pruebas; Compose, Mailpit y ClamAV no son gates de desarrollo. Management 8091 no debe exponerse públicamente.
4. Base de datos: todas las pruebas automatizadas de persistencia usan H2 con esquema generado; no ejecutan el árbol Flyway SQL Server ni validan su compatibilidad. SQL Server 2022 sigue como destino de producción, pero no se exige Testcontainers, ejecución MSSQL ni evidencia de VPS. No se debe declarar certificada la compatibilidad SQL Server; reporta esta limitación y las decisiones de licencia/edición/TDE pendientes.
5. Recorridos de producto: valida web pública ES/EN, Home/servicios/Sobre nosotros/Contacto, CMS y legales donde estén en alcance; registro/invitación; cliente y propiedad de solicitudes; presupuesto/empleado; contacto/bandeja; SimbioSphere vía WireMock en offline. Verifica 403/404/500, roles, CSRF, IDOR, INTERNAL oculto, temas, viewport, accesibilidad, SEO y manejo de errores conforme a pruebas existentes. No uses datos reales.
6. Errores/operación: comprueba inventario central `ErrorInventoryService` separado de `/error`, escritura JSON fuera de SQL/webroot, captura backend/frontend/SQL saneada, deduplicación, ADMIN-only, y que un fallo del tracker no oculte error original. Confirma logs/correlation/masking y Actuator privado.

PRE LISTO PARA REVISIÓN, AUNQUE NO EJECUTADO
Comprueba y deja instrucciones/configuración revisables sin secretos para: perfil y propiedades PRE tipadas; variables requeridas y validación de arranque; URL `PORTAL_BASE_URL`/hosts pendientes marcados; SQL Server como destino, schema owner y plan de migración documentados (sin ejecutar comprobaciones de conexión o compatibilidad); procedimiento de backup/restauración documentado; decisión de edición/licencia y TDE pendiente; credenciales mediante secret manager externo; reverse proxy TLS y puertos; health/readiness y management interno 8091; SMTP/provider/DNS pendientes; ClamAV/file volume y permisos; volumen persistente cifrado del JSON de errores con single-writer/backup; política de logs/monitorización; dependencias y contrato DEMO WireMock frente a prueba real de proveedor; datos de prueba aislados; pasos de smoke test y evidencias esperadas. Ningún secreto, endpoint interno o credential debe aparecer en el informe.

Si un prerequisito requiere plataforma, proveedor, legal, credenciales o decisión abierta, etiqueta `PREPARADO_NO_EJECUTADO` y no `PASS`. Si faltan archivos de configuración/runbook para que PRE pueda revisarse, identifica el hueco y, solo si está cubierto por tareas asignadas y no requiere valor inventado, déjalo documentado con placeholders seguros y validación fail-fast.

INFORME FINAL
Crea o actualiza `docs/plan/VALIDACION-LOCAL-PRE.md` sin sobrescribir evidencia previa; registra fecha, versión/revisión del artefacto si existe (sin consultar Git), alcance, tabla por gate con estado/evidencia/comando, resultados de pruebas H2, recorridos, limitaciones, fallos bloqueantes y pendientes PRE con owner/criterio de cierre. Indica explícitamente que SQL Server no fue verificado y no es un gate. Cierra con una decisión inequívoca:
- `LOCAL APTO PARA REVISIÓN` solo si todos los gates obligatorios ejecutados pasan y no hay blocker.
- `LOCAL NO APTO` si existe fallo/bloqueo local.
- `PRE PREPARADO, NO VALIDADO` si el paquete/configuración están listos, pero PRE no se pudo ejecutar.
- `PRE VALIDADO` únicamente con evidencia real autorizada de PRE.

No despliegues ni cierres release. Presenta resumen, ubicación del informe, comandos ejecutados, tests omitidos y defectos corregidos.
```

---

## Reglas comunes de integración para todos los agentes

- La ficha de tarea define criterios y tests; el roadmap define IDs y dependencias. No adelantes tareas bloqueadas ni cambies estimaciones, requisitos o decisiones cerradas unilateralmente.
- Antes de editar, comprueba los cambios del workspace y anuncia/limita las rutas que tocarás. Si hay una colisión, comparte una interfaz o espera al owner: no reescribas cambios ajenos ni edites los mismos archivos en dos worktrees sin coordinación.
- Una tarea debe dejar test `TC-<ID>`, documentación requerida y evidencia. Usa el comando más pequeño que valide el cambio y el gate modular aplicable. Una compilación no sustituye test funcional/seguridad.
- Mantén contrato primero para dependencias: módulos de S-portal por API/SPI/eventos; S-portal↔DEMO por OpenAPI aprobado y WireMock local. No uses llamadas de red real desde tests normales.
- SQL Server 2022 es la única base relacional productiva. H2 es el único motor de pruebas; no añadas un segundo árbol de migraciones. Cada tabla tiene módulo owner. Las pruebas SQL Server/Testcontainers no son gates y no se exige evidencia de VPS; la compatibilidad de producción queda sin verificar. Prohibido borrar datos persistentes o ejecutar operaciones destructivas por comodidad.
- No guardar secretos, PII no necesaria, passwords externos, tokens, bodies ni datos reales en código, tests, logs, capturas, documentación o inventarios. Usa fixtures sintéticos y test secrets efímeros.
- No cambies DEMO salvo ejecutando el Prompt 17 en su repo correcto y con autorización. No afirmes disponibilidad de la API DEMO ni de PRE sin evidencia.
- Si una ambigüedad bloquea una decisión de comportamiento, documenta el hueco y recomendación; no inventes comportamiento incompatible con RF, D01–D12 o decisiones registradas.
- Al cerrar cada carril, devuelve IDs hechos/bloqueados, pruebas y comandos/resultados, archivos/migraciones afectados, riesgos, dependencias liberadas y siguiente paso seguro.

## Resultado esperado del uso del paquete

Los prompts hacen posible repartir trabajo por fichas y módulos, pero no prometen que todos los agentes puedan empezar al mismo tiempo. La concurrencia es segura cuando sus predecesoras están integradas, sus rutas/contratos están acordados y sus archivos tienen owners distintos. El gate final deja dos conclusiones separadas: **qué se probó correctamente en LOCAL** y **qué queda preparado para una futura revisión en PRE, sin fingir que se probó**. La compatibilidad SQL Server y la ejecución de sus migraciones quedan explícitamente fuera de los gates de desarrollo y sin verificar.
