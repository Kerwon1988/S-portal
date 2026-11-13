# ADR-001 — Spring Boot 4.1.x y gobernanza modular

- Estado: Aceptado
- Fecha: 2026-11-11

## Contexto
D10 fija Java 21 y la última versión Spring Boot con soporte OSS vigente al ejecutar el plan. FND-001 verificó Boot 4.1.2 y Spring Modulith 2.1.2; la compatibilidad de las dependencias se probó con H2. SQL Server 2022 sigue como destino productivo, pero no se valida con Testcontainers ni constituye un gate de desarrollo.

## Decisión
Usar Java 21, Spring Boot 4.1.2 y Spring Modulith 2.1.2 para iniciar FND-002..006. Adoptar Spring Modulith para `ApplicationModules.verify()`, `@ApplicationModuleTest`, named interfaces y Event Publication Registry; complementar con ArchUnit para capas/ownership. Un monolito, una BD y un esquema por módulo. `shared` y `config` OPEN con límites estrictos.

### Versiones fijadas por FND-001

| Componente | Versión / gestión |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.2 |
| Spring Modulith | 2.1.2 |
| Thymeleaf Layout Dialect | 3.3.0 |
| Thymeleaf Extras Spring Security | 3.1.2.RELEASE |
| Flyway | Gestionado por Spring Boot 4.1.2 |
| H2 | 2.4.240 |
| Bouncy Castle (`bcprov-jdk18on`) | 1.80 |
| Bucket4j (`bucket4j-core`) | 8.10.1 |
| Resilience4j Retry | 2.3.0 |

El spike pasó `mvn verify` con Maven 3.6.3 y Java 21: 2 tests, 0 fallos, 0 errores. Flyway aplicó la migración de prueba H2; el test verificó también la cadena de seguridad, los dialectos Thymeleaf, Argon2, Bucket4j, Resilience4j y `ApplicationModules.verify()`. Flyway advierte que H2 2.4.240 es posterior a la última versión con la que se declara verificado (2.3.232); se conserva la versión probada y se registra la advertencia, sin afirmar certificación de esa combinación.

## Alternativas
- Seguir en Boot 3.3.5: menor cambio inicial, pero el soporte OSS terminó según brief; no recomendado.
- Boot 3.5.x: alternativa si Boot 4 incompatibiliza dependencias requeridas; fijar solo tras spike y confirmar soporte vigente.
- ArchUnit sin Modulith: menos dependencias, pero no aporta detección/contratos/eventos Modulith requeridos por el diseño.

## Consecuencias
Requiere validar starters/APIs Boot 4, plugin Maven, configuración de seguridad, Flyway con H2 y esquema EPR. `@ApplicationModuleTest` y Modulith deben probarse antes de fijar estructura. La sintaxis y comportamiento de Flyway sobre SQL Server quedan sin verificar. El owner confirma `com.simbio.core` como `groupId` y paquete raíz; `portal` queda como `artifactId` provisional hasta validar el build en FND-002.

## Estado de cierre

FND-001..FND-010 se dan por completadas según el alcance acordado: build y pruebas locales con Java 21/H2, incluyendo 32 tests Surefire, 2 Failsafe y las comprobaciones JaCoCo. Dependency-Check permanece configurado en CI, pero ejecutar el análisis o revisar licencias no es requisito de cierre; este ADR no afirma que el análisis haya resultado limpio.

SQL Server 2022 continúa como destino productivo, sin compatibilidad validada. H2 es el único motor de pruebas automatizadas y no se requieren gates MSSQL ni Testcontainers.
