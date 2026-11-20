# F0 — Fundaciones (esqueleto, build, CI, Docker, seguridad y observabilidad base): tareas

- Objetivo: esqueleto modular con gates, CI, configuración Docker declarativa y base de seguridad/observabilidad (hito M0, ver [roadmap](../10-roadmap-backlog.md)). Docker/SQL Server no se ejecutan como gates de desarrollo.
- Módulo(s) owner: build, config, shared e infra (sin módulos de negocio); módulos y DAG en [arquitectura](../03-arquitectura.md).

## Estado de ejecución — PROMPT 02

FND-001..FND-010: **COMPLETADOS** el 2026-11-11. La verificación local con JDK 21 y H2 terminó con BUILD SUCCESS: 32 tests Surefire, 2 tests Failsafe y comprobaciones JaCoCo aprobadas.

La ejecución del análisis Dependency-Check, la revisión de licencias y el lanzamiento remoto de GitHub Actions no son requisitos para cerrar estas tareas. Dependency-Check permanece configurado en el perfil CI; no se afirma ningún resultado de análisis. SQL Server sigue siendo el destino productivo, sin compatibilidad validada y sin gate MSSQL/Testcontainers, conforme al alcance acordado.

| ID | Estado | Evidencia de implementación |
|---|---|---|
| FND-001 | COMPLETADO | Spike H2 y versiones registradas en ADR-001 |
| FND-002 | COMPLETADO | Build Maven, wrapper, Java 21 y test de aplicación |
| FND-003 | COMPLETADO | Kernel `shared`, value objects y SPIs |
| FND-004 | COMPLETADO | Gates Modulith, DAG y ArchUnit |
| FND-005 | COMPLETADO | Compose declarativo y validación estática |
| FND-006 | COMPLETADO | Migraciones/convenios y persistencia H2 |
| FND-007 | COMPLETADO | Perfiles, propiedades tipadas y reglas de secretos |
| FND-008 | COMPLETADO | Workflow CI, JaCoCo, reglas de plantillas e i18n |
| FND-009 | COMPLETADO | CSRF, cookies, cabeceras, CSP y pruebas |
| FND-010 | COMPLETADO | Logs JSON, correlación, redacción y Actuator |

### FND-001 — Spike de stack y verificación de versiones (cierra ADR-001)
- Fase / épica / módulo owner: F0 / FND / build
- Tipo: docs
- Depende de: —
- Descripción: Cubre: RF-GOV-01, RNF-DB-01; spike desechable para la versión Boot estable y soportada al iniciar (Boot 4.1.2 es referencia verificada al 2026-11-10), Java 21, Thymeleaf+Layout Dialect+extras-security, Flyway, Modulith, Argon2, Bucket4j y Resilience4j. Usa H2 como único motor de pruebas; SQL Server 2022 sigue como destino productivo, pero no se verifica su compatibilidad en el spike ni se exige Testcontainers. Maven 3.6.3/JDK 17 se usa para resolver dependencias y Maven 3.6.3/JDK 21 para los builds, sin requisito de versión Maven mínima.
- Ficheros/paquetes a crear o modificar (aproximado): `docs/adr/ADR-001-*.md` (Aceptado; tabla «Versiones fijadas»; Java 21 dentro de 17–27); spike temporal en el workspace, sin incorporar código de prueba al esqueleto.
- Impacto modular: sin contratos, eventos ni tablas; fija versiones (BOM y Modulith) para FND-002..006. Ciclo: ninguno (sin código).
- UI: n/a (spike sin interfaz).
- Seguridad: sin secretos ni datos reales. Dependency-Check está configurado en CI por FND-008; ejecutar el análisis y revisar licencias no son gates para aceptar FND-001.
- Criterios de aceptación (Given/When/Then):
  - Given el spike When se ejecutan con Java 21 y H2 los tests focales de Thymeleaf+Security y Flyway Then pasan y ADR-001 fija las versiones probadas; SQL Server queda documentado como destino no verificado, no como gate.
  - Given un componente incompatible con la versión estable elegida When falla su prueba Then ADR-001 anota la alternativa y FND-002 no empieza hasta Aceptado.
- Tests: TC-FND-001: WT (Thymeleaf+Security), UT (Argon2id) y persistencia de prueba con H2. No se requieren Testcontainers ni pruebas MSSQL.
- Documentación a actualizar: ADR-001; GOV-004 registra DIARIO e INVENTARIO a partir de evidencia observable del workspace y las tareas ejecutadas.
- Estimación: M (≤ 1 d)
- Riesgos: Boot 4.1 trae starters renombrados y librerías sin soporte; la compatibilidad con SQL Server queda sin probar.

### FND-002 — Esqueleto Maven (Boot estable, Java 21), paquete base y wrapper
- Fase / épica / módulo owner: F0 / FND / build
- Tipo: infra
- Depende de: FND-001
- Descripción: Cubre: RNF-OPS-03; proyecto Maven con `groupId` y paquete raíz `com.simbio.core`, `artifactId` provisional `portal`, Spring Boot estable verificado en FND-001 y Java 21, wrapper `mvnw`, `PortalApplication` y puerto 8090 (DEMO usa 8080/8081). Fuera de alcance: módulos, seguridad, Docker.
- Ficheros/paquetes a crear o modificar (aproximado): `pom.xml` (starter-webmvc/-test; enforcer de JDK 21), `mvnw` (Maven 3.6.3), `.mvn/wrapper/`, `application.yml`, `PortalApplication`, `PortalApplicationTests`.
- Impacto modular: nace el paquete raíz `com.simbio.core`; los módulos serán subpaquetes directos. Sin contratos, eventos ni tablas. Ciclo: ninguno (sin módulos).
- UI: n/a (sin plantillas aún).
- Seguridad: sin starter de seguridad (FND-009) ni endpoints; secretos en variables de entorno y `.env` local protegido, nunca en documentación ni salidas; versiones del BOM de ADR-001.
- Criterios de aceptación (Given/When/Then):
  - Given un clon limpio con JDK 21 When ejecuto `./mvnw -B -Dtest=PortalApplicationTests test` Then compila y pasa `PortalApplicationTests` con `server.port=8090`.
  - Given JDK 17 When ejecuto el test focal del build Then el enforcer falla indicando Java 21; no hay una versión mínima de Maven como gate.
- Tests: TC-FND-002: IT (`@SpringBootTest`: contexto carga, `server.port=8090`, `Runtime.version().feature()==21`).
- Documentación a actualizar: DIARIO + INVENTARIO; GOV-004 registra cambios y evidencias observables del workspace, sin consultar historial de Git.
- Estimación: S (≤ 0,5 d)
- Riesgos: nombre empresarial Simbio confirmado; `artifactId` `portal` sigue provisional hasta validarlo en FND-002.

### FND-003 — Kernel `shared` y SPIs (CurrentUser, RecipientDirectory, PersonalDataContributor, NavContribution)
- Fase / épica / módulo owner: F0 / FND / shared
- Tipo: feat
- Depende de: FND-002
- Descripción: Cubre: RF-GOV-01; kernel mínimo `shared` (módulo OPEN): value objects, paginación, errores y 4 SPIs como interfaces, sin lógica de negocio ni implementaciones.
- Ficheros/paquetes a crear o modificar (aproximado): `com.simbio.core.shared.{spi,security,value,paging,error}` (`Role`, `Permission`, `AuthenticatedUser`, `EmailAddress`, `PageQuery`, `PageResult`, `NotFoundException`), `package-info.java`.
- Impacto modular: Hoja OPEN. SPIs: `identity` implementa `CurrentUser`/`RecipientDirectory`; los módulos con PII, `PersonalDataContributor` (orquesta `legal`); `config` compone los `NavContribution`. Sin eventos ni tablas. Ciclo: ninguno (hoja).
- UI: n/a (solo contratos).
- Seguridad: `Role` ADMIN/EMPLOYEE/CLIENT y 11 `Permission`; `EmailAddress` enmascara en `toString()`; `AuthenticatedUser` sin hash ni tokens; `PageQuery` size 1..100.
- Criterios de aceptación (Given/When/Then):
  - Given `EmailAddress.of("  Ana@Example.COM ")` When se crea Then vale `ana@example.com` y `toString()` oculta la parte local.
  - Given `PageQuery.of(0, 101)` When se construye Then lanza `IllegalArgumentException` (size 1..100).
- Tests: TC-FND-003: UT (`EmailAddress`, `PageQuery`, `PageResult`), SEC (`toString()` sin PII).
- Documentación a actualizar: DIARIO + INVENTARIO (shared y SPIs)
- Estimación: M (≤ 1 d)
- Riesgos: kernel como cajón de sastre (lo vigila FND-004); un SPI mal diseñado se paga en varios módulos.

### FND-004 — Gates modulares: Spring Modulith + reglas ArchUnit de capas
- Fase / épica / módulo owner: F0 / FND / build
- Tipo: test
- Depende de: FND-003
- Descripción: Cubre: RF-GOV-02, RNF-TST-01; Modulith 2.1.2 + ArchUnit: análisis de módulos, `ModuleDagTest` y 6 reglas (`web→application→domain`, infraestructura implementa puertos, `@Entity` solo en `domain`, controladores sin repositorios, `api` sin `web`/`infrastructure`, `shared`/`config` sin negocio).
- Ficheros/paquetes a crear o modificar (aproximado): `pom.xml`; `src/test/java/com/symbio/portal/architecture/{ModularityTests,LayerRulesTest,ModuleDagTest}`, `fixtures/`, `src/test/resources/architecture/module-dag.csv`.
- Impacto modular: protege el DAG (`audit`, `files`, `notification` hojas; negocio→`api` ajenas) en `module-dag.csv`. Sin tablas ni eventos. Ciclo: ninguno; las pruebas de arquitectura detectan las infracciones.
- UI: n/a (sin interfaz).
- Seguridad: sin repositorios en controladores, la autorización por propietario (anti-IDOR) queda en `application`; prohibido `@Disabled` en estos tests.
- Criterios de aceptación (Given/When/Then):
  - Given solo `shared` When ejecuto los tests focales de arquitectura para `shared` Then los 3 tests de arquitectura pasan.
  - Given un fixture `web` con repositorio o una arista fuera de `module-dag.csv` When se evalúa Then falla nombrando clase y regla.
- Tests: TC-FND-004: MT (pruebas de arquitectura Modulith), UT (6 reglas; fixtures que DEBEN fallar; `ModuleDagTest`).
- Documentación a actualizar: DIARIO + INVENTARIO (gates)
- Estimación: M (≤ 1 d)
- Riesgos: VERIFICAR en FND-001 `allowedDependencies={}` y módulos OPEN; ArchUnit falla con reglas vacías: `allowEmptyShould(true)` hasta tener módulos.

### FND-005 — docker-compose dev (SQL Server 2022, Mailpit, ClamAV) y `.env.example`
- Fase / épica / módulo owner: F0 / FND / infra
- Tipo: infra
- Depende de: FND-002
- Descripción: Cubre: RNF-DB-01, RNF-OPS-03; `docker-compose.dev.yml` declarativo para entornos Docker habilitados con SQL Server (`mcr.microsoft.com/mssql/server:2022-latest`), Mailpit y ClamAV con healthchecks; BD `sportal` (SUPUESTO) con colación `Latin1_General_100_CI_AS_SC`; puertos 14330, 1026/8026 y 3310 (≠ DEMO). No requiere arrancar Docker/SQL Server para aceptar la tarea.
- Ficheros/paquetes a crear o modificar (aproximado): `docker-compose.dev.yml`, `.env.example`, `docker/mssql/init-db.sql`, `DevComposeTest`.
- Impacto modular: infra para FND-006, NTF-002 (SMTP), FIL-003 (ClamAV) y HRD-005. Sin contratos, eventos ni tablas. Ciclo: ninguno (sin código).
- UI: n/a (Mailpit UI `:8026`, solo dev).
- Seguridad: sin clave literal (DEMO `docker-compose.dev.yml:9`): `MSSQL_SA_PASSWORD` obligatoria vía variable de entorno o `.env` local protegido; puertos solo en `127.0.0.1`.
- Criterios de aceptación (Given/When/Then):
-  -  - Given `docker-compose.dev.yml` y `.env.example` When `DevComposeTest` inspecciona el YAML Then confirma referencias a secretos por entorno, puertos previstos y ausencia de credenciales literales.
- Given la configuración de Compose When no se dispone de Docker/SQL Server Then la tarea se acepta con la validación estática, sin exigir levantar servicios.
- Tests: TC-FND-005: UT (`DevComposeTest` valida YAML, ausencia de secretos literales y puertos; no requiere Docker).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Arranque local»
- Estimación: S (≤ 0,5 d)
- Riesgos: ClamAV usa mucha RAM y baja firmas al arrancar; MSSQL exige ≥ 2 GB; el tag `2022-latest` puede cambiar y debe revisarse antes del despliegue, no es un gate de desarrollo.

### FND-006 — Flyway + H2 para pruebas + convención de migraciones por módulo
- Fase / épica / módulo owner: F0 / FND / build
- Tipo: infra
- Depende de: FND-002, FND-005
- Descripción: Cubre: RNF-DB-01, RNF-DB-02, RNF-DB-03; un único árbol Flyway `db/migration/<modulo>/V<yyyyMMddHHmm>__<modulo>__<desc>.sql` dirigido a SQL Server y baseline `infra` (SUPUESTO). H2 usa esquema generado para todas las pruebas automatizadas; las migraciones SQL Server no se ejecutan ni se certifican.
- Ficheros/paquetes a crear o modificar (aproximado): `pom.xml` (Flyway y H2 en scope de test), `application-test.yml` (perfil H2), `V202611100000__infra__baseline.sql`, `MigrationConventionTest`, `PortalApplicationTests`.
- Impacto modular: contrato de datos: carpeta = esquema `<modulo>`, un owner por tabla (`infra`: build). Sin eventos ni API. Ciclo: ninguno (sin FK entre esquemas, SUPUESTO).
- UI: n/a (sin interfaz).
- Seguridad: `clean-disabled=true` y `baseline-on-migrate=false`; credenciales solo por entorno; sin datos reales en migraciones.
- Criterios de aceptación (Given/When/Then):
  - Given la baseline When corre la suite automatizada con H2 Then pasan las pruebas de persistencia y el único árbol de migraciones SQL Server queda versionado sin requerir ejecutarlo.
  - Given `V1__x.sql` o `dbo.t` en `identity/` When corre `MigrationConventionTest` Then falla nombrando fichero y regla.
- Tests: TC-FND-006: UT (`MigrationConventionTest`: nombre, carpeta=esquema, tipos/convenios estáticos; fixtures DEBEN fallar) y persistencia con H2. Sin Testcontainers ni ejecución de migraciones en SQL Server.
- Documentación a actualizar: DIARIO + INVENTARIO; README (migraciones); ADR-004
- Estimación: M (≤ 1 d)
- Riesgos: sintaxis y comportamiento de las migraciones SQL Server quedan sin verificar; evaluar `spring.modulith.runtime.flyway-enabled`.

### FND-007 — Perfiles dev/test/prod, propiedades tipadas y secretos por entorno
- Fase / épica / módulo owner: F0 / FND / config
- Tipo: infra
- Depende de: FND-002
- Descripción: Cubre: RNF-SEC-09, RNF-OPS-03; perfiles `dev`/`test`/`prod`, propiedades tipadas `portal.*` validadas al arrancar y secretos solo por variables de entorno. Fuera de alcance: gestor de secretos (HRD-010).
- Ficheros/paquetes a crear o modificar (aproximado): `application-{dev,test,prod}.yml`, `config/PortalProperties`, `.env.example` (añade `PORTAL_*`), `PortalPropertiesTest`, `ProdProfileTest`, `NoLiteralSecretsTest`.
- Impacto modular: `config` (OPEN) publica `PortalProperties` (`portal.base-url`, `portal.brand-name`); cada módulo declara sus `portal.<modulo>.*`. Sin eventos ni tablas. Ciclo: ninguno.
- UI: n/a (solo configuración).
- Seguridad: secretos solo por entorno, sin literales en YAML (DEMO `application-dev.yml:26-27`); proteger localmente `.env` y no incluirlo en entregables; sin perfil activo por defecto.
- Criterios de aceptación (Given/When/Then):
  - Given perfil `prod` When `PORTAL_BASE_URL` es `https://…` Then carga; si falta o es `http`, el arranque falla con error de binding.
  - Given `password: abc123` en `application-prod.yml` When corre `NoLiteralSecretsTest` Then falla nombrando fichero y clave.
- Tests: TC-FND-007: UT (`PortalPropertiesTest`), IT (`ProdProfileTest`), SEC (`NoLiteralSecretsTest`; fixtures que DEBEN fallar).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Configuración»
- Estimación: S (≤ 0,5 d)
- Riesgos: `${VAR}` sin resolver puede quedar literal (VERIFICAR): obligatorios con `@NotBlank`/`@Pattern`.

### FND-008 — Pipeline CI con gates (tests, cobertura por módulo, dependencias, i18n, plantillas)
- Fase / épica / módulo owner: F0 / FND / infra
- Tipo: infra
- Depende de: FND-004, FND-006
- Descripción: Cubre: RF-GOV-02, RNF-SEC-09, RNF-TST-06; `.github/workflows/ci.yml` (GitHub Actions, SUPUESTO): gates CI con H2, Modulith, ArchUnit, i18n, plantillas y JaCoCo ≥ 80 % en `domain`/`application`; no hay job ni evidencia SQL Server/Testcontainers requerida.
- Ficheros/paquetes a crear o modificar (aproximado): `.github/workflows/ci.yml`, `pom.xml` (JaCoCo, failsafe, Dependency-Check), `TemplateRulesTest`, `utext-allowlist.txt`.
- Impacto modular: transversal: ejecuta los gates de FND-004, UIK-005 (i18n) y UIK-002 sin duplicarlos. Sin contratos, eventos ni tablas. Ciclo: ninguno.
- UI: n/a (pipeline; revisa plantillas).
- Seguridad: `permissions: contents: read`, acciones fijadas por SHA, sin `pull_request_target`; `NVD_API_KEY` solo como secreto; falla con CVSS ≥ 7.
- Criterios de aceptación (Given/When/Then):
  - Given un PR que cumple los gates When corre CI con la suite H2 Then pasan los gates automatizados y se publican sus informes; disparar un workflow remoto no es requisito para cerrar FND-008.
  - Given `th:utext` fuera de la allowlist, un `*.domain` < 80 % o una dependencia CVSS ≥ 7 When corre CI Then falla ese job y bloquea el merge.
- Tests: TC-FND-008: UT (`TemplateRulesTest`: colores directos, `th:utext`, script sin nonce; fixtures DEBEN fallar), comprobación local del perfil CI. No se requiere un PR remoto de prueba.
- Documentación a actualizar: DIARIO + INVENTARIO; README «CI y gates»
- Estimación: M (≤ 1 d)
- Riesgos: NVD sin API key o caído; `includes` de JaCoCo por paquete (VERIFICAR); `main` sin protección deja los gates opcionales.

### FND-009 — Seguridad base: cadena Security, cookies propias, CSRF, cabeceras y CSP con nonce
- Fase / épica / módulo owner: F0 / FND / config
- Tipo: feat
- Depende de: FND-002, FND-007
- Descripción: Cubre: RNF-SEC-02, RNF-SEC-03, RNF-SEC-04; `SecurityFilterChain` base en `config.security`: CSRF por cookie `SPORTAL_XSRF`, sesión `SPORTAL_SESSION`, CSP con nonce, HSTS y acceso denegado por defecto.
- Ficheros/paquetes a crear o modificar (aproximado): `pom.xml` (starter-security), `config/security/{SecurityConfig,CspNonceFilter}`, `SecurityHeadersTest`, `CsrfTest`, `SessionCookieIT`.
- Impacto modular: `config` (OPEN) publica la cadena y `cspNonce` (UIK-004); IAM-004 añade el login sin que `config` importe `identity`. Sin eventos ni tablas. Ciclo: ninguno.
- UI: n/a (cabeceras, sin interfaz).
- Seguridad: cookies HttpOnly, Secure, SameSite=Lax; sin `unsafe-*` (DEMO `SecurityConfig.java:56-71`); CSP report-only solo en `prod` hasta HRD-002; `microphone=(self)` (RF-MSG-02).
- Criterios de aceptación (Given/When/Then):
  - Given perfil `test` y una página pública When responde Then CSP con nonce distinto por petición y sin `unsafe-inline`/`unsafe-eval`.
  - Given un `POST` sin token CSRF When se envía Then 403 sin ejecutar la acción; con el token de `SPORTAL_XSRF` pasa.
- Tests: TC-FND-009: WT (CSP, HSTS, nonce único, CSRF 403/OK), IT (cookies `SPORTAL_*` con atributos, sin `JSESSIONID`), SEC (ruta no listada y `/gestion/**` sin sesión → 403).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Seguridad»
- Estimación: M (≤ 1 d)
- Riesgos: Alpine CSP, htmx y `style=` chocan con la CSP (UIK-008); el HTML con nonce no se cachea en CDN.

### FND-010 — Observabilidad base: logs JSON, correlation ID, enmascarado de PII y Actuator protegido
- Fase / épica / módulo owner: F0 / FND / config
- Tipo: feat
- Depende de: FND-002
- Descripción: Cubre: RNF-SEC-08, RNF-OPS-01; logs JSON en `prod`, `CorrelationIdFilter` (`X-Correlation-Id` → MDC), `PiiMasker` y Actuator `health`/`info`/`prometheus` en el puerto 8091 (SUPUESTO).
- Ficheros/paquetes a crear o modificar (aproximado): `pom.xml` (actuator, Prometheus), `config/observability/{CorrelationIdFilter,PiiMasker}`, `ActuatorExposureIT` y tests unitarios.
- Impacto modular: `config` (OPEN): los módulos solo usan SLF4J y heredan `correlationId` del MDC; sin contratos, eventos ni tablas. Ciclo: ninguno.
- UI: n/a (sin interfaz).
- Seguridad: Actuator solo en 8091 (no publicado), 3 endpoints y sin detalles; `X-Correlation-Id` validado `[A-Za-z0-9-]{8,64}`; emails, `token=` y `password=` enmascarados.
- Criterios de aceptación (Given/When/Then):
  - Given un `X-Correlation-Id` inválido y un log con `ana@example.com` y `token=abc` When corre en `prod` Then el JSON lleva `correlationId` UUID, `***@example.com` y `token=***`.
  - Given la app arrancada When pido `/actuator/health/readiness` en `:8091` Then 200 `UP`; `/actuator/env` y `:8090/actuator/health` dan 404/403.
- Tests: TC-FND-010: UT (`PiiMaskerTest`: emails, `token=`, `stack_trace`; `CorrelationIdFilterTest`), IT (`ActuatorExposureIT`), SEC (JSON de `prod` sin PII).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Observabilidad»
- Estimación: M (≤ 1 d)
- Riesgos: verificar en la versión elegida los starters Prometheus/JSON logs; Security (FND-009) podría cubrir el puerto 8091.