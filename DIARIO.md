# Diario de desarrollo

## 2026-11-18 — Evaluación offline de versiones de Spring Boot

- Para retirar el requisito de `org.apache.artemis:artemis-project:2.53.0` se intentaron los parents Spring Boot 4.0.0, 4.1.1 y 4.1.0 con la compilación offline autorizada. Maven no pudo cargar el proyecto porque ninguno de esos POM parent está disponible en el repositorio local proporcionado.
- Se restauró `pom.xml` a Spring Boot 4.1.2, la única versión de parent disponible y previamente fijada. No se descargaron ni añadieron dependencias, no se inspeccionó la caché y no se llegó a compilar ni ejecutar tests.
- El usuario decidió mantener el modo offline. Por tanto, sigue pendiente retirar o resolver el parent Artemis 2.53.0; no se afirma que la compilación o las pruebas Java hayan pasado.

## 2026-11-18 — Reintento con repositorio offline actualizado

- Se usaron los comandos de `docs/lanzar en local.md` para compilar y ejecutar la suite. Ambos se detuvieron antes de cargar el proyecto: falta el parent `org.apache:apache:pom:37` requerido por `org.apache.artemis:artemis-project:2.53.0`.
- En la ejecución anterior, Maven llegó a Surefire. La salida de depuración confirma que el plugin selecciona `plexus-utils:3.5.1` y no solicita `plexus-utils:1.1`; el fallo de entonces fue la ausencia de `surefire-junit-platform:3.5.6`. Con el bloqueo actual del parent no se puede comprobar si el provider ya está disponible.
- No se ejecutaron tests en este reintento ni se inspeccionó o modificó el repositorio/cache offline.

## 2026-11-18 — Validación Maven y tentativa de arranque

- Se ejecutó la compilación solicitada con Java 21 y el repositorio offline proporcionado; terminó en BUILD SUCCESS, con fuentes productivas al día.
- Al rastrear la resolución del plugin se encontró que Surefire transitivamente solicitaba `org.codehaus.plexus:plexus-utils:1.1`. Se fijó la versión explícita `3.5.1` en las dependencias del plugin; la salida de depuración confirma que `1.1` ya no forma parte del classpath ni vuelve a solicitarse.
- Las fuentes de test compilaron, pero Surefire no pudo iniciar porque falta `org.apache.maven.surefire:surefire-junit-platform:3.5.6` en el repositorio offline. No se ejecutó ningún test.
- En una validación posterior, Maven `test-compile` recompiló con Java 21 las 342 fuentes de producción y las 119 fuentes de test y terminó en BUILD SUCCESS. `verify` llegó a Surefire, pero se detuvo antes de ejecutar tests por faltar `org.apache.maven.surefire:surefire-junit-platform:3.5.6` en el repositorio offline.
- El debugger informó que la aplicación había arrancado, pero al comprobarlo ya no había sesión activa; tampoco había listener en el puerto 8090 y el GET a `/acceso` no conectó. No se repitió el arranque tras ese primer fallo. Runtime sigue sin confirmar.
- No se inspeccionó ni modificó ninguna caché. Los servicios externos y SQL Server no se arrancaron ni probaron.

## 2026-11-18 — Continuación parcial de PROMPT 10

- Se completaron rutas de registro e invitación que estaban ausentes en la nota previa: `/registro` usa el locale de petición, comprobación server-side del desafío anti-bot, versiones legales actuales, creación de usuario CLIENT no verificado, cuenta OWNER, teléfono E.164 cifrado, consentimiento legal versionado y marketing opcional. Email duplicado conserva respuesta genérica.
- La invitación customer se expone en `GET/POST /gestion/clientes/nuevo`; el permiso decidido es ADMIN + `USERS_MANAGE`, con CSRF y comprobación adicional de rol/permiso en el servicio. Email/token siguen siendo propiedad de identity; customer conserva solo una referencia opaca y nombre de cuenta cifrado y crea la cuenta después de `InvitationAccepted`.
- Se añadieron migraciones customer V009/V011 e identity V010, pruebas focales y una prueba MVC para acceso público/CSRF. Se corrigieron errores de testCompile encontrados: método anidado por llave ausente, import de validador fiscal incorrecto, y `Locale.SPANISH`/matcher Mockito `any(Locale)` ausentes.
- Con el repositorio offline proporcionado, Maven compiló las fuentes de test; no llegó a ejecutar tests porque Surefire no pudo resolver `plexus-utils:1.1`. No se inspeccionó ni modificó ninguna caché. `npm run build` pasó y `npm run test:privacy` pasó 3/3.
- Estado: LGL-001 y CUS-001..004 tienen código parcial o fuente de tests, pero siguen sin validación Java. IAM-006 queda parcial porque el proof-of-work/honeypot propio no demuestra compatibilidad con ALTCHA oficial y el anti-replay no es multi-réplica; IAM-007/010 están integradas en el flujo, pero no validadas. LGL-002 sigue bloqueada por falta de contributors de todos los owners y aprobación de anonimización; LGL-003 sigue sin política/job ejecutable por falta de categorías y plazos aprobados. Se mantiene la decisión: solo dry-run, no purgas ni anonimización irreversible.
- Se alinearon README, INVENTARIO, arquitectura, modelo de datos, seguridad/RGPD y contrato UI↔backend con la implementación observable. SQL Server no se probó. No se declaran cerrados los IDs afectados.

## 2026-11-18 — Corrección sintáctica de IAM-006

- Se contrastó IAM-006 con el roadmap y `docs/plan/tareas/F2-notificaciones-identidad.md`: owner `config`, dependencias UIK-008/FND-009, criterios de validación server-side, uso único y rechazo genérico. El cambio no altera el comportamiento ni los contratos de `AntiBotVerifier`.
- Se cerró `digest()` antes de declarar `AltchaPayload`, que había quedado anidado ilegalmente en el método. La verificación Maven offline ya no reporta el error de sintaxis en `AntiBotVerifier`; javac alcanza 342 fuentes y falla con 13 errores en `MfaController`, `ConsentService`, `OutboxService` y `NotificationOutboxHealthIndicator`.
- `-Dtest=AntiBotVerifierTest test` tampoco llegó a ejecutar el test porque Maven compila primero todas las fuentes principales. IAM-006 no se declara validada; no se ejecutaron tests ni se arrancó la aplicación. La caché principal continúa requiriendo resolución de los errores de compilación restantes.

## 2026-11-18 — Comandos locales de compilación, tests y arranque

- README documenta por separado `compile`, `verify` y `spring-boot:run` en PowerShell, con Java 21, Maven 3.6.3, la caché principal proporcionada, modo offline y `local-h2`.
- `verify` ejecuta la compilación, las pruebas unitarias y de integración y el gate JaCoCo; el comando de arranque mantiene el servidor activo en el terminal. La documentación aclara que no se descargan dependencias y que la validación actual sigue bloqueada por errores de compilación, sin afirmar que los tests o el arranque hayan pasado.

## 2026-11-18 — Implementación parcial de PROMPT 10 (LGL / CUS)

- Se contrastó el alcance con `docs/plan/tareas/F2-legal-clientes.md`, el roadmap y las reglas modulares. Se reutilizaron los owners `legal` y `customer`; no se añadieron rutas de UI ni acceso cruzado a repositorios.
- LGL-001 añade registro versionado ligado a un documento CMS publicado/vigente, finalidad, locale, fuente, fecha UTC de servidor y auditoría mínima. El servicio rechaza que el sujeto no coincida con el usuario autenticado. Se añadió un borrador de copy comercial CMS, pendiente de revisión y publicación; ausencia de aceptación equivale a no consentimiento.
- CUS-001 añade `client_account` y `account_member`, miembro OWNER activo único, verificación de identidad por API y auditoría. CUS-004 añade validación sintáctica española para NIF/NIE/CIF, cifrado mediante `SensitiveDataProtector` y salida enmascarada solo para CLIENT propietario. No se consulta una autoridad fiscal ni se afirman países distintos de ES.
- Se añadieron tres migraciones Flyway SQL Server y tests unitarios/H2 para consentimiento, cuenta, restricciones, validación y cifrado. Un intento offline de Maven se detuvo durante la resolución del modelo antes de compilar o ejecutar tests; por ello LGL-001, CUS-001 y CUS-004 siguen sin validar. No se ejecutó SQL Server.
- LGL-002 sigue sin implementarse: el SPI `PersonalDataContributor` no tiene contributors de dominio registrados, no hay exportación ni tabla/servicio de solicitudes y la anonimización irreversible requiere aprobación legal. LGL-003 sigue sin implementación ejecutable: no se activan políticas ni purgas; la decisión vigente limita retención a dry-run hasta aprobar categorías/plazos.
- CUS-002 permanece bloqueada por IAM-006/IAM-007 y las pruebas faltantes; CUS-003 permanece bloqueada por IAM-010 no validado. No se amplió alcance para completar dependencias IAM. README, INVENTARIO, modelo de datos y seguridad/RGPD se actualizaron para distinguir código, pruebas escritas y pendientes.

## 2026-11-18 — Implementación parcial de PROMPT 09 (NTF / IAM)

- Se contrastó el alcance con `docs/plan/tareas/F2-notificaciones-identidad.md`, el roadmap y las normas modulares. Se trabajó en `notification`, `identity`, `shared` y `config`; DEMO no se modificó. SMTP, key ring y retención productivos permanecen configurables, sin inventar proveedor, secretos ni política legal.
- `notification` añade `EmailSender`, outbox con payload AES-256-GCM/key ID, SMTP, plantillas ES/EN HTML/texto plano, worker con lease y reintentos limitados, health y métricas. El envío no ofrece exactly-once: puede haber duplicado si SMTP acepta y el proceso cae antes de confirmar el estado. Mailpit no se arrancó ni se probó.
- `identity` añade tokens opacos aleatorios almacenados como SHA-256, expiración/revocación/consumo condicional, verificación de email, recuperación/cambio de contraseña, invitaciones ADMIN, TOTP con secreto cifrado y códigos de recuperación hashados, perfil con nombre/locale/tema, y adapters para `CurrentUser`, `RecipientDirectory`, `ThemePreference` y `LocalePreference`. Las mutaciones usan CSRF, el perfil resuelve al principal autenticado y los eventos solo transportan IDs. MFA es obligatorio para ADMIN; se corrigió el handler de acceso para que EMPLOYEE pueda optar por enrolarse y después también deba completar el desafío.
- `config` añade proof-of-work/honeypot autoalojado, nonce protegido, expiración, protección de replay por proceso, métrica agregada y alternativa accesible. IAM-006 sigue parcial: no implementa protocolo ALTCHA oficial ni está conectado a formularios de registro/contacto, que aún no existen.
- Se añadieron tests para AES-GCM, anti-bot, delivery policy, TOTP, consumo concurrente H2 de tokens y desafío MFA de un EMPLOYEE enrolado; también se actualizaron tests de perfil/login ADMIN. El comando focal Maven no llegó a leer/compilar el proyecto: falló la confianza TLS del certificado corporativo al resolver BOMs (`PKIX path building failed`). Un segundo intento offline confirmó que faltan esos BOMs en la caché. No se desactivó TLS ni se intentó instalar dependencias por otra vía. No se afirma que los tests hayan pasado.
- El índice de email de invitación usa ahora HMAC-SHA-256 con separación por propósito en vez de SHA-256 sin clave para nuevas invitaciones. La revocación deriva candidatos desde todas las claves configuradas para conservar compatibilidad durante la rotación, y consulta temporalmente el hash heredado para revocar invitaciones anteriores; su eliminación persistente depende de la política/mecanismo de purga de tokens, aún pendientes. Hay pruebas escritas para clave activa/anterior, separación de propósito y normalización/revocación, aún sin ejecución por el bloqueo Maven. El key ring anterior debe conservarse mientras existan invitaciones activas creadas con esas claves.
- Por indicación del owner, no se invertirá más tiempo en la confianza de certificados de Maven durante este prompt; el gate Java queda explícitamente pendiente, sin desactivar validación TLS.
- `npm run build` y `npm run test:privacy` pasaron (3/3 tests de privacidad); el build mostró únicamente el aviso de `caniuse-lite` desactualizado. No validan el código Java. No se ejecutaron Mailpit, SQL Server, Playwright ni despliegue. Una restricción SQL de rol de invitación se endureció para rechazar `NULL` de forma explícita, ya que un `CHECK` SQL Server puede aceptar UNKNOWN.
- README, INVENTARIO, arquitectura/modelo de datos y ADR-002/007/009 se actualizaron para distinguir implementación de evidencia. NTF-001..003 e IAM-003/006..011/013 permanecen abiertos; no se atribuye compatibilidad SQL Server. También siguen pendientes proveedor/región SMTP, retención aprobada, operación/rotación de claves, anti-replay multi-réplica y pruebas de flujo completo.

## 2026-11-17 — Validación de cambios recientes de PROMPT 08

- `npm run e2e -- --list` terminó correctamente y descubrió 16 pruebas; no ejecuta las pruebas contra la aplicación. `npm run test:privacy` pasó 3/3 y `npm run build` finalizó correctamente.
- El goal Maven `test` volvió a terminar antes de compilar y ejecutar pruebas. No se inspeccionó ni modificó ninguna caché o configuración de dependencias. Los tests Java nuevos del footer y del filtro de caché siguen sin validar.
- No se ejecutó Playwright E2E ni Lighthouse/CWV porque requieren la aplicación arrancada; no se declaran validados el comportamiento HTTP ni los umbrales de rendimiento.

## 2026-11-17 — PROMPT 08 WEB-006: footer legal y privacidad de cookies

- El footer del módulo `content` obtiene únicamente documentos legales publicados; los borradores no generan enlaces públicos. Los textos legales siguen ocultos hasta aprobación.
- El estado actual usa cookies técnicas de tema/idioma y sesión. No hay cookies opcionales ni analítica de terceros configuradas, por lo que no se muestra un banner de cookies. El informe técnico de errores es independiente, requiere aceptación explícita y usa un POST same-origin protegido con CSRF.
- Se añadió `npm run test:privacy`: 3 tests Node pasan para rechazo por defecto, envío solo tras aceptación con CSRF/same-origin y ausencia de banner/tracker analítico público.
- `npm run build` también pasó; el test Maven focal de las pruebas Java del footer se detuvo antes de ejecutar tests, por lo que la verificación completa anterior no cubre estas pruebas nuevas. No se inspeccionó ni modificó ninguna caché de dependencias.
- WEB-006 sigue parcial: faltan la aprobación del contenido legal y la implementación/validación del flujo condicional si se decide configurar cookies no esenciales. No se publica copy pendiente ni se habilita tracking.

## 2026-11-17 — PROMPT 08 WEB-007: caché de assets fingerprintados

- Se añadió un filtro `config.web` que aplica `public, max-age=31536000, immutable` solo a respuestas GET/HEAD 200/206/304 de assets `/assets/<nombre>.<16 hex>.(css|js|png)`. No coincide con `asset-manifest.json`, contenido de páginas, previews ni rutas privadas; no cachea respuestas de error.
- Se añadieron tests unitarios para el caso positivo, manifiesto, nombres sin fingerprint, rutas públicas no estáticas, métodos no permitidos, status de error y context path. El Maven focal se detuvo antes de ejecutar tests; la política está implementada, pero su test Java queda sin validar.
- Tras `npm run build`, un chequeo del manifiesto confirmó que sus 8 rutas usan el patrón fingerprintado esperado; esto no sustituye la validación HTTP de los headers.
- Reintento focal del goal `test` a las 20:07: se volvió a detener antes de compilar y no ejecutó ninguna prueba; no se inspeccionó la caché ni se cambió la configuración Maven.
- WEB-007 sigue parcial: las mediciones Lighthouse/CWV y la comprobación HTTP integrada de respuestas autenticadas/previews esperan que la aplicación pueda arrancar. No se declara cumplido ningún umbral.

## 2026-11-17 — Respuestas del owner sobre bloqueos restantes de PROMPT 08

- WEB-002: mantener Home estática; no habilitar vídeo ni póster hasta recibir un recurso aprobado.
- WEB-003/004: conservar copy verificable y fallback local; el material no aprobado queda pendiente.
- WEB-006: no configurar cookies no esenciales ni habilitarlas antes del consentimiento explícito y la aprobación correspondiente.
- Estas decisiones mantienen abiertos los criterios que requieren recursos o aprobaciones; no cambian las decisiones previas sobre SIM-002 ni las mediciones de rendimiento.

## 2026-11-17 — PROMPT 05: verificación del repositorio de dependencias aportado

- Se verificó `C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias` como repositorio Maven estructurado. Los JAR de Tika 3.2.3, Commons IO 2.20.0, WebP 0.1.6, `spring-boot-data-jpa-test` 4.1.2 y `spring-security-test` 7.1.2 existen en las rutas Maven indicadas.
- Con Java 21 y modo offline, usando un mirror temporal para el ID de repositorio de la caché, Maven leyó el POM y llegó a la resolución de dependencias del proyecto. Se detuvo antes de compilar en `jaxb-runtime:4.0.9`, `classmate:1.7.3`, `byte-buddy:1.18.15`, `byte-buddy-agent:1.18.15` y `commons-logging:1.3.6`; sus rutas y nombres de artefacto no aparecen en la carpeta aportada. Este resultado se limita a esa carpeta, no afirma que no existan en otras ubicaciones.
- No se ejecutaron tests ni se arrancó el micro. No se descargó nada ni se modificó ningún repositorio/caché; se retiraron los settings temporales offline.

## 2026-11-17 — Reanudación de PROMPT 05: refactor de capas y gate offline

- Se movió la conversión de códigos de tipos CMS del formulario web a `content.application`; el formulario ahora pasa códigos y la aplicación valida los valores. Se añadió una prueba negativa de códigos de página/bloque desconocidos. El cambio no está validado porque no se pudo compilar.
- Se intentó `.\mvnw.cmd -o -gs .mvn\prompt05-empty-global-settings.xml -s .mvn\prompt05-offline-settings.xml -Plocal-h2 '-Dtest=LayerRulesTest,ContentServiceTest' test` con Java 21. Maven llegó al proyecto, pero falló al resolver dependencias offline (incluidos Tika, Commons IO, WebP, CommonMark, OWASP Java HTML Sanitizer y los dos starters de test) antes de compilar; no se ejecutó ningún test. No se descargó ni se modificó la caché.
- El micro no se arrancó: no hay compilación/tests actuales que confirmen el código y el depurador necesitaría resolver el mismo classpath Maven.
- Se actualizaron los criterios específicos de Prompt 05: compilación Java 21 y todos los tests requeridos son el gate de cierre; `verify`/Enforcer se registra aparte. Si Maven no permite compilar o ejecutar los tests, Prompt 05 sigue abierto. No se declara cerrado ningún ID.

## 2026-11-17 — Respuestas del owner a bloqueos de PROMPT 08

- No hay URL pública de producción aprobada; se conserva el fallback local. Textos legales, comerciales y materiales del equipo permanecen ocultos hasta su aprobación. El owner autorizó específicamente una variante del logo para Home, registrada abajo.
- SIM-002 permanece como propuesta hasta la aprobación de ambos owners Portal y DEMO. El contenido público de SimbioSphere se limitará a afirmaciones verificables; lo no confirmado se omite y no se llama ni modifica DEMO.
- Lighthouse/Core Web Vitals se medirán cuando Maven pueda compilar y la aplicación pueda arrancar; no se afirma todavía ningún resultado de rendimiento.
- Se autoriza continuar con trabajo independiente y conservar explícitamente los bloqueos. Para todos los prompts, Maven queda limitado a compilar, ejecutar tests o arrancar la aplicación; `verify` solo se permite cuando sea necesario para lanzar tests de integración H2/Failsafe. El owner proporciona las dependencias y no se ejecutan comandos de resolución, descarga o inspección de caché.

## 2026-11-17 — Test focal con repositorio local proporcionado

- Se ejecutó el goal permitido `test` con JDK 21, `-o`, perfil `local-h2` y el repositorio local indicado por el owner. Tras ese intento, se creó un `settings.xml` temporal propio con dicha ruta y el mirror `maven-public`, y se repitió el mismo test focal.
- Ambos intentos se detuvieron antes de compilar al resolver el POM padre Spring Boot contra `maven-public` en modo offline. No se ejecutaron tests ni se inspeccionó el contenido del repositorio; esto no afirma que el artefacto falte físicamente. El settings temporal se eliminó.

## 2026-11-17 — Avance parcial de PROMPT 08

- SEO (`content`): se añadieron metadatos absolutos por idioma, canonical/hreflang, OpenGraph, JSON-LD `Organization`, respuestas de sitemap/robots y caché pública limitada a contenido publicado. Las previews siguen excluidas de indexación. No se inventan dominio ni datos corporativos no confirmados.
- Se ajustó el seed de «Sobre nosotros» para no publicar automáticamente materiales pendientes de aprobación; se añadió una aserción H2. Legal, equipo y assets no aprobados permanecen sin publicar.
- SIM-002: se creó `docs/plan/contracts/simbiosphere-platform-v1.yaml`, un test estructural del contrato y tres fixtures WireMock locales. El YAML es una propuesta no publicada; DEMO-01 y la revisión/acuerdo Portal–DEMO siguen pendientes. No se modificó ni se llamó a DEMO.
- Se actualizaron README, INVENTARIO y `05-integracion-simbiosphere.md`. WEB-001..007/SEO-001..002 no se declaran completos porque sus predecesoras CMS y aprobaciones siguen sin validar; SIM-002 permanece parcial/bloqueada por el acuerdo externo.
- Gate focal con JDK 21/H2 intentado: `mvnw -o -B -s temporary-maven-settings.xml -Plocal-h2 -Dtest=ContentPublicControllerSeoTest,SimbioSphereOpenApiContractTest,AuthorizationMatrixTest -Dit.test=ContentSeedH2IT verify`. Maven falló antes de compilar al resolver el descriptor de `tika-core`: no pudo resolver `org.testcontainers:testcontainers-bom:pom:1.21.3` para el repositorio activo en modo offline. Esto describe el fallo de resolución de esta ejecución, no una comprobación de ausencia física del artefacto. No se ejecutaron tests. El settings temporal se eliminó.

## 2026-11-17 — Logo de Home y media HERO

- `content` permite referenciar media desde bloques HERO además de IMAGE; al publicar, ambos requieren medio registrado, limpio y con alt ES/EN. El render público conserva el contenido y la imagen del bloque.
- El owner eligió y autorizó la variante PNG transparente del logo, con alt ES «Logotipo de Simbio» y EN «Simbio logo». Se incorporó al pipeline local fingerprintado; la Home la sirve desde el manifiesto con tamaño intrínseco 2869×929, carga eager y `fetchpriority=high`.
- Se añadieron tests para preview de medio HERO, rechazo de medio no limpio y para la inclusión fingerprintada del logo y su uso prioritario en la plantilla. `npm run build` pasó y el manifiesto apunta al PNG fingerprintado generado. Maven se intentó solo con el goal `test` y se detuvo antes de compilar al cargar el modelo por BOMs no resolubles en modo offline; los tests no se ejecutaron.
- WEB-001 sigue parcial hasta ejecutar los tests Java y medir Lighthouse/Core Web Vitals; WEB-007 y las aprobaciones de dominio, contenido legal/comercial y equipo siguen abiertas.

## 2026-11-17 — Versión oficial de H2

- Por decisión explícita del owner, H2 `2.2.224` pasa a ser la versión oficial para tests automatizados y el perfil `local-h2`; se fijó en `pom.xml` para ambos scopes y se actualizó ADR-001, README e INVENTARIO.
- El JAR indicado está presente en la caché Maven local. Esto no valida por sí solo el build ni las pruebas; el intento offline de Maven sigue bloqueado por dependencias transitivas del BOM (`org.apache.activemq:activemq-bom:6.2.10`) no disponibles en caché.
- No se afirma validación de SQL Server. La suite H2 queda pendiente de ejecución con esta versión.

## 2026-11-17 — Verificación de caché Maven

- Con JDK 21, `mvnw -o -B -s C:\Users\x455035\.m2\settings-s-portal.xml -Plocal-h2 test-compile` resolvió el modelo del proyecto, pero falló en dependencias: faltan `org.apache.tika:tika-core:3.2.3` y `org.sejda.imageio:webp-imageio:0.1.6`; Maven tampoco encuentra `commons-io:commons-io:2.20.0` en el repositorio local seleccionado.
- Inspección de archivos: H2 `2.2.224`, varios BOM importados y Commons IO `2.20.0` existen en `C:\Users\x455035\.m2\repository` o en `C:\Users\x455035\.m2`, pero la caché está repartida entre ambas raíces. Tika y WebP tienen POMs/archivos `.lastUpdated`, pero no sus JARs en ninguna de las dos raíces. `settings.xml` predeterminado selecciona `.m2`; `settings-s-portal.xml` selecciona `.m2\repository`.
- `maven-dependency-plugin:3.8.1:go-offline` no pudo ejecutarse offline porque el plugin no está registrado para el ID de repositorio disponible en esa caché. El intento online sin configuración específica volvió a fallar con `PKIX` al conectar con Nexus. Ningún test llegó a ejecutarse.

## 2026-11-17 — Revisión posterior de dependencias descargadas

- Repetido `mvnw -o -B -s C:\Users\x455035\.m2\settings-s-portal.xml -Plocal-h2 test-compile` con JDK 21. El proyecto llega a la resolución de dependencias, pero falla antes de compilar porque no están disponibles en `C:\Users\x455035\.m2\repository` los JAR de `org.apache.tika:tika-core:3.2.3`, `commons-io:commons-io:2.20.0`, `org.sejda.imageio:webp-imageio:0.1.6`, `org.commonmark:commonmark:0.24.0` y `com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1`.
- Comprobación directa: H2 2.2.224 y Bucket4j 8.10.1 sí están en esa caché; los cinco JAR indicados arriba no. Maven avisa que faltan los POM de commonmark y OWASP Java HTML Sanitizer. No se ejecutó ningún test.

## 2026-11-17 — Revisión de prerrequisitos de PROMPT 08

- Se confirmó que `C:\Users\x455035\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar` existe y que `pom.xml` fija H2 2.2.224. La ejecución focal de Maven con JDK 21 y `-Plocal-h2` se detuvo al leer el modelo del proyecto: falta el BOM importado `org.apache.activemq:activemq-bom:6.2.10` en la caché offline. Los tests no llegaron a compilar ni ejecutarse; H2 no es la causa de ese fallo.
- No existe implementación funcional del módulo `content` en `src/main`; por ello WEB-001..007 y SEO-001..002 continúan bloqueadas por sus dependencias CMS y no se añadieron rutas que omitan el API de contenido publicado.
- SIM-002 sigue condicionada a DEMO-01 y a la revisión/acuerdo de los owners Portal y DEMO. El borrador existente en `05-integracion-simbiosphere.md` no equivale a ese acuerdo. No se hicieron llamadas a DEMO ni cambios en el workspace externo.
- Resultado: Prompt 08 no puede completarse todavía. Siguiente paso: habilitar resolución Maven con la configuración/cache autorizada, completar y validar las predecesoras de CMS, y obtener la revisión de DEMO-01; después ejecutar los IDs de Prompt 08 respetando su DAG.

## 2026-11-11 — PROMPT 02, F0 Fundaciones (FND-001..FND-010)

- FND-001: el spike de versiones quedó registrado en ADR-001. Boot 4.1.2, Java 21 y Modulith 2.1.2 fueron probados con H2. El spike ejecutó 2 tests sin fallos; Flyway avisó que H2 2.4.240 supera la última versión declarada como verificada por Flyway (2.3.232).
- FND-002..FND-004: creado el build Maven 3.6.3 con exigencia de Java 21, aplicación en `com.simbio.core`, kernel `shared`, SPIs, módulos `shared`/`config` abiertos y gates Modulith/ArchUnit.
- FND-005..FND-007: Compose para servicios opcionales y validación estática; baseline/convenios Flyway para SQL Server con persistencia de pruebas H2; perfiles, propiedades tipadas y reglas de secretos.
- FND-008: pipeline CI con JaCoCo, Failsafe, Dependency-Check y reglas de plantillas. Se añadió la comprobación de paridad de claves ES/EN para los futuros bundles por módulo.
- FND-009..FND-010: CSRF, cookies, cabeceras, CSP con nonce, logs JSON de producción, correlación, enmascarado de PII y Actuator en puerto de gestión.
- Verificación final con JDK 21: `.\mvnw.cmd -o -B -s .\temporary-maven-settings.xml -Pci verify` — BUILD SUCCESS, 32 tests Surefire + 2 Failsafe, sin fallos ni errores; JaCoCo informó que se cumplieron las comprobaciones configuradas. El settings temporal, sin credenciales, alineó el mirror ID con la caché y se eliminó después.
- Dependency-Check permanece configurado en CI, pero ejecutar el análisis, revisar licencias o lanzar GitHub Actions remotamente no son requisitos para cerrar FND. No se afirma ningún resultado del análisis.
- No se arrancaron Docker ni SQL Server, conforme al plan. H2 es el único motor de pruebas; la compatibilidad con SQL Server y las migraciones sobre SQL Server siguen sin verificar. No hay módulos de negocio ni bundles de traducción todavía.

## 2026-11-11 — PROMPT 03, F0 UI kit y gobernanza (UIK/GOV)

- Se empezó UIK-001..009 en orden de dependencias: configuración local de Tailwind, HTMX 2.0.11 y Alpine CSP 3.17.5; manifest SHA-256 para CSS/JS; tokens claros/oscuros y comprobación WCAG; locale ES/EN; layouts, errores, menú/tema accesibles y fragments neutrales de formulario, tabla, paginación y toast.
- Se añadió el puerto `shared.spi.ThemePreference`; hasta que exista un proveedor de identidad, no se afirma persistencia de tema en perfil autenticado.
- Decisión del owner: en F0 el tema se persiste en cookie/localStorage también para usuarios autenticados; la persistencia en perfil se difiere a la futura integración con Identidad. Se eliminó el fallo cuando no existe proveedor, manteniendo el SPI opcional.
- La infraestructura Playwright + axe cubre la matriz ES/EN × claro/oscuro × móvil/escritorio para el shell público. Se decidió alinear Playwright con la versión instalada 1.62.1 y Tailwind con 3.4.19. Chromium y axe están presentes en `node_modules`, pero no se ejecutó E2E por falta de servidor actualizado.
- Decisión del owner (2026-11-12): retirar las fuentes web personalizadas de F0 y usar `system-ui, sans-serif`, sin archivos tipográficos ni CDN.
- GOV-001..006: iniciados las normas P01–P12, instrucciones Copilot y skill del portal, inventario/diario, diagramas, test de inventario↔Modulith y validador ADR.
- Tailwind 3.4.19 compiló la entrada CSS y el script local generó los assets fingerprintados y el manifest. Se copiaron desde `temp\node_modules` HTMX 2.0.11, Alpine CSP 3.17.5, axe Playwright 4.10.2 y sus dependencias runtime; `temp` se eliminó. La validación posterior detectó que `package-lock.json` aún fija Playwright 1.56.1 y Tailwind 4.1.11; por la restricción del owner no se ejecutaron comandos npm para regenerarlo.
- Con JDK 21 y modo offline, compilaron las fuentes principales/tests y pasaron `ThemeContrastTest` (2), `DocumentationArchitectureTest` (3), `ThemePreferenceTest` (3), `PreferenceControllerTest` (1), `WebViewsTest` (3), el test focalizado de nonce/layout (1) y el de tipografía del sistema (1). `FrontendAssetPipelineTest` falla por las versiones obsoletas del lockfile. En `WebViewsTest`, MockMvc comprueba el status de las rutas y el contenido de `/error` con status y locale simulados, ya que no ejecuta el despacho de error del contenedor tras `sendError`. No se ejecutaron npm ni `mvn verify`.
- Decisión del owner: en F0 el tema se persiste en cookie/localStorage también para usuarios autenticados; la persistencia en perfil se difiere a la futura integración con Identidad. Se eliminó el fallo cuando no existe proveedor, manteniendo el SPI opcional.
- La configuración Maven personal apunta su repositorio local a `.m2`, mientras la caché disponible está en `.m2/repository`; `settings-s-portal.xml` aísla el override y el perfil local, sin tocar el original. La salida anterior mostró Java 17 y PKIX; se configuró JDK 21 y modo offline. El primer arranque identificó que `jakarta.persistence-api` estaba declarado directamente con scope `test`, prevaleciendo sobre el scope `compile` que aporta el starter JPA; se eliminó esa declaración redundante y `dependency:tree` confirmó scope `compile`. También se movió el frontend plugin al perfil `frontend-build` activo por defecto, que `local-h2` desactiva; CI activa explícitamente ambos perfiles. La app arrancó con H2. Playwright/axe pasó 9/9 tras corregir el landmark duplicado, el destino del skip link y el ancho táctil de enlaces. No se ejecutó npm ni `mvn verify`; queda pendiente sincronizar `package-lock.json`.

## 2026-11-11 — PROMPT 04, inventario central y captura backend/BD

- ERR-001: creado el SPI `shared.spi.ErrorReporter` y `config.errortracking.ErrorInventoryService`; el servicio carga JSON al inicio, deduplica por huella SHA-256, agrega ocurrencias y persiste mediante reemplazo atómico fuera de webroot/SQL. Límites configurables de entradas, bytes y longitud; solo se evictan registros RESOLVED/IGNORED. La capacidad agotada se señala explícitamente sin sobrescribir errores abiertos.
- ERR-002: filtro servlet, resolver MVC y aspecto de repositorios capturan fallos backend/SQL sin cambiar el flujo normal de resolución ni reemplazar la excepción original. Se deduplica la misma excepción/cadena causal por request. Solo se guardan metadatos técnicos permitidos; no se persisten mensajes de excepciones ni SQL crudo.
- El path por defecto es `logs-error/error-inventory.json`; para operación debe dirigirse a un volumen privado de una única instancia. La captura no depende de SQL Server. Se alinearon las importaciones existentes de Jackson y `ErrorController` con Spring Boot 4 para que compile el workspace actual.
- ERR-003 no se inició: UIK-001 todavía no tiene build/recursos frontend verificados, prerrequisito explícito; no se modificó ese carril. ERR-004 queda fuera del alcance.
- Verificación Java 21: compilación de fuentes principales, 7 UT de error-tracking, 4 comprobaciones de arquitectura (11 tests Surefire en total) y 1 IT H2 aprobados. Se usó un POM temporal de validación porque el POM normal resuelve en modo offline artefactos ajenos a este carril que faltan en caché (`spring-modulith-docs:2.1.2` y `frontend-maven-plugin:1.15.1`); el POM temporal se eliminó. No se ejecutó el `verify` completo.

## 2026-11-13 — Reanudación de PROMPT 03

- El `package-lock.json` de la raíz se sincronizó con el manifiesto desde `logs-error/.package-lock.json`; `FrontendAssetPipelineTest` pasó 5/5 con JDK 21 y Maven offline usando el ID de caché `nexus`, sin descargar dependencias.
- `npm run build` pasó tras `mvn clean`, generando CSS/JS minificados, assets fingerprintados y manifest. Node usó las dependencias ya presentes; no se ejecutó `npm ci`.
- La batería focal UI/GOV pasó: 39 tests, 0 fallos/errores; incluye pipeline, contraste, tema, locale/paridad, navegación por permisos, vistas, CSRF, reglas de templates, arquitectura, gobernanza y ADR.
- `npm run e2e -- --list` descubre 13 pruebas. Se añadieron pruebas Playwright para persistencia del tema, cambio de idioma, desbordamiento/foco del menú y CSRF/errores HTMX. No se ejecutaron en navegador: el segundo intento con el lanzador Java volvió a reportar `Maven build failed with exit code 1`, sin mostrar la causa.
- Se corrigió el fixture de `ProdProfileTest` añadiendo valores sintéticos válidos de `portal.error-tracking.*`, para que el test alcance la validación de `portal.base-url`. Después, `mvn clean verify -Plocal-h2` terminó en BUILD SUCCESS: 73 tests entre Surefire/Failsafe, 0 fallos/errores y JaCoCo aprobado.
- El `java` del PATH es JDK 17, y se reprodujo que Maven bajo ese JDK falla el Enforcer con `S-portal requires Java 21`. Se configuró `JavaSE-21` como runtime predeterminado del workspace y se recargó VS Code. El lanzador Java volvió a fallar, sin exponer stderr; por tanto, el runtime 17 es un problema confirmado para Maven cuando se usa ese Java, pero no se ha probado que fuera la causa del fallo del lanzador después de la recarga.
- Para respetar que no se ejecute `npm ci`, se añadió `-Dfrontend.skip=true` a las opciones Maven locales de VS Code y se regeneraron CSS/JS y manifest fingerprintados mediante `npm run build`, con el `node_modules` existente. El lanzador Java siguió fallando con `Maven build failed with exit code 1`; las salidas disponibles de Maven para Java solo muestran el repositorio local y no contienen el stderr de esa invocación.
- Los validadores GOV-001..006 pasan. El ciclo Maven `frontend-build` no se ejecutó porque ejecuta `npm ci`; el build frontend limpio sí se validó directamente con las dependencias ya instaladas.
- Tras actualizar la evidencia de PROMPT 03, los validadores GOV/arquitectura/ADR focalizados volvieron a pasar: 11 tests, 0 fallos/errores.
- No se declara éxito de PROMPT 03: `ProdProfileTest` y el `verify` completo ya pasan, pero UIK-001 sigue parcial por no ejecutar el perfil Maven que invoca `npm ci`; UIK-004/005/007/008/009 siguen parciales por E2E/axe/matriz pendientes y el bloqueo del lanzador Java aún sin causa confirmada. No se hicieron más intentos tras el último fallo.

## 2026-11-13 — Validación E2E pendiente de PROMPT 03

- Playwright mostró que el `<nav>` podía crecer más allá del viewport y que por eso la comparación de overflow no activaba el drawer. Se limitó el nav al espacio flexible disponible y la medición ahora restaura temporalmente la lista antes de recalcular, evitando una lectura de ancho cero cuando ya está oculta.
- El control sintético de la prueba HTMX no tenía contenido y era invisible; se le dio un nombre visible/accesible. El E2E de overflow ahora comprueba también que no aparece scroll horizontal.
- HTMX insertaba en runtime un bloque `<style>` sin nonce, incompatible con CSP. Se desactivó esa inyección por `meta[name=htmx-config]`, se trasladaron las reglas de indicador a CSS local fingerprintado y se añadió una aserción E2E que exige ausencia de estilos inline sin nonce.
- `npm run build` pasó con el `node_modules` existente. El JAR actualizado se empaquetó offline con Java 21 y la caché Maven local; se arrancó `local-h2` y `npm run e2e` terminó 13/13 tras el ajuste CSP, con axe sin violaciones y capturas de la matriz visual en `visual/`.
- UIK-002..009 quedan validadas por la evidencia acumulada de MVC, accesibilidad y E2E. La integración Maven del frontend no se ejecutó localmente y queda fuera del gate del alcance.

## 2026-11-13 — Ajuste de alcance del PROMPT 03

- Se retiró la tarea de integración Maven/`npm ci` del alcance obligatorio de PROMPT 03 y del roadmap. El pipeline local existente, el lockfile, los assets fingerprintados y su uso en CI se conservan; no se exige reinstalar dependencias para cerrar el prompt.
- Se reasignaron las dependencias de UIK-002/003 a FND-002 y la de ERR-003 al layout UIK-006; RF-UI-08 queda cubierta por UIK-008. Se recalculó el camino crítico del roadmap.
- PROMPT 03 queda cerrado dentro del alcance revisado: UIK-002..009 y GOV-001..006 validadas; la integración Maven local del frontend no es un gate y sigue disponible para CI.

## 2026-11-13 — Reanudación de PROMPT 04 (ERR-003)

- UIK-006 ya estaba validada, por lo que se retomó ERR-003. Se añadió el reporter local fingerprinted para errores JS no controlados, rechazos, errores de recursos, red y respuestas HTMX 5xx, junto con DTO/controller de intake en `config`.
- El cliente envía únicamente evento/nombre allowlisted, stack reducido a marcos JS fingerprinted, locale, correlation ID y ruta allowlisted; no envía mensajes libres, DOM, formularios, cookies ni headers. El servidor fija `FRONTEND`, valida y sanea de nuevo, exige `Origin` same-origin y JSON no comprimido, limita el cuerpo a 4 KiB y mantiene CSRF. Rate limit: máximo cinco informes por minuto en cliente y token bucket de 30 de ráfaga/30 por minuto en servidor. Las excepciones de payload rechazado en este endpoint no se registran recursivamente como errores BACKEND.
- La primera ejecución de `mvn -o -s settings-s-portal.xml -Plocal-h2 verify` descubrió un defecto en vistas: Thymeleaf 3.1 deshabilita `#request` por defecto. Se reemplazó el acceso desde la plantilla por `portalCorrelationId` expuesto con `WebViewAdvice`, y `WebViewsTest` pasó 4/4.
- La repetición de `mvn -o -s settings-s-portal.xml -Plocal-h2 verify` pasó con 76 tests Surefire y 3 Failsafe, sin fallos/errores; JaCoCo aprobó. `npm run build` pasó y Playwright pasó 14/14 contra la aplicación local actualizada, incluido el E2E de telemetría.
- El log capturado mostró que Spring Boot inició correctamente en unos 36 segundos. Los probes previos habían sido demasiado tempranos; no había fallo de arranque. Se verificaron HTTP 200, el asset `error-reporter` fingerprinted y el meta de correlation ID. La instancia temporal se detuvo después de las pruebas y los puertos 8092/8093 quedaron libres. ERR-001..003 quedan validadas para el perfil local H2; ERR-004 queda fuera de PROMPT 04. SQL Server, multi-réplica, retención y parámetros operativos productivos siguen sin verificar/aprobar.
- Decisión del owner: ERR-003 requiere opt-in explícito. Se implementó banda no modal ES/EN, denegada por defecto, aceptar/rechazar y acceso desde el pie para cambiar o retirar; localStorage versionado, sin identidad ni evidencia backend. Fallo de escritura mantiene el envío desactivado. El copy es provisional y queda pendiente de revisión legal antes de producción.
- Tras añadir consentimiento, `npm run build` pasó, `mvn -o -s settings-s-portal.xml -Plocal-h2 verify` volvió a pasar (76 Surefire + 3 Failsafe; JaCoCo aprobado) y Playwright pasó 15/15, incluida la prueba de consentimiento y fallo de almacenamiento. La aplicación arrancó en el puerto aislado 8092; se detuvo al terminar y 8092/8093 quedaron libres. ERR-001..003 quedaron validadas en H2 local; SQL Server, escalado single-writer, retención y copy legal siguen como pendientes de producción, no como gates de cierre de PROMPT 04.

## 2026-11-13 — Cierre de PROMPT 04

- PROMPT 04 y ERR-001..003 quedan COMPLETADOS dentro del alcance de desarrollo: `npm run build`, `mvn -o -s settings-s-portal.xml -Plocal-h2 verify` (76 Surefire, 3 Failsafe, JaCoCo aprobado) y Playwright (15/15) pasaron. También pasaron los gates focalizados de documentación, gobernanza, paridad i18n y plantillas (19 tests).
- No se exige SQL Server real, Docker, Testcontainers, PRE ni aprobaciones productivas para cerrar esta entrega. La compatibilidad SQL Server, el escalado multi-réplica/rate limit, la retención y la revisión legal del texto provisional siguen identificados para antes de producción; no se declaran verificados.

## 2026-11-13 — Evaluación de PROMPT 06 (CMS-001..013)

- PROMPT 06 queda **bloqueado antes de implementar CMS-001**: el DAG de `10-roadmap-backlog.md` requiere FND-006 y FIL-001 para el primer ítem. FND-006/UIK-008 tienen evidencia en la fundación y el UI kit; FIL-001 no está terminada: `files` solo tiene `package-info.java` y una migración, sin puerto/API de almacenamiento ni adaptador/tests que satisfagan la ficha.
- No se creó el módulo `content` ni migraciones, rutas o plantillas CMS fuera de orden. Los escalones posteriores también dependen de AUD-001 (CMS-002), IAM-012 (CMS-005), y FIL-003/FIL-004 (CMS-008); `audit` igualmente solo tiene `package-info.java` y migración. No hay tests `TC-CMS-001..013` ni implementación CMS en el workspace. El contrato UI↔backend de CMS sí está documentado en `docs/plan/contratos-ui-backend.md`.
- Ownership previsto: módulo `content`, su API pública y tablas propias; consume `files.api` y `audit.api`. Controles requeridos al reanudar: CONTENT_EDIT en servidor, CSRF, auditoría mínima, preview no pública, allowlist de sanitización antes de cualquier `th:utext`, medios CLEAN con alt por locale y pruebas negativas de permisos/XSS/CSRF. La aprobación de textos legales y del copy comercial/gratuidad sigue pendiente: CMS-010/012 deben mantener esos textos en borrador y CMS-013 no debe presentarlos como condiciones oficiales sin aprobación.
- No se ejecutaron pruebas CMS porque no existe implementación y el prerrequisito de almacenamiento está incompleto. Estado: CMS-001..013 pendientes/bloqueadas por dependencias; ninguna se declara completada. Próximo paso seguro: completar y validar FIL-001, AUD-001 e IAM-012 según su DAG antes de abrir el carril CMS.

## 2026-11-13 — Evaluación bloqueada de PROMPT 08

- No se implementaron WEB-001..WEB-007 ni SEO-001..SEO-002: no existe aún el módulo funcional `content` y sus dependencias CMS están pendientes. WEB-001/003/004 dependen de CMS-004/012; WEB-005 de CMS-004/009; WEB-006 de CMS-010; SEO-001 de CMS-004. Por transitividad, WEB-002, SEO-002 y WEB-007 también quedan bloqueadas. UIK-006 está disponible, pero no satisface la dependencia CMS-010.
- SIM-002 queda bloqueada por falta de evidencia de DEMO-01, aunque FND-002 está disponible. `05-integracion-simbiosphere.md` conserva un borrador marcado como propuesta; no hay lint/contract test ni revisión/acuerdo de los owners Portal y DEMO, y no se generaron stubs WireMock.
- Se confirmó que `docs/plan/contratos-ui-backend.md` ya propone las rutas públicas pertinentes; no se inventaron rutas ni se cambió el contrato. No se modificó código ni el repositorio DEMO, ni se afirma aprobación legal/de marca o disponibilidad del API externo.
- Siguiente paso seguro: completar las predecesoras CMS según el DAG y obtener la decisión/evidencia DEMO-01; después reanudar las tareas habilitadas de WEB/SEO y cerrar SIM-002 con validación y revisión de ambos owners.
- Validación documental no ejecutada: el runner directo no detectó pruebas Java; el intento Maven focalizado con `-o` no pudo resolver `org.apache.activemq:activemq-bom:6.2.10` desde la caché local. El `java` del PATH es JDK 17; no se descargaron dependencias.

## 2026-11-13 — PROMPT 05, avance FIL-001

- Se completó parcialmente FIL-001 sobre el código existente en `files`: adaptador de disco privado y cuarentena con claves UUID, normalización de rutas y rechazo del webroot, streaming de lectura, autorización antes de consultar metadata, y persistencia JPA de metadata. La ruta se configura con `PORTAL_FILE_STORAGE_ROOT`.
- Se añadieron tests para almacenamiento/lectura, rechazo de traversal, denegación de descarga y persistencia H2. Se añadió un scanner explícitamente no disponible que devuelve `UNAVAILABLE`; por ello las cargas permanecen `QUARANTINED` y no se descargan hasta FIL-003. No se implementó ruta HTTP ni se declaró el volumen/backup productivo resuelto.
- **Estado: parcial, no validado.** Maven con JDK 21 no pudo leer el POM por errores PKIX al descargar BOMs desde el Nexus corporativo; las pruebas nuevas no llegaron a ejecutarse. No se desactivó TLS ni se descargaron dependencias por otra vía.
- Próximo paso: recuperar la confianza/caché Maven autorizada y ejecutar los tests focalizados. Solo después de validar persistencia, stream y rechazo podrá cerrarse FIL-001 y habilitarse CMS-001; FIL-002..004 siguen pendientes.

## 2026-11-13 — Continuación de PROMPT 05

- Se integró la persistencia existente de metadata con el contrato `FileStorage`; el adaptador local separa cuarentena y ficheros limpios, y solo permite abrir un archivo después de moverlo a CLEAN. Se conservan claves aleatorias, checks de ruta, rechazo del webroot y nombres genéricos para no guardar nombres de carga potencialmente identificativos.
- Se añadió el adaptador ClamAV por protocolo INSTREAM, configurado por variables de entorno; una caída o respuesta inválida mantiene el fichero en cuarentena. También se añadieron tests para validación MIME/firma, lectura autorizada, estado de cuarentena y el servicio de auditoría. El H2 de test/local crea los esquemas de módulos.
- El primer intento Maven offline no encontró el plugin Enforcer; con la configuración Maven local, la descarga de Bucket4j desde JFrog respondió 401. Maven Central respondió 403. No se desactivó TLS y las pruebas no llegaron a compilarse/ejecutarse; por tanto AUD-001 y FIL-001..003 siguen parciales/no verificadas.
- IAM-001/002/004/005/012/014, AUD-002 y FIL-004 siguen pendientes; la identidad no está implementada y CMS-001 sigue bloqueada hasta validar FIL-001. No se declara ningún ID del Prompt 05 completado.

## 2026-11-13 — Continuación de PROMPT 05: pruebas y estado

- Se añadieron pruebas H2 para persistencia/búsqueda/retención de auditoría e idempotencia del seed de identidad, además de pruebas del protocolo ClamAV para respuestas healthy/infected y servicio no disponible.
- `mvn -o '-Dtest=ClamAvMalwareScannerTest,AuditPersistenceH2IT,IdentitySeedH2IT' test` no llegó a compilar: el POM no se pudo resolver offline porque falta en la caché local el BOM transitivo `org.apache.activemq:activemq-bom:6.2.10`. No se descargaron dependencias por vías alternativas ni se ejecutaron tests.
- Se alinearon README e INVENTARIO con la implementación observable. AUD-001/002, FIL-001..004 e IAM-001/002/004/005/012/014 siguen implementados parcialmente pero no validados; no se declara completado ningún ID. SQL Server, Docker y el despliegue externo siguen sin validar. CMS-001 continúa bloqueada hasta que sus dependencias queden validadas.

## 2026-11-17 — Continuación de PROMPT 05

- Se retomó sobre el workspace actualizado, sin asumir el inventario previo: ahora hay módulos con UI/API y pruebas añadidas de auditoría, ficheros e identidad. Se corrigió el test H2 de metadata tras el cambio del constructor `FileMetadataEntity`; la prueba comprueba también el nombre genérico almacenado. Se endureció el mapeo de identidad para rechazar roles/permisos desconocidos en vez de convertir roles desconocidos a CLIENT, con prueba negativa; el login sin destino seguro ahora vuelve a `/es`.
- El `settings.xml` activo apunta a `C:\Users\x455035\.m2`, mientras los BOMs añadidos están en `C:\Users\x455035\.m2\repository`; con el repositorio predeterminado Maven falla antes al resolver los BOMs. Al ejecutar con `-Dmaven.repo.local=C:\Users\x455035\.m2\repository -Plocal-h2`, el POM sí se resuelve, pero Maven informa que faltan los JAR `org.apache.tika:tika-core:3.2.3`, `commons-io:commons-io:2.20.0` y `org.sejda.imageio:webp-imageio:0.1.6` (en caché solo hay `.jar.lastUpdated`). El JDK activo es 17.0.21 y el proyecto requiere Java 21. Ningún test llegó a compilarse ni ejecutarse; no se deshabilitó TLS ni se cambió la configuración corporativa.
- Estado: AUD-001/002, FIL-001..004 e IAM-001/002/004/005/012/014 continúan **parciales/no validadas**. No se declara cerrado ningún ID; falta ejecutar las pruebas unitarias, H2, MVC, arquitectura y frontend correspondientes cuando el acceso Maven autorizado vuelva a estar disponible. CMS-001 permanece bloqueada por la validación pendiente de FIL-001.
- Se corrigió el fixture de FIL-004 para usar un JPEG con un bloque EXIF/GPS estructurado y se cerró el helper de test que había quedado mal delimitado. La prueba focal no pudo verificar la corrección porque Maven se detuvo al procesar el POM; los tests no llegaron a compilarse ni ejecutarse. El JDK 21 y la resolución Maven autorizada siguen siendo requisitos para cerrar Prompt 05.
- AUD-002 añade una prueba MVC de página 2, historial limitado a la entidad solicitada y etiquetas/caption/encabezados accesibles. Los enlaces de paginación conservan ahora los filtros de entidad. La ejecución focal de ambas pruebas (`AuditControllerMvcTest` y `ImageVariantServiceTest`) vuelve a quedar bloqueada al resolver BOMs offline, antes de compilar.
- Revisión de caché a las 10:35: con `-Dmaven.repo.local=C:\Users\x455035\.m2\repository -Plocal-h2`, Maven avanzó a la resolución de dependencias y reportó cinco artefactos ausentes: Tika 3.2.3, Commons IO 2.20.0, WebP 0.1.6, CommonMark 0.24.0 y OWASP Java HTML Sanitizer 20240325.1. Los tres primeros tienen POM pero no JAR; los dos últimos no tienen POM ni JAR. El plugin frontend sí existe, pero su marcador de caché es `nexus` mientras el POM solicita `maven-public`. Los tests no llegaron a compilarse ni ejecutarse.
- Corrección a las 10:54: se verificaron las rutas indicadas por el usuario bajo `C:\Users\x455035\.m2` (repositorio local activo). Tika 3.2.3, Commons IO 2.20.0 y WebP 0.1.6 sí tienen JAR/POM; el reporte anterior de que faltaban fue incorrecto porque consultó la subcarpeta `repository`. Sus marcadores, y los de BOMs presentes, usan el ID `central-8fac3ebd6edbaca3c794783fa38088af0aa128e7`; Maven offline intenta resolver con `maven-public` y los rechaza por discrepancia de ID. CommonMark 0.24.0 y OWASP Java HTML Sanitizer 20240325.1 continúan ausentes en ambas rutas. La ejecución focal se detuvo durante la resolución de BOMs, antes de compilar; Java activo 17.0.21, requisito del proyecto 21.
- Revalidación a las 11:18 en `C:\Users\x455035\.m2`: ahora Maven offline resuelve el modelo y avanza más allá de los BOMs y de Tika/Commons IO/WebP. Los únicos artefactos que reporta como no resueltos son `org.commonmark:commonmark:0.24.0` y `com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1`; faltan sus POM/JAR. Commons IO conserva marcador `nexus`, pero no aparece como error en esta ejecución. No hubo compilación ni tests; Java activo 17.0.21 frente al requisito 21.
- Revalidación a las 11:28: los cinco artefactos reportados antes (Tika, Commons IO, WebP, CommonMark y OWASP Java HTML Sanitizer) ya tienen JAR/POM en el repositorio local activo. Maven no los reporta ahora; falla al leer el descriptor de Enforcer por `org.apache.maven:maven-parent:pom:42`. Su POM sí existe, pero `_remote.repositories` solo registra `central-8fac3ebd6edbaca3c794783fa38088af0aa128e7`, mientras el intento offline solicita `maven-public`. No se llegó a compilar ni ejecutar tests.

## 2026-11-17 — Implementación parcial de PROMPT 06 (CMS-001..013)

- Se añadió el owner `content`: entidades y migración de páginas, traducciones, bloques, snapshots, redirects, referencias/alt de medios, equipo y versiones legales; API para evento de publicación con IDs, condiciones, documentos vigentes y lectura de miembros del equipo.
- Se implementaron editor CMS, preview privada noindex/no-store, publicación/versionado/restauración, rutas públicas ES/EN, fallback/canonical/redirect, sanitizer CommonMark + OWASP allowlist, variantes responsive de imágenes, medios CLEAN, seed idempotente, filtro/paginación de traducciones y copy neutral de condiciones sin activar pagos.
- Se mantuvo la propiedad de archivos en `files`: `content.media_asset` solo almacena referencia y alt; `files.api` aporta metadata de archivos limpios. La UI ya no ofrece un checkbox para autoaprobar materiales del equipo y el guardado fuerza estado de borrador; `TeamMemberServiceTest` comprueba que no se activa ni aprueba. La autoridad de aprobación legal/comercial y de materiales sigue sin contrato, de modo que no se permite aprobarlos desde CMS.
- Se añadieron tests UT/IT/MVC focales y se corrigió el DAG de módulos, la configuración H2 duplicada, el modelo de datos, el mapa modular, el contrato UI↔backend, README, inventario y ADR-008. H2 2.2.224 sigue siendo el motor definido; no se ha ejecutado la suite.
- El reintento focal con `-Dmaven.repo.local=C:\Users\x455035\.m2\repository -Plocal-h2` falló antes de compilar al resolver CommonMark 0.24.0: `certificate_unknown`/PKIX del Nexus corporativo. OWASP HTML Sanitizer tampoco está cacheado. No se desactivó TLS ni se ejecutaron tests; no se aplicó la migración a SQL Server.
- Estado: CMS-001..013 **implementadas parcialmente/no validadas**. FIL-001/003/004, AUD-001 e IAM-012 siguen sin validación; además falta aprobar el flujo/owner de revisión legal, comercial y de materiales. Ningún ID se marca completo.
- Revisión precisa de caché (11:17): `settings.xml` configura `C:\Users\x455035\.m2`. Al ejecutar Maven offline sin `-Dmaven.repo.local`, el error lista solo `org.commonmark:commonmark:0.24.0` y `com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1`; las otras dependencias señaladas antes (Tika, Commons IO y WebP) sí tienen JAR/POM en esa ruta. También están H2 2.2.224 y el artefacto declarado `com.bucket4j:bucket4j-core:8.10.1`; `bucket4j_jdk17-core` no está declarado. La subcarpeta `repository` es otra caché incompleta y no debe forzarse. El intento offline se detuvo antes de compilar; plugins y transitivas de los dos artefactos ausentes aún quedan por verificar.
- Continuación funcional: el editor ahora selecciona medios CLEAN mediante un control HTML nativo y muestra previews privadas; `mediaLibrary` filtra referencias que `files.api` ya no reporta CLEAN. CMS-010 permite proponer fecha efectiva de borrador en `Europe/Madrid` (almacenada UTC) y la consulta conserva la versión previa hasta la fecha, sin añadir aprobación automática. Se añadieron UT/H2 y cobertura del seed EN; pendiente compilar y ejecutar tests.
- Gate ejecutado: `mvn -Plocal-h2 verify` se detuvo antes de compilar porque la conexión a Nexus rechazó la cadena de certificado (`certificate_unknown`, PKIX) al resolver el descriptor de `org.commonmark:commonmark:0.24.0`. No se ejecutaron pruebas unitarias ni de integración; no se desactivó TLS ni se declaró el CMS validado.
- Decisiones para continuar (12:19): el usuario preparará JDK 21 y la CA/truststore corporativa de Nexus; esperar aviso antes de reintentar una sola vez `mvn -Plocal-h2 verify`. Mantener textos legales/comerciales en DRAFT/PENDING hasta que Legal/Comercial asigne owner y flujo aprobador; usar solo fixtures sintéticos para perfiles del equipo hasta disponer de consentimiento/licencias; diferir el consumidor `request` de CMS-013 hasta F3, sin crear un módulo prematuro. Estas decisiones no validan ni completan las tareas CMS.
- Corrección operativa (12:32, actualizada 16:30): Maven se limita a compilar, ejecutar tests (incluido `verify` si se requiere Failsafe/H2) o arrancar la aplicación. El usuario proporciona las dependencias locales. No se inspecciona ni comunica metadata/origen de la caché ni se solicita descargar dependencias otra vez; si una operación no inicia, se registra solo que no se ejecutó y se continúa trabajo independiente. Sin omitir gates ni afirmar resultados no observados.
- Gate ejecutado (13:39): `.\mvnw.cmd -o -Plocal-h2 verify` no llegó a compilar ni ejecutar pruebas. Por instrucción del usuario, no se investiga ni se vuelve a comunicar el diagnóstico específico de resolución local; este bloqueo no detiene tareas independientes de Prompt 06. No se afirma que el gate haya pasado.
- Pruebas focales CMS: se intentó el runner integrado sobre los tests Java del módulo `content`; informó que no encontró tests para esos archivos, por lo que tampoco ejecutó pruebas. No hay evidencia nueva para cerrar CMS-001..013.
- Actualización de caché (11:30): CommonMark y OWASP ya tienen JAR/POM, aunque sus marcadores corresponden a `central`; Commons IO tiene archivos pero marcador `nexus`. Tika, WebP, H2 y Bucket4j están marcados para `maven-public`. El nuevo `mvn -o -Plocal-h2 -DskipTests test` se detiene antes de resolver dependencias del proyecto: `maven-enforcer-plugin:3.5.0` requiere `org.apache.maven:maven-parent:pom:42`, cuyo POM sí existe localmente pero solo está registrado bajo `central`; `org.apache:apache:pom:32` también está presente con ese marcador. Así, no hay ausencia física de los artefactos CMS revisados, pero sí marcadores de origen incompatibles con la resolución offline activa; la compilación, plugins restantes y árbol transitivo siguen sin validar.

## 2026-11-17 — Verificación y cierre de PROMPT 06 (CMS-001..013)

- Se ejecutó la verificación completa con Java 21, Maven offline y el perfil `local-h2`: BUILD SUCCESS, 166 tests Surefire y 14 Failsafe sin fallos ni errores; JaCoCo aprobó todas las comprobaciones configuradas.
- La suite incluye los tests TC-CMS para persistencia y unicidad de slug, permisos/publicación/auditoría, sanitizer XSS, locale/fallback/redirección, preview, bloques, restauración, medios CLEAN/cuarentena, equipo, vigencia legal, traducciones pendientes, seed bilingüe e idempotente y copy de condiciones/fallback.
- PROMPT 06 queda **COMPLETADO** en el alcance de desarrollo aprobado. La migración SQL Server no se ejecutó ni se declara compatible. Aviso legal y copy comercial permanecen DRAFT/PENDING hasta aprobación de Legal/Comercial; perfiles/materiales de equipo siguen sin publicación hasta contar con consentimiento/licencia. El consumidor `request` de `content.api` se difiere a F3 por decisión del owner; el fallback neutral no activa pagos.
- Un primer intento completo devolvió errores de carga de clases de test; las siete suites implicadas pasaron en ejecución focal y el `verify` completo repetido terminó correctamente. La evidencia de cierre es la ejecución final exitosa, no el intento fallido.
- La reejecución focal de `DocumentationArchitectureTest` y `GovernanceArtifactTest` tras esta actualización documental no llegó a ejecutar tests; no cambia el resultado del `verify` completo anterior.

## 2026-11-17 — Verificación y cierre de PROMPT 05

- Con JDK 21, Maven offline y `local-h2`, `.\mvnw.cmd -o -gs .mvn\prompt05-empty-global-settings.xml -s .mvn\prompt05-offline-settings.xml -Dmaven.repo.local=C:\Users\x455035\.m2 -Plocal-h2 verify` terminó en `BUILD SUCCESS`: 166 tests Surefire, 14 tests Failsafe y JaCoCo aprobados. Enforcer/JDK 21 también pasó.
- Pasaron los tests focales de capas y CMS (23 tests) y las IT focales de audit, metadata y variantes (4 tests). Las correcciones de precisión temporal y perfil `test` para H2 quedaron verificadas en la suite.
- PROMPT 05 queda cerrado: las tareas incluidas, sus criterios funcionales/de seguridad, compilación Java 21 y tests requeridos están cubiertos por la evidencia local. No se descargó nada ni se alteró la caché. SQL Server, Docker y entornos externos no se validaron.
- Se intentó arrancar el JAR mediante Java Debugger, pero la sesión terminó y `http://localhost:8090/es` no respondió. No se obtuvo diagnóstico del cierre y no se repitió el lanzamiento; la aplicación no está activa. El arranque local queda como limitación operativa separada del criterio acordado de cierre.
- Los settings Maven temporales usados solo para esa ejecución offline se retiraron al finalizar.

## 2026-11-18 — Evaluación y bloqueo de PROMPT 11

- Se identificó el alcance exacto: STF-001..STF-002, USR-001..USR-004 y E2E-001 de `docs/plan/tareas/F2-staff-gestion-usuarios.md`. Se contrastaron sus dependencias con el roadmap, el inventario, el diario y el código presente; no se consultó Git.
- No se crearon paquetes, rutas, tablas ni tests fuera de orden. Los paquetes owners `staff`, `customer` y `legal` y sus migraciones todavía no existen. En `identity` hay servicios de aplicación para tokens/invitaciones, recuperación y MFA, pero no se encontraron los TC-IAM-003/008/009/010 ni migraciones de tokens/MFA; esos IDs siguen sin poder certificarse. `notification` tiene outbox, SMTP/worker y migración, pero no tests de ese módulo ni evidencia de cierre NTF-001..003. También falta CUS-001 y LGL-002 para las ramas correspondientes. La ficha condiciona el inicio a que se satisfagan dependencias.
- Estado individual: STF-001 bloqueada por IAM-010; STF-002 por STF-001; USR-001 por CUS-001/STF-001; USR-002 por USR-001; USR-003 por USR-002/IAM-008/IAM-009/IAM-010; USR-004 por USR-002/LGL-002; E2E-001 por STF-001/IAM-009. Ninguno se declara completado. IAM-012, AUD-002, UIK-008/UIK-009 y CMS-006 cuentan con evidencia previa; hay una nota histórica de estado CMS en INVENTARIO que se corrigió para evitar contradicción con el cierre posterior.
- No se ejecutaron pruebas de Prompt 11 porque no hay un ID habilitado por el DAG. El siguiente paso requiere decidir si se amplía el alcance para completar primero las dependencias faltantes o si se espera a que se integren por sus owners.

## 2026-11-18 — Implementación parcial de PROMPT 07 (ERR-004)

- Se verificó que ERR-001..003, IAM-012 y los componentes base UIK-008 están presentes antes de empezar. El owner sigue siendo `config.errortracking`/`config.web`; no se añaden tablas ni migraciones.
- Se añadió `/gestion/errores` con filtros por fuente/severidad/estado, paginación, detalle resumido saneado, navegación ADMIN, formularios CSRF y acciones RESOLVED/IGNORED. La consulta requiere rol ADMIN y `ERROR_INVENTORY_MANAGE`; se rechaza también EMPLOYEE aunque tenga esa autoridad.
- Los cambios de estado y notas breves redaccionan patrones de emails, secretos y números/identificadores antes de persistirlos en el JSON privado; una nueva ocurrencia reabre el registro. Las resoluciones generan un evento mínimo mediante `shared.spi.AuditEventRecorder`, implementado por el owner `audit`. No hay borrado masivo.
- La vista muestra disponibilidad y ocupación actual frente al cap configurado; si el inventario no se puede cargar o persistir, presenta estado degradado sin filas ni ruta interna. El servicio sigue preservando el error original en la captura backend.
- Se añadieron tests focales de servicio, sanitización, autorización/rol+permiso, CSRF, filtros y estado degradado. No se ejecutaron compilación ni tests de PROMPT 07; ERR-004 queda parcial y abierto hasta obtener esa evidencia. No se verificaron SQL Server, despliegue ni retención productiva.
- Validaciones independientes: `npm run build` pasó, `npm run test:privacy` pasó 3/3 y la paridad ES/EN del bundle `config` pasó con 71 claves. La búsqueda de imports Java en `config` no detectó dependencias directas de módulos de negocio.

## 2026-11-18 — Continuación parcial de PROMPT 11 (STF / USR)

- Se continuó sobre el trabajo presente del espacio compartido y respetando el DAG `customer → staff` y `staff → identity`. Identity publica `IdentityAccountChanged` al crear el ADMIN inicial; el listener de staff sincroniza el perfil en la misma transacción. Se corrigieron un método que había quedado anidado por error en `StaffProfileService` y el nombre del constructor compacto de `IdentityAccountChanged`.
- Se añadieron pruebas unitarias para el evento de bootstrap, la creación del perfil del ADMIN y los guardarraíles que impiden auto-desactivar o desactivar al último ADMIN. Estas pruebas existen, pero no se ejecutaron.
- Se detectó una colisión global Customer/Identity V202611180002 y se renumeró el par de migraciones Customer a V202611180007/V202611180008, preservando el orden crear-cuenta → añadir-identificador-fiscal. Legal usa V202611180006 para evitar colisión con Notification V202611180001; Staff usa V202611180004. `MigrationConventionTest` ahora rechaza versiones duplicadas entre carpetas owner. No se ejecutó Flyway ni se afirma compatibilidad SQL Server.
- Maven offline no pudo resolver BOMs no cacheados. El intento online terminó antes de construir por `certificate_unknown`/PKIX al acceder a Nexus corporativo; no se alteraron settings ni se deshabilitó la validación TLS. No hay evidencia de compilación ni tests Java de esta continuación.
- Avance observable: quedan perfiles/invitación y listados básicos, detalle de usuario sin paneles owner, CSV acotado de clientes y edición/estado de trabajador. Acciones generales USR-003, exportación LGL-002/USR-004 con todos los contributors y E2E-001 siguen sin implementar o validar. Se mantiene la decisión del usuario de no ejecutar anonimización hasta contar con política de retención aprobada.
- Decisión aclarada: no se ofrecerá exportación personal parcial; queda pendiente hasta que todos los owners hayan implementado `PersonalDataContributor`. La página `/cuenta/datos` seguirá siendo informativa mientras tanto.

## 2026-11-18 — Continuación de USR-003

- Se corrigieron errores de compilación visibles en `IdentityUserDirectoryService` y `AdminUserActions`; también se eliminó un import duplicado en `JpaUserAccountStore`.
- Se conectó la ficha de usuario con acciones POST confirmadas para activar/desactivar, desbloquear, solicitar reset de contraseña y cambiar el rol entre EMPLOYEE/ADMIN. El servicio exige ADMIN y permisos explícitos, no permite auto-desactivación/auto-degradación ni quitar al último ADMIN, y rechaza transiciones de rol que involucren CLIENT porque no hay workflow owner para sincronizar la cuenta cliente.
- Los cambios de estado, desbloqueos y roles se auditan y encolan una notificación genérica localizada sin PII; el reset usa el flujo de recuperación existente. Las modificaciones del último ADMIN ahora bloquean las filas activas de ADMIN con `PESSIMISTIC_WRITE` para serializar intentos concurrentes.
- Se añadieron tests unitarios de acciones, correo y matriz de autorización, sin ejecutar; Maven sigue bloqueado antes de compilación por la confianza TLS del Nexus corporativo. No se validó SQL Server. USR-003 queda parcial por transiciones de cliente y reenvío de invitación aún no integrados, ausencia de validación y falta de E2E.

## 2026-11-18 — Ubicación de dependencias Maven

- El usuario indicó que las dependencias locales proporcionadas para este workspace están en `C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias`.
- Se registró la ruta en README, inventario, plan de calidad, gobernanza modular, ficha ERR-004 y PROMPT 07/instrucciones de build. Los comandos offline seleccionan esa ubicación mediante `-Dmaven.repo.local`; no se descargaron dependencias.
- Se intentó `.\mvnw.cmd -o -B "-Dmaven.repo.local=C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias" -Plocal-h2 compile`. Maven falló antes de compilar: no pudo resolver `org.springframework.boot:spring-boot-starter-parent:4.1.2` en modo offline. No se inspeccionó ni descargó dependencias; compilación y tests Java siguen sin evidencia y ERR-004 permanece abierto.

## 2026-11-18 — Corrección de errores de compilación y clasificación del inventario

- Owners y tareas afectadas: `identity` (IAM-009/USR), `legal` (LGL-001), `notification` (NTF-001), `config` (ERR-002/ERR-004) y anti-bot transversal. Se preservaron `web → application → domain`, el API publicado `legal.api.ConsentSource`, el SPI `shared.spi.ErrorReporter` y los límites existentes de sesión MFA, actor de consentimiento y cifrado de payload.
- Se corrigieron imports para Spring Security web, Jackson 3 y Spring Boot 4 Health; se añadió el getter de `createdAt` que ya tenía persistido `UserAccountEntity`; se ajustó el catch de Jackson 3 y se normalizaron filtros del visor a variables efectivamente finales.
- El resolver MVC se ordenó después de los resolvers de Spring y no registra `ErrorResponse` 4xx. Los tests guardan errores en `target/test-error-inventory.json`, no en `logs-error/error-inventory.json`. El inventario existente se conservó sin cambiar estados.
- Maven offline con JDK 21 y la caché documentada pasó `compile` (342 fuentes) y `test-compile` (119 fuentes). No se ejecutaron tests: Surefire no pudo resolverse porque falta `org.codehaus.plexus:plexus-utils:1.1` en esa caché; el repositorio offline alternativo también carece de BOMs requeridos. SQL Server y runtime no se validaron.
- El inventario recibido contiene respuestas 400/403/404 y violaciones H2 23505 generadas en contexto de test, no evidencia suficiente de fallos productivos. Queda abierta la entrada `ServletException`/Thymeleaf: su traza saneada termina truncada antes de identificar la plantilla/expresión, y no pudo reproducirse sin ejecutar tests.
- Se localizó por existencia, sin abrir el archivo, `C:\Users\x455035\.m2\settings-s-portal.xml` y se repitió el mismo goal en offline usando ese settings y el repositorio de dependencias indicado. Maven devolvió el mismo error de POM padre no resuelto; no hubo compilación ni tests.

## 2026-11-18 — Continuación de PROMPT 07 (ERR-004)

- Se contrastaron la ficha ERR-004, el DAG conservado, arquitectura, seguridad, UX/UI, calidad, ADR-012 y el contrato UI/backend. El owner continúa en `config.errortracking`/`config.web`; no se añadieron tablas, migraciones ni dependencias de módulos.
- Se amplió `ErrorInventoryUnavailableIT` para cubrir también una mutación POST cuando el JSON de inventario no está disponible: el endpoint redirige al visor degradado sin revelar la ruta privada. Los tests focales existentes cubren rol ADMIN más permiso, denegación de CLIENT/EMPLOYEE, CSRF, filtros, resolución, IDs no existentes, redacción y marcado accesible del contenido.
- El gate pedido por ERR-004 se intentó con Maven offline, `-Plocal-h2`, el repositorio local proporcionado y los tests de controlador/servicio/IT. Maven volvió a detenerse antes de leer el proyecto porque `spring-boot-starter-parent:4.1.2` no está disponible localmente; no hubo compilación ni tests Java. No se inspeccionó la caché ni se descargaron dependencias.
- Validaciones independientes: `npm run build` pasó y `npm run test:privacy` pasó 3/3. No son evidencia de compilación Java, accesibilidad renderizada ni cierre ERR-004. La tarea sigue parcial/abierta; SQL Server, despliegue, retención y límites operativos continúan fuera de esta validación.

## 2026-11-18 — Reemplazo de Plexus Utils en Surefire

- Se identificó que el classpath runtime del plugin `maven-surefire-plugin:3.5.6` intentaba resolver `org.codehaus.plexus:plexus-utils:1.1`. Se fijó `3.5.1` como dependencia del plugin, sin añadirla al classpath de la aplicación.
- La traza Maven confirma que el classpath de Surefire selecciona `plexus-utils:3.5.1` y ya no solicita `1.1`. `mvn -o -B -Plocal-h2 -DskipTests test-compile` terminó en `BUILD SUCCESS`.
- El test focal no llegó a ejecutarse: el repositorio offline carece de `org.apache.maven.surefire:surefire-junit-platform:3.5.6`. No se modificó ni se completó la caché; SQL Server, tests Java y runtime siguen sin validar.

## 2026-11-18 — Ordenamiento del directorio PROMPT 11

- USR-001 ahora admite orden allowlist por fecha de creación, nombre o correo, en ambas direcciones. El orden se propaga desde los filtros server-side a la API owner de identity, la paginación y la exportación CSV; el ID ascendente desempata resultados para mantener páginas estables.
- Se añadió en el owner `audit` un fragmento HTMX de historial de usuario, con `AUDIT_VIEW` independiente, respuesta Thymeleaf localizada y enlace de respaldo sin JavaScript. No se conectaron request/messaging/SimbioSphere porque no publican aún rutas/API en el workspace. Se añadieron pruebas negativas/positivas de autorización y de render del fragmento.
- Se añadieron claves ES/EN, una prueba de aplicación que verifica el orden tipado y el desempate, y se actualizaron contrato UI, rutas, README e inventario. No se alteraron flujos legales.
- `mvn -o -B "-Dmaven.repo.local=C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias" -Plocal-h2 clean test-compile` pasó: 343 fuentes de producción y 119 de test compiladas con Java 21.
- La ejecución focal de `IdentityUserDirectoryServiceTest` falló antes de iniciar tests porque falta `org.junit.platform:junit-platform-commons:1.12.2` en el repositorio Maven offline. No se descargó ni se cambió la configuración del repositorio. SQL Server y E2E siguen sin validar.
