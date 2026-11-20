# Diario de desarrollo

## 2026-11-20 — Resolución del ciclo Modulith antes de Oleada 7

- Se eliminó la dependencia inversa `notification → request`. `request.application.RequestNotificationListener` consume sus eventos propios después del commit y publica el contrato owner `notification.api.RequestNotificationRequested`, que contiene solo UUID técnico, ruta interna sin token, plantilla, locale fallback y clave idempotente.
- `notification` conserva outbox, locale efectivo, preferencias y entrega; deja de consultar `request.api` y no importa entidades/repositorios. Se retiraron `RequestNotificationApi` y `RequestNotificationTarget` del módulo request. El flujo RequestCreated/StatusChanged/QuoteSent conserva sus claves deterministas y pruebas negativas.
- Evidencia focal final: `ModularityTests`, `ModuleDagTest`, `LayerRulesTest` y 15/15 tests de request/notification pasan; total selección 19/19. ADR-013 documenta ownership y contrato. Se actualizó el diagrama textual de arquitectura; no cambian permisos, tablas, PII ni comportamiento funcional.

## 2026-11-20 — Cierre coordinador de Oleada 6

- Se corrigió y revalidó `LayerRulesTest`: 2/2 pasan; los adaptadores `simbiosphere.infrastructure` implementan puertos de aplicación y `DemoHttpClient` declara constructor primario Spring. El fallo que queda en `ModularityTests` es distinto: ciclo `customer→identity→notification→request→customer`, que requiere reconciliar ownership/event wiring en una tarea separada antes del gate transversal completo.
- PROMPT 15 quedó revalidado sin DEMO real. SIM-001/003/011/012 y el cliente/contrato loopback pasan; se añadió la vista owner-only `/cuenta/simbiosphere` sin token ni secreto. Selección final: 35/35 Surefire y 2/2 H2, con LayerRules, DAG, i18n, templates, OpenAPI y contrato HTTP local en verde. SIM-004 WireMock se pospone por dependencia/ejecutor no disponible; SIM-005/006/007/009/010/014 permanecen bloqueados por origen de intención, consentimiento/campos y worker/aprovisionamiento aprobados. SIM-013 requiere entorno externo autorizado.
- SIM-002 se reconcilió: aceptado para desarrollo local por autorización owner común 2026-11-20 (ADR-002/DIARIO), pero no publicado ni autorizado para PRE/producción/tráfico real. El texto interno histórico del YAML conserva la copia byte-identical; la decisión normativa posterior prevalece.
- E2E-002/E2E-003 y `E2E-004` están consolidados y descubiertos por Playwright (4 specs de oleada 6 en 3 ficheros). La ejecución sigue `POSTPONED`: faltan servidor, Chromium, Mailpit y fixtures; E2E-004 además requiere provider/API autorizado para el recorrido completo. No se ejecutó red real ni se capturaron tokens/PII.

## 2026-11-20 — Cierre local de pendientes PROMPT 12

- MSG-002 se completa localmente con vista cliente/empleado, filtrado PUBLIC/INTERNAL desde consulta a plantilla, eventos mínimos y consumidores de mensajería. No se crean rutas paralelas ni se exponen notas internas.
- REQ-010 consume `RequestCreated`, `RequestStatusChanged` y `QuoteSent` después de commit mediante APIs publicadas. Resuelve owner/locale con SPI, usa enlaces internos sin token y claves deterministas `enqueueOnce` para impedir duplicar el outbox al reentregar un evento. `QUOTE_SENT` deja de encolarse directamente desde `request`, eliminando dependencia inversa y duplicación.
- REQ-011 amplía la matriz IDOR: detalle, adjuntos, vistas cliente/empleado, roles y mensajes INTERNAL. Cliente ajeno recibe 404/403 y no consulta el archivo; empleado autorizado sí ve la nota. Evidencia final: 53 tests focales Surefire y 4 H2 Failsafe, además de DAG, i18n y templates, sin fallos.
- Pendientes externos: Playwright/axe con servidor/Chromium/fixture y Mailpit/SMTP real. El gate global LayerRules continúa fallando por tres clases `simbiosphere.infrastructure` de F5 y se registra como bloqueo ajeno a PROMPT 12, sin modificar ese alcance. No se afirma SQL Server ni WCAG real.

## 2026-11-20 — Ejecución de PROMPT 14: contacto, bandeja y notificaciones

- MSG-003..005 implementan contacto server-side, consentimiento configurable bloqueado por defecto hasta aprobación, ALTCHA/honeypot/rate limit, PII cifrada en tablas `messaging`, acuse por outbox, adjuntos limpios mediante `files.api`, descarga autorizada y audio MediaRecorder de hasta 120 s con previsualización y fallback. No se inventaron texto legal, canales oficiales ni proveedor SMTP.
- MSG-007..012 añaden operaciones de lectura/asignación/etiquetas/archivo/papelera/restauración/purga manual y job diario limitado por lote, siempre desactivado con retención no aprobada. MSG-008 completa localmente respuesta PUBLIC/INTERNAL con adjuntos limpios, plantillas ES/EN, eventos y descarga autorizada. El enlace mágico usa 32 bytes/SHA-256/TTL/un uso/alcance CONTACT y también autoriza adjuntos; polling HTMX usa intervalo/backoff configurables y pausa fuera de vista. MSG-012 mantiene IA desactivada.
- MSG-013 queda PARCIAL/BLOQUEADO: la conversión de CONTACT anónimo necesita una cuenta cliente owner y campos mínimos de conversión que el contrato no fija. E2E-003 se preparó en `e2e/contact-reply.spec.ts`, sin ejecutarlo: requiere servidor, fixture `INBOX_MANAGE`, consentimiento aprobado, Mailpit y Chromium. El token no se guarda en capturas/traces y el replay debe devolver 404.
- NTF-004 queda PARCIAL por su dependencia de `simbiosphere`: ya consume `RequestStatusChanged`, `MessagePosted` y el acuse `MessageReceived`, resuelve destinatario/locale por APIs/SPI, conserva enlaces internos sin token y usa ES/EN/texto plano. NTF-005 está implementado localmente con preferencias owner-only, CSRF, tabla `notification` y supresión antes del outbox. Falta Mailpit real y el evento de aprovisionamiento de PROMPT 15.
- Evidencia focal: selección Prompt 14 Maven pasó sin fallos, incluidos arquitectura, DAG, plantillas, i18n, MVC y servicios; H2 focal de contacto/conversación/outbox pasó. `npm run build` y 3/3 tests del grabador pasaron. Playwright solo se listó. SQL Server, consentimientos, SMTP productivo, widget ALTCHA oficial y navegador real no se declaran validados.

## 2026-11-20 — Corrección focal PROMPT 13

- Se confirmó y cubrió con `RequestServiceTest` que cada solicitud creada abre inmediatamente su conversación REQUEST mediante `ConversationApi`, dentro del límite de `request`; no se modificó ningún interno de `messaging`. Esto permite que el detalle de empleado cargue el panel aun sin mensajes previos del cliente.
- `E2E-002` corrige el control de acceso sin sesión: con cliente HTTP sin seguimiento de redirecciones exige `302` y cabecera `Location` hacia `/acceso`.
- Revalidación Maven focal con el repositorio offline proporcionado: selector de Prompt 13 más `RequestServiceTest`, **31/31 pruebas sin fallos ni errores**. El cambio concurrente de `ConversationInboxState` quedó fuera del alcance.

## 2026-11-20 — Ejecución de PROMPT 13: presupuestos y portal de empleados

- Scope exacto: `QUO-001..004`, `EMP-001..007`, `E2E-002`. Owner `request`; consumo solo por APIs publicadas de `customer`, `staff`, `messaging`, `audit` y `notification.api`. No se modificaron internals de `messaging` ni otras tareas/prompts. Antes de editar se fijaron aceptación, permisos, ownership, CSRF, PII, migración y evidencia H2.
- Producción: agregados `quote`/`quote_version`, restricciones monetarias `DECIMAL(19,4)`, versiones append-only, estados, decisión owner con identificador fiscal presente, outbox de email, eventos mínimos post-commit y job UTC de caducidad. Operaciones de empleado: dashboard, filtros/paginación/CSV neutralizado y redactado, timeline, asignaciones activas 1..N, transición/prioridad/estimación con optimistic locking, mensajes PUBLIC/INTERNAL por contrato y carga con SLA configurable.
- UI server-side bajo `/gestion/**` y detalle cliente: Thymeleaf/HTMX, fragmentos, CSRF, ES/EN, layout responsive, nombres de controles, foco/teclado semántico y disclaimer de condiciones provisionales. Se añadió navegación backoffice.
- Migración owner: `V202611201800__request__quotes_and_employee_operations.sql`; añade índices operacionales, unicidad de responsable, quote/versiones, caducidad y campos request. H2 usa entidades creadas por el esquema de test; la migración SQL Server no se ejecutó ni certificó.
- Tests creados/impactados: `QuoteVersionEntityTest`, `QuoteServiceTest`, `QuoteExpiryJobTest`, `EmployeeRequestServiceTest`, `EmployeeRequestControllerTest`, `QuotePersistenceH2IT`, `QuoteMigrationContractTest` y extensión focal de `ClientRequestControllerTest`; E2E `request-assignment-quote.spec.ts`.
- Validación ejecutada: el wrapper con settings por defecto no pudo resolver el plugin (PKIX); con Java 21 + Maven 3.6.3 + `-Dmaven.repo.local=... -Plocal-h2` proporcionados, `compile` pasó. La selección focal final pasó 26/26: `QuoteVersionEntityTest`, `QuoteServiceTest`, `QuoteExpiryJobTest`, `EmployeeRequestServiceTest`, `EmployeeRequestControllerTest`, `ClientRequestControllerTest`, `QuotePersistenceH2IT`, `QuoteMigrationContractTest`, `ModularityTests`, `ModuleDagTest`, `I18nKeyParityTest`. `TemplateRulesTest` quedó bloqueado por un color literal preexistente de `notification/email/message_posted.html`; `LayerRulesTest` por `@ResponseStatus` preexistente en `messaging.application.ContactAttachmentNotFoundException`. Una recompilación posterior encontró un cambio concurrente no perteneciente a Prompt 13 en `messaging` (`InboxController`/`InboxFilters` incompatibles); no se modificaron esos internals.
- Pendientes no inventados: fiscalidad/IVA/moneda/validez comercial requieren aprobación; SLA de 1 día hábil solo se conserva como propuesta configurable y faltan calendario/festivos; aprobaciones externas siguen abiertas; la métrica de mensajes no leídos se marca pendiente porque no hay evidencia de lectura integrada. `E2E-002` queda `POSTPONED` por servidor/fixture Playwright y permanece reproducible con las variables `PORTAL_E2E_*` exigidas por el spec. IDs `QUO-001..004`, `EMP-001..007` y `E2E-002` son PARCIALES/NO CERRADOS hasta gates obligatorios.

## 2026-11-20 — Implementación local DEMO autorizada y SIM-003

- Tras la autorización owner explícita, DEMO-02a..08 se implementaron en `DEMO-orders-thymeleaf`: HMAC/scopes/anti-replay/rate limit, registro y rotación de claves, idempotencia pre-tenant, tres endpoints REST de plataforma, Problem Details, slug/defaults, referencia externa/términos, alta sin contraseña y enlace de activación de un uso/15 min. La selección focal local DEMO pasa **18/18**, incluida arquitectura **8/8**. No se ejecutaron SQL Server, PRE, provider externo ni tráfico real.
- En Portal, SIM-003 quedó implementado y validado focalmente: cliente Java 21 tipado, HMAC y vector canónico, idempotencia, 3 s/10 s, circuit breaker, mapping RFC 7807 sin body/secretos y endpoint local de contrato. La selección `simbiosphere` pasa **35/35** con H2, migración, capas, DAG, plantillas, i18n y OpenAPI.
- La suite global `ModularityTests` permanece bloqueada por un ciclo concurrente ajeno al módulo nuevo: `customer→identity→notification→request→customer`, derivado del nuevo contrato de notificación de request. No se confunde ese fallo con SIM-003. Debe resolverse antes de declarar el gate transversal cerrado. SIM-004 sigue parcial: hay fixtures y servidor local de contrato, pero no dependencia ni ejecución WireMock.

## 2026-11-20 — Autorización owner y desbloqueo local SIM-002/DEMO

- El owner común de S-portal y DEMO autorizó expresamente el 2026-11-20 la modificación completa de `DEMO-orders-thymeleaf` y la implementación local del contrato M2M v1. La decisión queda registrada en ADR-002 y ADR-013, y eleva SIM-002/DEMO-01 a **Accepted para desarrollo local**.
- El alcance autorizado comprende código, configuración y pruebas sintéticas H2/contrato de proveedor y portal. No comprende despliegue, PRE/producción, tráfico externo ni validación con datos/secretos reales. El OpenAPI y el SHA-256 sincronizado permanecen como fuente contractual compartida.
- Con este registro queda desbloqueado SIM-003 y puede avanzarse SIM-004 y el núcleo local posterior; las validaciones DEMO real, entorno compartido y E2E siguen como gates externos separados.

## 2026-11-20 — PROMPT 15: bloqueo previo por DAG y contrato

- Se leyó y comprobó el alcance exacto de PROMPT 15: `SIM-001`, `SIM-003..SIM-014` y `E2E-004`, con owner principal `simbiosphere`, tablas exclusivas del módulo y consumo de `customer.api`/contratos publicados. Se revisaron arquitectura, seguridad/RGPD, modelo de datos, calidad/testing, operación, ADR-002 y la ficha F5 antes de editar.
- La precondición no negociable no está satisfecha. `SIM-002` sigue marcado como parcial/bloqueado en el inventario y su OpenAPI `docs/plan/contracts/simbiosphere-platform-v1.yaml` declara que es una propuesta no publicada. Se inspeccionó además, en solo lectura, `C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\DEMO-orders-thymeleaf`: no hay referencias a DEMO-01/SIM-002, contrato `/api/platform/v1/**` ni endpoint `owner-onboardings`; `docs/integraciones/api-spec.yaml` solo documenta `/platform/tenants`, y el código mantiene `OwnerOnboardingOrchestratorApi` como API Java interna con controlador MVC en `/onboarding`. Por tanto no existe evidencia observable de aprobación/acuerdo por los owners Portal y DEMO. El prompt prohíbe afirmar que DEMO publicó la API cuando solo hay una propuesta.
- No se creó ni modificó código de producción, configuración, migración, plantilla, dependencia, test o E2E de `simbiosphere`; tampoco se modificó DEMO ni se realizó red externa. Esto evita implementar SIM-003..014 contra un contrato aún no autorizado y evita convertir SIM-001/SIM-004 o las fixtures locales en falsa evidencia de cierre.
- En una fase documental autorizada posterior se preparó DEMO-01 en el workspace DEMO: ADR-013 en estado `Proposed`, copia byte a byte del OpenAPI v1 en `docs/integraciones/simbiosphere-platform-v1.yaml` y test estructural `SimbioSpherePlatformContractTest`. Ambas copias tienen SHA-256 `405887EBB18B90655B71A4BDE20D88A1C4A44FBEA96975D76DD1D6A9D7410B2F`. La selección DEMO pasó 9/9 (`ModuleArchitectureTest` 8/8 + contrato 1/1); el test focal de Portal pasó 2/2. No se añadió endpoint, filtro HMAC, key registry, idempotencia, tabla, flujo REST ni llamada externa.
- Se revalidó el DAG y se avanzó PROMPT 15 hasta el límite local habilitado. SIM-001 crea el módulo owner `simbiosphere` y sus tablas exclusivas `provisioning`, `account_link` y `linked_business`: inicio PENDING, idempotencia estable, un vínculo externo único y 1..N negocios por cuenta, sin FK ni entidad importada de `customer`/`identity` y sin vincular por email. Publica DTO/evento mínimo y servicio idempotente por cuenta owner.
- SIM-011 implementa alternativa temporal segura: feature flag `PORTAL_SIMBIOSPHERE_FALLBACK_ENABLED` desactivado, allowlist local/PRE, HTTPS obligatorio salvo loopback, rechazo de credenciales/query/fragment, CTA ES/EN y GET `/continuar/simbiosphere` que responde 404 cuando está deshabilitado. No copia ni serializa datos del formulario y no simula vínculo o alta.
- SIM-005 avanza de forma parcial y segura: un `EmailVerified` solo transita una intención PENDING existente a READY, publica `ProvisioningStateChanged` tras commit y es no-op para usuarios sin cuenta/intención; no existe llamada HTTP. No se crea intención automáticamente desde cualquier `ClientRegistered`, porque faltan los campos/consentimiento conjunto de SIM-009 y no debe darse de alta DEMO por coincidencia o registro ordinario. EPR persistente, métricas y UI siguen pendientes.
- SIM-012 queda diseñado para R2 mediante `AccountLinkPort`: challenge, TTL e intentos máximos para prueba de posesión; no existe adaptador, endpoint ni vínculo automático por email. Se actualizaron arquitectura, modelo ER, saga, ADR-002 y ficha F5.
- Evidencia focal final: selección Java/H2 de Prompt 15 pasa **30/30**, 0 fallos/errores: dominio/aplicación, H2 con unicidad externa, revisión de migración, DAG/Modulith/capas, plantillas, i18n, fallback/open redirect y OpenAPI. JaCoCo de una ejecución `verify` focal no se usa como gate de cierre porque el check global requiere cobertura de todos los módulos y reportó paquetes no seleccionados; los tests y gates aplicables sí pasan. SQL Server no se ejecutó ni certifica.
- Estado: SIM-001, SIM-011 y SIM-012 están implementados localmente en su alcance; SIM-002 continúa preparado pero `Proposed`. La única precondición inmediata para reanudar SIM-003 es la aprobación trazable de owners Portal/DEMO. SIM-005+ además requieren dependencias locales/DEMO indicadas en la ficha; SIM-013/E2E-004 siguen externos. No se declaran WireMock servidor, provider test, HMAC, cliente HTTP, llamadas DEMO, E2E o SQL Server como aprobados.

## 2026-11-20 — Ejecución de PROMPT 12: request y recorrido cliente

- Se añadió el módulo owner `request` y se verificaron antes de editar sus dependencias, contratos y límites: `customer.api`, `files.api`, `messaging.api`, `audit.api` y `shared`; no importa repositorios ni entidades externas. La migración ownereada crea categorías, contador anual, solicitud, historial, adjuntos y asignaciones; todas las referencias customer/identity/staff son UUID sin FK cruzadas.
- REQ-001..004 implementan categorías activas, `SOL-AAAA-NNNNNN`, máquina completa para desarrollo a medida y sugerencia de módulo, historial/auditoría y eventos mínimos. REQ-005..009 añaden formularios server-side, borrador de sesión autenticada, validación, listado con filtros/paginación server-side, detalle/historial PUBLIC, mensajes, adjuntos limpios y cancelación owner. La ruta nunca revela notas INTERNAL: además del filtro de `messaging`, el controlador cliente aplica una defensa de visibilidad antes de renderizar.
- Se conservaron los bloqueos del DAG: REQ-010 queda parcial porque la emisión de presupuesto/empleado depende de `EMP-005`, `QUO-002` e `IAM-013`; REQ-011 queda parcial por `EMP-006` y no se inventaron rutas de empleado. Los eventos `RequestCreated`/`RequestStatusChanged` se publican tras commit, solo con IDs/estado, sin PII.
- Seguridad y arquitectura: CLIENT debe resolver a cuenta owner en cada caso de uso, IDOR devuelve 404 sin enumeración, mutaciones conservan CSRF, adjuntos pasan por `FileStorage`, notas INTERNAL no llegan al DTO/vista cliente y cambios de estado exigen rol/permiso. Se añadieron navegación ES/EN, respuestas accesibles/escapadas y advertencia de que presupuesto/fecha son no vinculantes.
- Evidencia focal final con JDK 21: `mvn compile` pasa y la selección de PROMPT 12 pasa 34/34 tests (`Request*`, MVC, H2, migración estática, Modulith, DAG, capas, plantillas e i18n), 0 fallos/errores. No se ejecutó la suite completa. SQL Server no se ejecutó y no se certifica. La prueba Playwright/axe de `/cuenta/solicitudes/**` queda `POSTPONED`: requiere servidor local en 8090, fixture H2 de dos clientes/empleado y credenciales sintéticas; ejecutable pendiente de validación del navegador. Reproducir arrancando `spring-boot:run` con `local-h2` y el fixture autorizado antes de ejecutar el recorrido axe/Playwright.
- Continuación no bloqueada por herramientas: se contrastó la máquina implementada con `07-seguridad-rgpd.md` y se corrigieron los códigos estables a `PENDIENTE_INFO_CLIENTE`, `PRESUPUESTO_ENVIADO`, `EN_PAUSA`, `EN_DESARROLLO`, `EN_REVISION_CLIENTE`, `ENTREGADA` y `PUBLICADA`. Se guardan estado previo de pausa, motivo de descarte y referencia duplicada. También se integró atómicamente «aportar información» con el API de `messaging`: guarda mensaje PUBLIC y devuelve `PENDIENTE_INFO_CLIENTE→EN_ANALISIS`, con test positivo. La cancelación de cliente se limita a RECIBIDA/EN_ANALISIS; EN_PAUSA→CANCELADA queda exclusivamente para flujo administrativo futuro.
- Estados: MSG-001 completada localmente; MSG-002, REQ-001..009 implementados localmente con cierres externos pendientes; REQ-010 y REQ-011 parciales/bloqueados por PROMPT 13. README, INVENTARIO, modelo de datos y ficha F3 se actualizaron con evidencia observable.

## 2026-11-20 — Oleada 5: cierre local de MSG-001

- Se auditó MSG-001 contra su ficha: owner `messaging`, dependencias FND-006/FIL-001 observables, tablas propias, referencias a otros módulos solo por UUID, API `ConversationApi`, invariantes de contexto/autor y orden cronológico UTC. No se añadió código de producción ni ruta nueva.
- Se añadió `MessagingMigrationContractTest` para cubrir el criterio pendiente de revisión estática: índices por propietario/estado, conversación/mensaje/visibilidad, restricciones de contexto/estado/visibilidad/cuerpo y ausencia de FK a request/customer/identity. La selección focal pasó 25/25 Surefire y 5/5 Failsafe H2, sin fallos ni errores.
- MSG-001 queda **COMPLETADA en alcance de desarrollo local**. No se ejecutó SQL Server porque no es requisito de este cierre; la compatibilidad productiva permanece sin certificar.
- MSG-002 no se cierra: su revalidación pasó 36/36 Surefire y 5/5 H2, pero falta la superficie cliente ligada a solicitudes, la aceptación del contrato local, el consumidor de notificaciones y la validación navegador. No se inventó una ruta cliente paralela porque el contrato sitúa ese recorrido bajo `request`. REQ-001..011 se omiten sin cambios porque REQ-001 depende de STF-001, todavía parcial; no existen tareas REQ independientes habilitadas.

## 2026-11-20 — Revalidación focal de PROMPT 11

- Se releyeron las fichas y dependencias de IAM-010, IAM-012, STF-001/002, USR-001..004 y E2E-001. IAM-010 conserva token hashado, un uso, TTL, revocación, consentimiento y publicación posterior al commit; se añadió una prueba unitaria que impide consumir una invitación revocada. IAM-012 amplió la evidencia MVC para rechazar EMPLOYEE/CLIENT en todas las rutas de lectura de PROMPT 11 incluso si presentan authorities de gestión.
- STF-002 vinculó los errores localizados de puesto, estado y especialidades a sus controles mediante `aria-invalid`/`aria-describedby` y IDs únicos. `StaffManagementControllerTest` pasó 11/11 con una aserción MVC del HTML; no se afirma Axe ni WCAG de navegador. USR-001 añadió `UserDirectoryH2IT`, que persiste cuentas sintéticas y verifica filtros de rol/estado/locale, total y orden estable entre páginas por desempate de ID.
- La primera selección focal nueva pasó 29 Surefire (`AuthorizationMatrixTest` 7, `TemplateRulesTest` 6, `OneTimeTokenServiceTest` 5 y `StaffManagementControllerTest` 11) y 1 Failsafe `UserDirectoryH2IT`, sin fallos ni errores. La revalidación previa pasó `InvitationServiceTest` 8/8, `StaffProfileServiceTest` 10/10, `InvitationAcceptanceStaffH2IT` 3/3, `StaffInvitationListenerH2IT` 6/6 y `StaffPersistenceH2IT` 2/2. No se ejecutó la suite completa ni JaCoCo.
- USR-003 sigue cubierto por autorización, CSRF, confirmación, auditoría/notificación y guardarraíles del último ADMIN; no se modificaron las conversiones CLIENT↔staff, deliberadamente rechazadas. USR-004 permanece bloqueado por política de retención y contributors completos de datos personales. USR-002 no avanza paneles request/messaging/SimbioSphere sin aprobación owner.
- E2E-001 se descubrió con Playwright 1.62.1: 18 pruebas totales, incluida la de invitación/CMS. La ejecución queda `POSTPONED`: no hay servidor en localhost:8090, Mailpit, ejecutable Chromium esperado ni credenciales sintéticas ADMIN/MFA. No existe fixture de navegador ni reloj/semilla segura ya implementados para fabricar y abrir una invitación vencida sin esperar ni exponer un endpoint; el test H2 de expiración no reemplaza esa evidencia. No se instalaron herramientas ni se lanzó un servicio externo.

## 2026-11-20 — Aceptación real de invitación integrada con STF-001

- Se añadió `InvitationAcceptanceStaffH2IT` en el módulo de tests de `identity`. Comprueba que emitir una invitación pendiente no provisiona una cuenta; luego acepta el token mediante `InvitationService.accept`, comprueba la cuenta activa y consulta el perfil a través de `StaffDirectory`, sin acceder a repositorios/entidades de `staff`.
- La prueba confirma especialidades vacías y que reutilizar el token no crea otra cuenta ni altera el perfil. Un segundo escenario marca expirado el token H2 persistido y confirma que la aceptación falla sin crear cuenta ni registrar consentimiento. Se sustituyó únicamente `ConsentRecorder`: no se fabrican documentos legales aprobados; la integración real de las versiones legales continúa pendiente.
- Una selección focal combinada pasó 22 Surefire (8 `InvitationServiceTest`, 4 `OneTimeTokenServiceTest`, 10 `StaffProfileServiceTest`) y 13 Failsafe (1 `InvitationAcceptanceStaffH2IT`, 4 `OneTimeTokenH2IT`, 6 `StaffInvitationListenerH2IT`, 2 `StaffPersistenceH2IT`), sin fallos ni errores. Tras añadir el caso de expiración, la última selección pasó 22 Surefire y 2 Failsafe (`InvitationAcceptanceStaffH2IT` 2/2). No se ejecutó la suite completa ni JaCoCo en estas selecciones.
- Se añadió un tercer escenario: cuando falla la escritura simulada del consentimiento, se revierten cuenta y consumo del token, no aparece perfil huérfano y el mismo token puede aceptarse al reintentar. La última selección pasó `InvitationAcceptanceStaffH2IT` 3/3 y 22 tests Surefire relacionados, sin fallos ni errores.
- `ModuleDagTest`, `ModularityTests` y `LayerRulesTest` pasaron 4/4; `DocumentationArchitectureTest`, `GovernanceArtifactTest` y `AdrDocumentRulesTest` pasaron 11/11 con la documentación actualizada.
- STF-001 avanza, pero sigue parcial: la comprobación de expiración está verificada en H2, pero faltan E2E-001 con Playwright/Mailpit y sus credenciales locales, dependencias/validaciones pendientes y la aprobación legal correspondiente. No se validó SQL Server.

## 2026-11-20 — LGL-002: protección de la página informativa de derechos

- Se añadió y reforzó `DataRightsControllerTest`: comprueba 403 para una visita anónima y para usuarios EMPLOYEE/ADMIN autenticados, y que un CLIENT autenticado solo ve la información, sin controles de solicitud, exportación o baja.
- No se añadió procesamiento de solicitudes, exportación parcial ni anonimización. LGL-002 sigue bloqueada hasta que todos los owners aporten sus contributors y exista un flujo aprobado.
- La primera selección documental y del test detectó que el acceso anónimo responde 403, no redirección; se alineó la aserción con la denegación real. Tras restringir explícitamente `/cuenta/datos` a `ROLE_CLIENT`, `DataRightsControllerTest` pasó 3/3; `DocumentationArchitectureTest`, `GovernanceArtifactTest` y `AdrDocumentRulesTest` pasaron 11/11 con `temurin-21.0.11`, Maven offline y `local-h2`. No se ejecutó la suite completa.
- La selección focal de PROMPT 10 con Java 21/Maven offline pasó 42 Surefire y 5 Failsafe H2, sin fallos, errores ni omisiones. Incluyó registro/consentimiento, cuenta, invitaciones, identificador fiscal y la página informativa de derechos; no se ejecutó SQL Server.
- Se alinearon los estados observados de LGL-001..003 y CUS-001..004 en la ficha F2. LGL-002/003 permanecen parciales/bloqueadas; CUS-002 mantiene pendiente la comprobación de navegador y la compatibilidad SQL Server no se afirma.

## 2026-11-20 — Intento E2E de accesibilidad del registro

- Se lanzó `PortalApplication` con `local-h2` mediante Java Debugger y se ejecutó solo `customer registration form is accessible` con Playwright. La navegación a `http://127.0.0.1:8090/registro` terminó con `ERR_CONNECTION_REFUSED`, por lo que axe no analizó la página.
- Con autorización posterior para relanzar, el Java Debugger no abrió sesión: su compilación m2e falló por incompatibilidad de `ClassLoader` entre Maven Resources Plugin 3.5.0 y el classloader OSGi de Eclipse (`loader constraint violation` en `BuildContext.newDeleteScanner`). Se comprobó que no quedó sesión activa. No se sustituyó el lanzamiento por terminal ni se descargaron herramientas, conforme a la guía Java.
- La prueba sigue `POSTPONED`: falta una aplicación local escuchando en una URL reproducible. No se declara PASS ni fallo de accesibilidad; instrucción pendiente: corregir el build integrado de Java o proporcionar un arranque local autorizado con JDK 21/offline, y ejecutar `customer registration form is accessible` con `PORTAL_E2E_BASE_URL`.
- Se eliminaron la captura, traza y contexto de error generados por el intento inicial.

## 2026-11-20 — Límites de cierre de PROMPT 10

- LGL-002 no puede cerrar dentro del alcance exclusivo de `legal`/`customer`: `PersonalDataContributor` tiene definición compartida pero cero implementaciones registradas. Una exportación útil exigiría contributors en los owners con PII, contratos y pruebas de cobertura; no se creó una exportación parcial.
- LGL-003 conserva únicamente el estado no destructivo. Aunque se autorizó conceptualmente una purga, no se proporcionaron categorías, plazos, condiciones de legal hold ni tratamiento de fallos; no se inventó una política y no se creó ni ejecutó un job destructivo.
- CUS-003 permanece dependiente de IAM-010/IAM-012. `InvitationIssuer` no publica expiración/estado por referencia para `customer`, por lo que el metadata customer pendiente no puede limpiarse con fuente autoritativa sin modificar el contrato owner de `identity`; no se modificó ese módulo.
- CUS-004 queda validado localmente para la única decisión de país vigente: ES. No se añadieron países ni consultas externas.

## 2026-11-20 — MSG-003 parcial: vista previa de contacto sin captura

- Se añadió la vista pública bilingüe de contacto en modo solo lectura. Los controles están desactivados; no hay formulario ni POST y no se captura ni persiste información.
- El consentimiento (texto y versión) y la política de retención continúan pendientes de aprobación legal; no se añadieron envío ni acuse.
- Pasaron 28/28 pruebas focales: 2 de contacto, 6 de assets, 3 de i18n, 6 de plantillas, 3 de arquitectura documental, 4 de gobernanza y 4 de ADR. `npm run build` pasó con el aviso existente de `caniuse-lite` desactualizado.
- No se repitió la suite completa Java/H2 para esta vista. La migración SQL Server, la aprobación legal y las pruebas de navegador autenticado siguen sin validarse.

## 2026-11-20 — Política de ejecución focal de tests

- Se actualizó la documentación operativa y de gobernanza para que cada desarrollo, modificación o fix ejecute solo los tests relacionados con las clases modificadas, añadidas o impactadas, junto con los gates obligatorios de ese alcance.
- La suite completa queda como último recurso si no existe una selección focal fiable o un criterio de aceptación explícito la exige; el motivo debe quedar registrado. La política no cambia los gates de CI/release ni permite omitir criterios de aceptación.
- Se alinearon README, norma P11, guía de calidad/testing, instrucciones Copilot y skill modular, guía de ejecución local, gobernanza y prompts de desarrollo; el inventario refleja la política.
- No se modificó código de aplicación, configuración ni contratos. `GovernanceArtifactTest`, `DocumentationArchitectureTest` y `AdrDocumentRulesTest` pasaron 11/11 con Maven offline, Java 21 y `local-h2`; no se ejecutó la suite completa.

## 2026-11-20 — Idempotencia de eventos STF-001 y verify completo

- Se añadieron dos pruebas a `StaffProfileServiceTest`: una confirma que una invitación repetida no sobrescribe un perfil previamente editado/inactivo; la otra verifica que repetir un evento de cambio de cuenta no vuelva a persistir un estado sin cambios y conserve el puesto.
- La prueba de integración `StaffInvitationListenerH2IT` detectó que el listener `AFTER_COMMIT` no dejaba persistido el perfil. `createFromInvitation` ahora usa una transacción independiente `REQUIRES_NEW`; la prueba del evento confirmado pasó 1/1.
- Se ampliaron los tests de `StaffInvitationListenerH2IT` para validar que el evento CLIENT y una transacción de invitación revertida no crean un perfil, que repetir el evento EMPLOYEE conserva el perfil, que una cuenta ADMIN activa crea el perfil inicial y que una cuenta EMPLOYEE inactiva sincroniza el estado sin perder el puesto. La selección focal más reciente pasó `StaffProfileServiceTest` 10/10, `StaffInvitationListenerH2IT` 6/6 y `StaffPersistenceH2IT` 2/2. Los seis escenarios del listener se ejecutaron después del último verify completo.
- El último `verify -Plocal-h2` completo, anterior a esos dos casos nuevos, terminó `BUILD SUCCESS`: 385 Surefire y 36 Failsafe H2, cero fallos/errores, un test opt-in omitido y todos los umbrales JaCoCo aprobados.
- Tras actualizar README e INVENTARIO, `DocumentationArchitectureTest`, `GovernanceArtifactTest` y `AdrDocumentRulesTest` pasaron 11/11.
- Después de documentar los cinco escenarios del listener, la misma selección documental/gobernanza volvió a pasar 11/11.
- Después de añadir y documentar el caso de rollback, los gates documentales/gobernanza volvieron a pasar 11/11 con Java 21 y Maven offline.
- STF-001 continúa parcial por sus dependencias IAM-010/IAM-012 y por las comprobaciones de aceptación todavía pendientes. No se ejecutó SQL Server ni E2E-001, ni se instalaron herramientas.

## 2026-11-20 — Gate completo Java/H2 y cobertura

- Con Java 21, Maven offline y `local-h2`, la ejecución completa terminó `BUILD SUCCESS`: 381 tests Surefire y 35 Failsafe H2, sin fallos ni errores; un test opt-in de Mailpit quedó omitido.
- JaCoCo informó que todos los umbrales se cumplieron. El informe anterior con cobertura insuficiente estaba obsoleto; no fue necesario modificar código ni pruebas de dominio para resolverlo.
- La salida mostró mensajes de Hibernate al eliminar esquemas/tablas durante el cierre del contexto H2, pero la suite y el gate terminaron correctamente. No se validó SQL Server, Mailpit real ni entorno externo.

## 2026-11-20 — MSG-008 parcial: respuesta textual en hilo interno

- Se añadió `POST /gestion/bandeja/{conversationPublicId}/responder` para personal autenticado. El controlador rechaza CLIENT; la ruta exige uno de los permisos de bandeja/solicitudes y el caso de uso vuelve a comprobar permiso contextual y estado de conversación.
- El formulario permite respuesta `PUBLIC` o nota `INTERNAL`, aplica CSRF y límites de cuerpo, conserva errores de validación accesibles y usa POST/redirect/GET. Solo el mensaje público publica el evento existente con IDs tras confirmar la transacción.
- La respuesta es textual. No se añadieron adjuntos, plantillas de email ni envío desde messaging; no existe consumidor de `MessagePosted` para este flujo. MSG-008 permanece PARCIAL.
## 2026-11-20 — Cobertura MVC de mutaciones administrativas USR-003

- Se parametrizaron las comprobaciones de CSRF y confirmación explícita para las cuatro rutas POST de acciones de usuario: estado, desbloqueo, reset y rol. Se añadió denegación de ADMIN sin `USERS_MANAGE`; no se modificó código productivo.
- `UserAdminActionsControllerTest` pasó 12/12 con Java 21, Maven offline y `-Plocal-h2`. El verify completo previo a esta adición de tests pasó 371 Surefire + 34 Failsafe H2 (1 omitido por opt-in), sin fallos/errores y JaCoCo aprobado; no se repitió porque el cambio posterior solo añade pruebas.
- Tras registrar el resultado en README/INVENTARIO/DIARIO, la selección combinada `UserAdminActionsControllerTest`, `DocumentationArchitectureTest`, `GovernanceArtifactTest` y `AdrDocumentRulesTest` pasó 23/23.
- USR-003 continúa parcial por sus dependencias/validaciones restantes; conversiones de rol CLIENT permanecen bloqueadas por decisión. No se instalaron herramientas ni se iniciaron servicios externos.

## 2026-11-20 — Desbloqueo de gates y cobertura de dominio PROMPT 11

- Se acotó `LayerRulesTest` para que su regla shared/config aplique a los paquetes transversales reales, no a cualquier paquete de módulo cuyo nombre termine en `.config`. Se mantuvo la prueba negativa `shared` usando un selector explícito para su fixture; `LayerRulesTest` pasó 2/2.
- Se añadieron `ConversationEntityTest` y `MessageEntityTest` con invariantes, validación de nulos y límites, transiciones de estado/lectura y comprobación de que `toString()` no filtra el texto de mensajes. La selección de dominio de mensajería pasó 12/12; no cambió código productivo de `messaging`.
- La verificación completa con Java 21, Maven offline y `-Plocal-h2` terminó `BUILD SUCCESS`: 371 Surefire, 34 Failsafe (1 test opt-in omitido), 0 fallos/errores y JaCoCo aprobado. `messaging.domain` alcanzó 100% de cobertura de líneas.
- Tras actualizar README, INVENTARIO y arquitectura con los resultados vigentes, `DocumentationArchitectureTest`, `GovernanceArtifactTest` y `AdrDocumentRulesTest` pasaron 11/11.
- Se mantienen sin cerrar los IDs de PROMPT 11: paneles USR-002 pendientes de aprobación owner; USR-004 pendiente de contributors y política legal; E2E-001 runtime sin ejecutar. SQL Server no fue probado. No se instalaron herramientas ni se iniciaron servicios externos.

## 2026-11-20 — MSG-012: persistencia H2 focal

- Con Java 21, Maven 3.6.3, dependencias locales y el perfil `local-h2`, `ConversationPersistenceH2IT` terminó `BUILD SUCCESS`: 5 tests, sin fallos, errores ni omisiones. Incluye la persistencia de un `MessageEntity` sintético de autor `AI_AGENT` como `INTERNAL` y su exclusión de la lectura pública.
- La selección focal de MSG-012 registrada previamente pasó 25/25 Surefire. Esta prueba H2 no persiste el objeto de borrador `AiReplyDraft`; el borrador sigue siendo inmutable y `PENDING_APPROVAL`.
- MSG-012 permanece PARCIAL: no hay persistencia/flujo de aprobación del borrador, proveedor ni llamada externa; ADR-011 sigue Propuesto/Futuro. La migración no se ejecutó contra SQL Server.

## 2026-11-19 — MSG-006 parcial: canales de contacto

- En `messaging` se añadieron propiedades opcionales para el número WhatsApp E.164, los textos ES/EN y el email de contacto. No se asignaron valores oficiales; sin número/email válidos el fragmento oculta los enlaces.
- El generador restringe el destino WhatsApp a `https://wa.me/`, codifica el texto UTF-8 y construye `mailto:` con email validado y codificado. Los valores inválidos producen una advertencia sin incluir el dato configurado; no se registran PII ni se reporta el clic como mensaje recibido.
- Se creó `templates/messaging/channel-links.html` con nombres accesibles ES/EN, foco visible y tamaño táctil. Aún no se incluye en ninguna página pública: MSG-003 no ha creado el formulario/página consumidora y no se añadió una ruta nueva.
- `ContactChannelLinkFactoryTest` cubre configuración, encoding, canales ausentes/inválidos, prevención de parámetros `mailto` no confiables y renderizado localizado. La tarea queda PARCIAL hasta que exista una página aprobada que integre el fragmento y se ejecute la validación visual/accesible de navegador.

## 2026-11-20 — MSG-007 parcial: bandeja de conversaciones

- Se añadieron `GET /gestion/bandeja` y `POST /gestion/bandeja/buscar` para formulario completo y respuesta HTMX. El POST requiere CSRF y mantiene el término de búsqueda fuera de la URL; la aplicación no lo registra ni persiste.
- La consulta filtra categoría/contexto, estado, asignación, mensajes entrantes no leídos y texto literal dentro de mensajes `PUBLIC`; no busca notas `INTERNAL`. REQUEST se excluye por defecto y exige `REQUESTS_VIEW_ALL` además de `INBOX_MANAGE`.
- El orden usa fecha de conversación e ID como desempate estable. La vista no expone cuerpos, emails ni identificadores de asignados; ofrece tarjetas móviles y tabla de escritorio, controles etiquetados, foco visible y paginación accesible.
- La selección focal pasó 28/28 Surefire (servicio, MVC, permisos, arquitectura, reglas de plantilla e i18n); `ConversationPersistenceH2IT` pasó 4/4, incluyendo filtros, exclusión de notas internas/conversaciones REQUEST no autorizadas y paginación. `npm run build` pasó con el aviso conocido de `caniuse-lite`.
- MSG-007 queda PARCIAL: axe/Playwright no se ejecutó con una sesión y conversaciones sintéticas en navegador. No se afirma cobertura completa ni compatibilidad SQL Server.

## 2026-11-20 — MSG-012 parcial: scaffold IA sin proveedor

- Se añadió `AI_AGENT` como tipo sin identidad de usuario y se mantiene restringido a mensajes `INTERNAL`; la migración SQL Server actualiza únicamente la restricción de autor. La ejecución de esa migración en SQL Server no se realizó.
- `AiReplyDraft` es inmutable y solo puede crearse como `PENDING_APPROVAL`; su `toString` excluye el cuerpo. `AiReplyAssistant` queda como puerto sin implementación ni caller.
- `MessageReceived` ya existía y conserva únicamente UUID públicos de conversación y mensaje; su publicación entrante sigue tras commit. No se añadieron proveedor, llamada externa, persistencia de borradores AI, flujo de aprobación ni envío automático. ADR-011 no se modifica y sigue Propuesto/Futuro.
- `AiReplyDraftTest` comprueba pendiente/validación/redacción y que mensajes `AI_AGENT` no pueden ser `PUBLIC`; el test H2 de mensajería comprueba persistencia INTERNAL y ausencia en lectura pública. La tarea queda PARCIAL hasta contar con decisión formal y controles de R3 para cualquier aprobación/publicación futura.

## 2026-11-19 — Base local de mensajería F3 (MSG-001/002 parcial)

- Se completó la separación de tipos de conversación/mensaje en `messaging::api`, se alineó el filtro JPQL de mensajes públicos con ese paquete, y se declaró `context_public_id` no nulo en el mapeo JPA igual que en la migración. El esquema H2 crea `messaging` antes del ciclo de Hibernate.
- Con Java 21, Maven 3.6.3, dependencias offline y `local-h2`, pasaron 15 tests Surefire focales: `ConversationServiceTest` (10), `DocumentationArchitectureTest` (3), `ModularityTests` (1) y `ModuleDagTest` (1). Pasaron también los 2 tests Failsafe H2 de `ConversationPersistenceH2IT`; `test-compile` compiló todas las fuentes productivas y de test.
- Los casos añadidos verifican la reutilización de una conversación REQUEST existente, publicación de `MessagePosted` con IDs tras commit y ausencia de eventos para notas internas.
- En el primer intento de persistencia falló la carga JPQL por una referencia residual al enum en su antiguo paquete; se corrigió y la repetición pasó 2/2. En la repetición no aparecieron avisos por `DROP` de un esquema inexistente.
- MSG-001/002 permanecen parciales: solo está implementada la vista de personal de lectura; falta una superficie cliente y no está conectado un consumidor de notificaciones. El formulario público CONTACT pertenece a MSG-003/F4 y no se implementó aquí. No se ha ejecutado la migración en SQL Server. REQ-001..011 siguen bloqueados por STF-001 parcial. No se afirma cobertura completa del proyecto ni validación externa.
- Tras autorización expresa del usuario para adoptar localmente la propuesta de ruta, se añadió GET `/gestion/bandeja/{conversationPublicId}` como vista Thymeleaf de solo lectura. La ruta exige `INBOX_MANAGE` o `REQUESTS_VIEW_ALL`; el controller limita a EMPLOYEE/ADMIN y la API vuelve a comprobar permiso contextual/alcance del hilo. Mensajes se renderizan con `th:text`; bundles ES/EN incluidos. La aprobación owner formal continúa pendiente y la vista cliente no se añadió.
- `ConversationThreadControllerTest` pasó 6/6, y la selección focal consolidada de mensajería, autorización, arquitectura, plantillas, i18n, assets y convención de migraciones pasó 47/47 Surefire. Se verificaron permisos contextuales, denegación de CLIENT en la ruta de personal, 404 sin enumeración, etiquetas ES/EN, escape HTML, landmarks y mensaje de estado accesible. Los 2 tests H2 de persistencia pasaron en una ejecución focal anterior a la vista web.
- `npm run build` pasó y regeneró CSS/assets fingerprintados con las clases de la plantilla; solo mostró el aviso conocido de `caniuse-lite` desactualizado. `FrontendAssetPipelineTest`, `I18nKeyParityTest` y `PortalLocaleResolverTest` pasaron 11/11. No se ejecutó Playwright/axe sobre la nueva vista: no se inició el servidor con una sesión y un hilo sintético.

## 2026-11-19 — Reanudación local de PROMPT 10

- Con Java 21, Maven 3.6.3, repositorio offline y perfil `local-h2`, la fase `test-compile` terminó `BUILD SUCCESS`; Maven indicó que las fuentes productivas y de test ya estaban al día.
- Dos conjuntos focales de PROMPT 10 y sus prerrequisitos pasaron: 41 tests Surefire y 5 Failsafe H2, sin fallos, errores ni omisiones. Maven mostró avisos no bloqueantes por POM metadata ausente; no se instalaron herramientas ni se descargaron dependencias.
- Se añadió en `e2e/portal-ui.spec.ts` la prueba axe de accesibilidad del formulario `/registro`; `npm run e2e -- --list --grep "customer registration form is accessible"` la descubre. No se ejecutó el navegador: `PORTAL_E2E_BASE_URL` no está configurado y no hay servidor en localhost:8090. Estado `POSTPONED`; no se instalaron herramientas.
- LGL-001 y CUS-001..004 tienen implementación local observable. Esto no cierra por sí solo sus IDs; SQL Server no se ejecutó y el país fiscal soportado sigue limitado a España.
- LGL-002 sigue sin flujo de solicitud/exportación completo ni contributors registrados para todos los owners. La anonimización irreversible permanece bloqueada por aprobación legal pendiente. LGL-003 no tiene política/job activo: solo está autorizado el dry-run y faltan categorías y plazos aprobados.
- `/cuenta/datos` es informativa. Los metadatos de invitaciones customer pendientes todavía no tienen caducidad/limpieza propia. No se realizó ninguna purga ni se afirmó validación externa.

## 2026-11-19 — Reanudación local de PROMPT 11 y desbloqueo Maven

- Se verificó el origen del bloqueo offline: `oss-parent-7.pom` y otros artefactos transferidos estaban presentes en el repositorio Maven local, pero sus entradas `_remote.repositories` indicaban `central`; Maven usa el ID `maven-public` y en modo offline no los aceptaba. Se quitaron solo las entradas de origen de artefactos existentes, tratándolos como instalados localmente. No se descargaron artefactos ni se modificó el POM del proyecto.
- Con Java 21, Maven 3.6.3 y el repositorio offline indicado por el usuario, `-Plocal-h2 compile` terminó `BUILD SUCCESS`. El test focal `AdminUserActionsTest` pasó 6/6. La verificación completa `-Plocal-h2 verify` terminó `BUILD SUCCESS`: 322 tests Surefire, 30 Failsafe H2 (29 aprobados y 1 omitido por opt-in), sin fallos/errores y JaCoCo aprobado.
- PROMPT 11 sigue parcial. El código y los tests locales disponibles pasan, pero USR-002 necesita contratos/rutas aprobados por los owners `request`, `messaging` y `simbiosphere`; USR-003 mantiene bloqueadas las transiciones de rol que involucran CLIENT hasta acordar el workflow owner; USR-004 espera contributors de todos los owners y política legal de retención/anonimización aprobada. E2E-001 sigue `POSTPONED`: requiere aplicación, Mailpit, navegador y credenciales sintéticas ADMIN/MFA; no se instalaron herramientas ni se simuló su ejecución.
- No se declara cerrado ningún ID pendiente ni compatibilidad SQL Server. El `verify` local no sustituye la ejecución de E2E-001 ni las decisiones de owners/legal.

## 2026-11-19 — Refuerzo local de USR-001 (exportación CSV)

- Se ampliaron las pruebas de `UserManagementControllerTest` para verificar el rechazo si falta cualquiera de los permisos de exportación, el límite de 1000 filas antes de registrar auditoría, la paginación multi-página conservando filtros/orden, la neutralización de fórmulas y la auditoría sin PII.
- Con Java 21, Maven 3.6.3, el repositorio local indicado y `-o -B -Plocal-h2`, la clase focal pasó 5/5. La selección relacionada de cinco clases pasó 25/25: `UserManagementControllerTest`, `UserManagementServiceTest`, `AuthorizationMatrixTest`, `StaffManagementControllerTest` y `AdminUserActionsTest`. No se instalaron herramientas ni se iniciaron servicios externos.
- El último `verify` completo (322 Surefire y 30 Failsafe H2, uno omitido, JaCoCo aprobado) fue anterior a estas pruebas nuevas; no se presenta como verificación del árbol actual completo. E2E-001 sigue `POSTPONED`; USR-001 permanece parcial hasta completar las validaciones requeridas.

## 2026-11-19 — Cobertura local adicional USR-003 y gate JaCoCo

- `AdminUserActionsTest` añade cobertura para desbloqueo efectivo (limpia bloqueo/contador, persiste, audita y notifica) y solicitud de restablecimiento (delegación al flujo recovery y auditoría). La clase pasó 8/8 con Java 21 y Maven offline.
- El `verify -Plocal-h2` completo ejecutó 334 tests Surefire y 32 Failsafe H2 (uno omitido por opt-in): 0 fallos y 0 errores en pruebas. La build terminó FAIL porque JaCoCo detectó 71% de cobertura de líneas en `com.simbio.core.messaging.domain`, por debajo del 80%. No se rebajó el umbral ni se modificó el módulo `messaging`, ajeno a los cambios F2 de este turno.
- La selección focal combinada de ocho clases pasó 38/38 (27 F2/autorización y 11 documentación/gobernanza) tras incorporar la fila owner `messaging` que exigía `DocumentationArchitectureTest`. El `verify` anterior exitoso es histórico y no cubre el árbol actual.
- Decisión del usuario: mantener bloqueadas las conversiones de rol que involucren CLIENT hasta disponer de un workflow owner de cuenta cliente. `AdminUserActions` ya las rechaza; se añade prueba para EMPLOYEE→CLIENT que complementa el rechazo CLIENT→EMPLOYEE. No cambia el comportamiento de producción.
- Tras añadir la prueba inversa, `AdminUserActionsTest` pasó 9/9 en modo offline; no se repitió la selección combinada ni el `verify` completo.
- Decisión del usuario para USR-002: mantener los paneles request/messaging/SimbioSphere parciales hasta la aprobación de sus owners; no habilitar rutas ni definir datos/permisos por inferencia.
- Se alineó el límite del formulario de especialidades de trabajador con las reglas del owner `staff`: acepta hasta 20 nombres de 80 caracteres en campos separados por CRLF y devuelve error de formulario para más de 20 o nombres largos, sin llegar a una excepción de servicio. `StaffManagementControllerTest` y `StaffProfileServiceTest` pasaron 18/18 en Maven offline; gates i18n/plantillas y compilación pendientes.
- No se instalaron aplicaciones ni se iniciaron servicios externos. La implementación/validación E2E-001 permanece `POSTPONED`; USR-002/USR-003 (transiciones CLIENT)/USR-004 siguen limitadas por contratos owner y decisiones legales pendientes.

## 2026-11-19 — Alineación de validación de especialidades STF-002

- Se detectó que el formulario aceptaba 1.600 caracteres aunque el owner `staff` permite 20 especialidades de hasta 80 caracteres; además, las entradas con más de 20 líneas o nombres demasiado largos podían llegar al servicio y provocar una excepción.
- Se alineó el límite de formulario a 1.638 caracteres para contemplar veinte líneas CRLF, se añadieron validaciones web/localizadas de cantidad y longitud, y se documentó la regla ES/EN.
- `StaffManagementControllerTest` + `StaffProfileServiceTest`: 18/18; selección de staff/UI/documentación: 34/34; `compile -Plocal-h2`: BUILD SUCCESS. No se instalaron herramientas ni se iniciaron servicios externos.
- Después de actualizar README/INVENTARIO/DIARIO, `DocumentationArchitectureTest` + `GovernanceArtifactTest` pasaron 7/7.
- El último `verify -Plocal-h2` ejecutó 350 Surefire y falló con un único error de regla arquitectónica en `LayerRulesTest.applicationClassesRespectAllSixRules` por la dependencia `messaging.config.MessagingConfiguration` → `messaging.config.MessagingChannelProperties`; Failsafe y JaCoCo no se alcanzaron. El verify completo anterior tampoco pasó su gate final: JaCoCo informó 71% en `messaging.domain` frente al 80%, con pruebas sin fallos/errores.
- PROMPT 11 continúa parcial: E2E-001 no se ejecutó; USR-002 espera aprobación de owners y USR-004 permanece bloqueada por contributors/política legal.

## 2026-11-19 — Revisión de disponibilidad Maven/npm

- En esa ejecución histórica de `test-compile` en modo offline, Maven no pudo cargar el modelo por el registro de origen de `org.sonatype.oss:oss-parent:pom:7`; la entrada fue corregida posteriormente como se documenta arriba. La anotación anterior de que el POM faltaba físicamente y la caché no se había modificado queda supersedida.
- `npm ls --depth=0` encontró las dependencias frontend declaradas y `npm run build` terminó correctamente; solo informó de que `caniuse-lite` está desactualizado. No hace falta descargar de nuevo las dependencias npm para continuar con los gates frontend actuales.
- El último full run previo a esta incidencia seguía siendo exitoso (321 Surefire, 29 Failsafe H2 aprobados y uno omitido por opt-in, JaCoCo aprobado); el resultado más reciente de 322 Surefire y 30 Failsafe se registra en la entrada nueva de este diario.

## 2026-11-19 — Implementación local de IAM-006 y verificación final de PROMPT 09

- Se completó IAM-006 en `config`: `GET /anti-bot/challenge` devuelve el formato de desafío ALTCHA con `Cache-Control: no-store`; el cliente local resuelve SHA-256, serializa el payload, y el verificador comprueba firma, expiración, honeypot, confirmación accesible y consumo de un solo uso. El GET queda rate-limited. `/registro` usa asset fingerprintado y fallback accesible si JavaScript no está disponible. No existe formulario de contacto en el workspace.
- Se añadieron tests para payload y desafío, firma inválida, expiración, replay secuencial y concurrente, honeypot, fallback, endpoint JSON/no-store/rate limit y binding del formulario. También se añadió `npm run test:anti-bot` (3/3 tests). Las 16 pruebas Java focales pasaron; `npm run build` pasó con el aviso no bloqueante de `caniuse-lite` desactualizado.
- Se preparó `SmtpMailpitIT`, que envía una verificación sintética por SMTP y revisa en la API de Mailpit el asunto y las partes HTML/texto. Se opta con `-Dportal.test.mailpit.enabled=true`; el `verify` local lo omitió (1 test skipped) porque Mailpit no se habilitó. Estado `POSTPONED`, no PASS. Para ejecutar en entorno habilitado: arrancar `mailpit` con `docker compose -f docker-compose.dev.yml up -d mailpit` y correr `$env:JAVA_HOME='C:\Users\x455035\2Programs\temurin-21.0.11'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; & 'C:\Users\x455035\2Programs\maven-3.6.3\bin\mvn.cmd' -o -B "-Dmaven.repo.local=C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias" -Plocal-h2 -Dportal.test.mailpit.enabled=true -Dportal.test.mailpit.smtp-host=localhost -Dportal.test.mailpit.smtp-port=1026 -Dportal.test.mailpit.http-base-url=http://localhost:8026 verify`. No se presenta la entrega real como PASS mientras no se ejecute.
- Se intentó obtener `altcha@3.6.0` para probar el widget oficial, pero npm falló con `ECONNRESET` al conectar con `registry.npmjs.org`; no se añadió la dependencia ni se modificó el lockfile. La implementación local usa el formato/payload ALTCHA y pasó sus pruebas, pero la interoperabilidad con el paquete oficial queda `POSTPONED` hasta poder obtenerlo y ejecutarlo. La caché anti-replay es por proceso y no coordina varias réplicas; no se añadió tabla, pues IAM-006 declara expresamente que no crea tablas/eventos.
- Verificación final con Java 21, Maven 3.6.3, `-o -B -Plocal-h2`: `BUILD SUCCESS`, 321 tests Surefire sin fallos, 30 Failsafe (29 aprobados, `SmtpMailpitIT` omitido por opt-in) y JaCoCo aprobado. `npm run build` y `npm run test:anti-bot` también pasaron. No se ejecutó SQL Server, Mailpit real, Playwright ni despliegue. PROMPT 09 se marca `COMPLETADO_LOCAL`; NTF-002 permanece abierto para la entrega real Mailpit y la interoperabilidad del widget oficial sigue pospuesta, sin presentarse como PASS.

## 2026-11-19 — Política transversal para herramientas de prueba no disponibles

- Se armonizó en los prompts autónomos, el prompt maestro, calidad/testing, operación y la skill del proyecto la regla de continuar implementando código, configuración y tests aunque falte localmente el ejecutor externo.
- Se ejecutan todos los gates disponibles; un fallo bloquea avanzar tareas dependientes. Solo se pospone la ejecución que dependa estrictamente del recurso ausente, con estado `POSTPONED` (`PREPARADO_NO_EJECUTADO` para PRE), test, motivo, entorno, evidencia requerida e instrucciones reproducibles para ejecutarla en otra máquina habilitada; nunca se presenta como PASS. La ausencia no bloquea la implementación o el trabajo independiente; los IDs con validación obligatoria pendiente siguen abiertos.
- README e inventario distinguen la entrega local de la prueba real Mailpit, que queda pospuesta hasta disponer de un entorno habilitado. IAM-006 ya tiene implementación y pruebas locales; la interoperabilidad del widget oficial y replay multi-réplica siguen sin validar.
- Evidencia documental vigente: la última `verify -Plocal-h2` pasó con 321 Surefire, 30 Failsafe H2 (29 aprobados, 1 omitido por opt-in) y JaCoCo aprobado. La suite se ejecutó para el cambio de código, no para el ajuste documental.

## 2026-11-19 — Verificación completa del estado actual

- Se repitió `verify` con Java 21, Maven 3.6.3, `-o -B`, perfil `local-h2` y el repositorio local indicado. Resultado: `BUILD SUCCESS`, 304 tests Surefire y 29 Failsafe H2, sin fallos ni errores; JaCoCo aprobó todos los umbrales. Esta ejecución completa supersede la revalidación anterior que había fallado en Surefire y la verificación selectiva de 76 pruebas.
- No se descargaron dependencias ni se aplicaron migraciones a SQL Server. La propuesta documental de USR-002 no altera el comportamiento implementado.

## 2026-11-19 — Revalidación selectiva de los fallos Surefire

- Se abordaron las clases que fallaron en la ejecución completa: corrección del key-id ausente en `AesGcmSensitiveDataProtector`; uso de `SpringTemplateEngine` en el render de correo; y regla ArchUnit limitada a clases top-level para los adapters de infraestructura. El workspace también contiene el nombre de bean diferenciado para el servicio de registro de identity, `AuthenticatedUser` del shared en controladores de staff/customer, owner `staff` en el inventario y los ajustes de datos/assertions para auditoría y CSV.
- La sintaxis de `AuditController` ya no bloqueó la compilación. Se generaron los assets con `npm run build` y se ejecutaron solo las 23 clases que aparecían en el informe anterior. Resultado: 76 tests, 0 fallos, 0 errores, `BUILD SUCCESS`. Incluyó las pruebas MVC/contexto y las de arquitectura y SMTP que anteriormente habían fallado.
- Este gate selectivo no vuelve a ejecutar los demás tests, Failsafe ni JaCoCo, por lo que no equivale a aprobación del verify completo. Se conserva el resultado anterior de 266 Surefire (1 fallo, 36 errores) y Failsafe separado 29/29 hasta una nueva ejecución completa.

## 2026-11-18 — Investigación de contratos USR-002

- Se buscó una base H2 persistida en `DEMO-orders-thymeleaf`; no hay archivo de base de datos. Las pruebas crean bases `jdbc:h2:mem:` temporales con tenants/usuarios sintéticos. Se acepta usar H2 solo como fixture de prueba; no representa ni certifica el esquema SQL Server futuro.
- La inspección de fuentes del proyecto DEMO encontró una administración de usuarios scoped por tenant y APIs Java internas de órdenes/reparaciones con `tenantId` y `locationId` explícitos; no son contratos UI ni API HTTP de S-portal. No existen allí módulos equivalentes de messaging o SimbioSphere que permitan derivar sus rutas.
- Se redactó en `contratos-ui-backend.md` una propuesta no aprobada para fragmentos GET de request, messaging y SimbioSphere. La futura interoperabilidad DEMO queda condicionada a un mapping server-side de cuenta→tenant/location y una proyección mínima; se excluyen IDs internos en navegador, joins cross-module y campos de detalle sensibles. Los tests H2 `mem:` de DEMO aportan fixtures sintéticos para aislamiento, no un modelo SQL Server. No se implementaron rutas ni integración; aprobación owners y contrato HTTP futuro siguen bloqueando el cierre de USR-002.

## 2026-11-18 — Auditoría de dependencias locales y de roadmap

- Con Java 21 y Maven offline, `compile`, `test-compile` y `package` terminaron correctamente. El empaquetado advirtió que falta el POM de `org.tukaani:xz:1.10`, pero generó el JAR. El parent Artemis no volvió a bloquear la resolución; Surefire y Failsafe resolvieron el provider/plugin con `plexus-utils:3.5.1`.
- Surefire ejecutó 266 pruebas: 1 fallo y 36 errores. La ejecución separada de Failsafe ejecutó 29 pruebas H2 sin errores. JaCoCo no se ejecutó después del resultado Surefire. No se clasifican los fallos de pruebas como dependencias ausentes.
- `npm run build` pasó con aviso de base Browserslist antigua. Los paquetes declarados están instalados y Chromium headless de Playwright se lanzó localmente. Node/npm globales son 24.22.1/11.19.1; el perfil Maven `frontend-build` fija Node 22.14.0/npm 10.9.2 y plugin 1.15.1, pero no se probó ese perfil.
- La revisión del roadmap identificó previsiones no incorporadas: Resilience4j Retry/CircuitBreaker 2.3.0 para SIM-003; se proponen WireMock 3.13.1 y Swagger Parser 2.1.27 para SIM-004 (no se encontraron pins en los POM/package de DEMO inspeccionados); para IAM-006 se propone la biblioteca oficial Java `org.altcha:altcha:2.1.1`, con `org.json:json:20240303` requerido en runtime, y el widget npm `altcha@3.6.0`. Son propuestas, no se añadieron ni validaron juntas. Thymeleaf Layout Dialect 3.3.0 y Thymeleaf Extras Spring Security 3.1.2.RELEASE están mencionados en ADR-001, pero no se usan y la compatibilidad Boot 4/Security sigue pendiente; no se solicitan. Dependency-Check 11.1.1 del perfil CI no se ejecutó ni tiene resolución offline comprobada. El plugin Maven frontend 1.15.1 resuelve offline, pero la comprobación omitió sus goals: la máquina tiene Node/npm globales 24.22.1/11.19.1 y no tiene instalado el toolchain fijado 22.14.0/10.9.2. Las imágenes de desarrollo declaradas son SQL Server `2022-latest`, Mailpit `v1.27.8` y ClamAV `1.4`; no son necesarias para el perfil H2.
- No se descargaron dependencias, no se inspeccionó ni modificó la caché Maven y no se ejecutaron los servicios Docker. El resultado actual no certifica SQL Server ni el perfil CI.

## 2026-11-18 — Revalidación de PROMPT 09 tras actualizar caché Maven

- Se confirmó que `surefire-junit-platform:3.5.6` ya estaba presente. El verify offline ejecutó 265 tests Surefire: 7 fallos y 39 errores. Failsafe y JaCoCo no llegaron a ejecutarse. La verificación exitosa anterior de 303 Surefire/29 Failsafe sigue siendo una evidencia válida para el estado que entonces se compiló, pero no sustituye esta revalidación fallida.
- En las pruebas de PROMPT 09 pasaron `OneTimeTokenServiceTest` (4/4), `MfaServiceTest` (2/2), `AdminMfaFilterTest` (6/6), `OutboxServiceTest` (2/2), `OutboxWorkerTest` (5/5) y `AccountLoginSuccessHandlerTest` (1/1). `AesGcmSensitiveDataProtectorTest` detectó que un key ID ausente provocaba NPE. Se añadió guard para key ID null/vacío; luego las pruebas focales AES-GCM y token pasaron 7/7.
- Tres pruebas de render en `SmtpEmailSenderTest` dieron `NoClassDefFoundError: ognl/PropertyAccessor`; no se halló el JAR OGNL en el repositorio offline. Se cambió el motor de texto plano y el helper de test a `SpringTemplateEngine`, pero esa edición posterior no se pudo recompilar: Maven encontró sintaxis inválida en `audit.web.AuditController.java:83`, archivo ajeno al alcance. El owner pidió dejarlo intacto.
- La regla de arquitectura también trató una clase anónima y una excepción interna de `SmtpEmailSender` como implementaciones top-level de infraestructura; se ajustó el test ArchUnit para aplicar esa regla solo a clases top-level. Esta edición tampoco se pudo recompilar por el error concurrente de AuditController.
- No se afirma que el verify esté aprobado: además de Prompt 09, fallaron gates de arquitectura, algunos tests MVC/contexto y otras pruebas del workspace. No se ejecutó Failsafe/H2 ni JaCoCo en esta revalidación.

## 2026-11-18 — Continuación PROMPT 11: reenvío paginado de invitaciones

- `identity` añade listados separados de invitaciones pendientes para clientes y trabajadores, con paginación de 25, orden estable por creación descendente/ID ascendente y correo enmascarado. Se incluyen invitaciones expiradas para que puedan reenviarse; consumidas y revocadas no aparecen.
- El reenvío está protegido por el permiso de gestión del ámbito y CSRF. En una transacción, revoca el token anterior y emite uno nuevo manteniendo la referencia opaca de la invitación. La ruta devuelve un resultado genérico y no expone email completo ni token.
- Se añadieron pruebas unitarias, de controlador, matriz de autorización y H2 para listado, rotación, conservación de correlación y límites de página. La verificación Java 21/Maven 3.6.3, offline y con `local-h2` terminó en `BUILD SUCCESS`: 303 tests Surefire y 29 Failsafe H2 sin fallos; JaCoCo aprobó todos los umbrales.
- Se actualizaron README, INVENTARIO, contratos UI/backend, rutas y seguridad. PROMPT 11 sigue parcial: faltan paneles de request/messaging/SimbioSphere por contratos de owners, transiciones de rol que incluyen CLIENT, exportación/anónimo RGPD hasta contributors y política legal aprobados, y E2E-001 con MFA/Mailpit/CMS. SQL Server no se verificó.

## 2026-11-18 — Cobertura Java y validación final

- Se fijó `org.codehaus.plexus:plexus-utils:3.5.1` también en `maven-failsafe-plugin`, igual que en Surefire, para que la ejecución offline no intentara resolver `1.1`.
- Se añadieron tests unitarios para recuperación de contraseña, verificación de email, invitaciones, perfil, enrolamiento y recuperación MFA, directorios de cliente/staff y reglas del dominio de identidad.
- La prueba focal de plantillas pasó 6/6; el conjunto focal de cobertura pasó 42/42. La verificación completa con Java 21, Maven 3.6.3, `-o -B`, `-Plocal-h2` y el repositorio local indicado terminó en BUILD SUCCESS: 298 tests Surefire y 27 Failsafe H2, sin fallos ni errores.
- JaCoCo pasó el mínimo del 80%: `customer.application` 88.4%, `identity.domain` 97.8%, `staff.application` 94.5% e `identity.application` 87.3%. No se pasó `-s`; Maven usó sus settings predeterminados. No se descargaron dependencias.
- PROMPT 07 ERR-004 está completado en el alcance de desarrollo local. PROMPT 08/09/10/11 siguen parciales por trabajo funcional pendiente. SQL Server, Mailpit real, despliegue, runtime y validaciones externas no se probaron.

## 2026-11-18 — Verificación inicial de PROMPT 11 (antes de ampliar cobertura)

- Con Java 21, Maven 3.6.3, `local-h2` y el repositorio offline indicado, `verify` ejecutó 266 tests unitarios y 27 tests Failsafe de integración H2; todos finalizaron sin fallos ni errores.
- Failsafe ya no solicita `org.codehaus.plexus:plexus-utils:1.1`; la ejecución superó las fases de pruebas de integración y JaCoCo generó el reporte.
- BUILD FAILURE ocurre únicamente en `check-domain-application-coverage`: `customer.application` 75.7%, `identity.domain` 75.8%, `staff.application` 68.1% e `identity.application` 55.1%, frente al mínimo del 80%.
- Los tests focales del directorio de identidad, panel de auditoría y matriz de autorización pasaron 12/12. PROMPT 11 sigue parcial por criterios funcionales pendientes y el gate de cobertura. SQL Server, Mailpit, MFA real y E2E-001 no se verificaron.

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

## 2026-11-18 — Verificación de cierre de PROMPT 07 (ERR-004)

- Se verificó el viewer bajo `config`, con rol ADMIN y `ERROR_INVENTORY_MANAGE`, CSRF en cambios, filtros/paginación, resumen saneado, auditoría mínima y estado degradado sin detalles internos. No se añadieron tablas ni migraciones.
- Con JDK 21, Maven 3.6.3, `local-h2` y el repositorio offline documentado, los tests focales pasaron: `ErrorInventoryServiceTest` 4/4, `ErrorInventoryControllerTest` 9/9 y `ErrorInventoryUnavailableIT` 2/2.
- El `verify` completo pasó: 298 tests Surefire, 27 Failsafe, sin fallos/errores y JaCoCo aprobado. La ejecución previa con un error intermitente de timeout de ClamAV no se toma como resultado final.
- ERR-004 queda validada para los criterios de desarrollo. No se verificaron SQL Server, despliegue productivo, almacenamiento multi-réplica ni aprobaciones de retención/límites operativos.

## 2026-11-19 — Refuerzo de accesibilidad focal de PROMPT 07

- Se añadió a `ErrorInventoryControllerTest` una comprobación MVC de encabezado principal, caption/encabezados de tabla, labels asociados a filtros, resumen de detalles y controles de cambio de estado.
- Se ejecutó únicamente `ErrorInventoryControllerTest` con Maven offline, JDK 21 y `local-h2`: 9/9 tests pasaron. No se lanzó la suite completa.

## 2026-11-18 — Reanudación de PROMPT 07 (ERR-004)

- Con JDK 21, Maven 3.6.3 y el repositorio offline documentado, `ErrorInventoryServiceTest` pasó 4/4 y `ErrorInventoryControllerTest` 8/8. `ErrorInventoryUnavailableIT` pasó 2/2 al seleccionarla directamente con Surefire; esto valida lectura y actualización cuando el almacenamiento no está disponible, pero no equivale a ejecutar el goal Failsafe.
- El `verify` completo ejecutó 266 tests y terminó fallando por un error de `TemplateRulesTest`: `notification/email/message.html` infringe la regla de color directo. Los tests focales de ERR-004 pasaron; no se modificó ese archivo ajeno al alcance.
- El verify focal con Failsafe no pudo completar su fase de integración porque el plugin aún solicita `org.codehaus.plexus:plexus-utils:1.1` en offline. ERR-004 permanece abierto hasta que pase el gate aplicable; SQL Server y despliegue no se verificaron.

## 2026-11-19 — Cierre local de PROMPT 07 (ERR-004)

- Se confirmó el estado actual ejecutando exclusivamente `ErrorInventoryServiceTest`, `ErrorInventoryControllerTest` y `ErrorInventoryUnavailableIT`: 15/15 tests pasaron con JDK 21, Maven offline y `local-h2`.
- PROMPT 07 / ERR-004 queda completada para el alcance de desarrollo local. El `verify` completo anterior había pasado (303 Surefire, 29 Failsafe y JaCoCo), pero la revalidación más reciente del workspace ejecutó 266 tests Surefire con 1 fallo y 36 errores; JaCoCo no corrió. No se atribuye esa ejecución global fallida a ERR-004 sin evidencia focal que lo confirme.
- Retención/límites aprobados, alertas, despliegue multi-réplica y SQL Server quedan pendientes operativos/de producción; no bloquean el cierre local de desarrollo.

## 2026-11-19 — Continuación local de PROMPT 11

- Se añadieron tests MVC para los controladores de staff e identity: autorización por rol/permisos, CSRF en mutaciones, validación de formularios, rechazo de cuentas CLIENT en la ficha laboral, acciones administrativas confirmadas y no exposición de cuentas no visibles. Los 12 tests focales pasaron.
- Se corrigió el reemplazo del bean de invitaciones en el contexto de test para mockear el tipo concreto `InvitationService`; el mock de la interfaz `InvitationIssuer` causaba un conflicto de tipo porque el controlador de identidad también inyecta la implementación concreta.
- E2E-001 quedó preparado en Playwright: login ADMIN con MFA, invitación a dirección sintética, token obtenido de Mailpit local, aceptación y rechazo de reutilización, comprobación del acceso denegado al CMS para EMPLOYEE y edición de borrador por ADMIN. El test no guarda capturas ni trazas. `npm run e2e -- --list` descubrió 17 tests y el filtro E2E-001 descubrió 1; esto valida el descubrimiento, no el runtime.
- Se comprobó sin mostrar valores que 8090, 8026 y 1026 están cerrados y faltan las variables de entorno E2E. El recorrido de navegador queda pospuesto hasta disponer de aplicación local con perfiles `local-h2,e2e`, Mailpit y credenciales sintéticas de ADMIN/MFA. No se validó SQL Server.
- Maven offline con JDK 21 y `local-h2` pasó el `verify`: 320 tests Surefire y 30 Failsafe (1 omitido), sin fallos/errores y con JaCoCo aprobado. Los IDs de PROMPT 11 siguen parciales/abiertos por dependencias y decisiones owner/legal; no se declara cerrado el prompt.
- El usuario confirmó mantener la política actual de CMS: EMPLOYEE no recibe `CONTENT_EDIT`, se prueba su denegación y ADMIN realiza la edición autorizada. No se amplió ninguna autoridad.
- Tras registrar esa decisión se repitió el gate documental focal; Maven se detuvo antes de cargar el proyecto porque la caché offline no contiene `org.sonatype.oss:oss-parent:pom:7`. Los 11 tests documentales habían pasado en la ejecución focal inmediatamente anterior; no se descargó ni alteró la caché.
- Se añadió a `AdminUserActionsTest` un caso positivo que verifica persistencia, auditoría, notificación y evento al activar una cuenta. Su ejecución focal se intentó y queda `POSTPONED` porque Maven vuelve a detenerse antes de cargar el proyecto por el POM padre `org.sonatype.oss:oss-parent:7`; el test no está incluido en el `verify` previo.
