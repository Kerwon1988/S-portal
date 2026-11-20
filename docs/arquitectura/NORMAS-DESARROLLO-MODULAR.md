# Normas de desarrollo modular P01–P12

Estas normas son el contrato para implementar y revisar cambios de S-portal. Se aplican al monolito completo; un cambio no puede eludirlas mediante una plantilla, tarea asíncrona, test o configuración.

| ID | Norma | Gate de revisión |
|---|---|---|
| P01 | **Ownership único.** Cada tabla, migración, agregado, plantilla funcional y recurso de módulo tiene un owner identificado en la arquitectura y el inventario. `config` posee solamente composición transversal y `shared` solo contratos mínimos. | El cambio declara el owner y no escribe datos ajenos. |
| P02 | **Contratos publicados.** Los módulos se comunican mediante su paquete `api` o SPI explícita en `shared`; nunca importan entidades, repositorios, controladores o plantillas de otro módulo. | Modulith, ArchUnit y revisión de imports. |
| P03 | **DAG acíclico.** Las dependencias siguen el DAG aprobado de `03-arquitectura.md`. Una dependencia nueva requiere actualizar arquitectura, contratos, inventario y los gates relacionados. | `ApplicationModules.verify()` y `ModuleDagTest`. |
| P04 | **Capas.** La dirección es `web → application → domain`; infraestructura implementa puertos. Dominio no depende de Spring MVC, infraestructura ni otras capas. Controladores llaman casos de uso y no repositorios. | Reglas ArchUnit y pruebas negativas. |
| P05 | **Datos propiedad del módulo.** Un solo esquema/owner por módulo; migraciones Flyway se ubican bajo el owner. No se modifican tablas ajenas ni se introducen migraciones destructivas. H2 es el motor automatizado de prueba y no certifica SQL Server. | Revisión de migraciones, owner y tests H2. |
| P06 | **Eventos mínimos.** Eventos Modulith contienen identificadores y estado técnico mínimo; nunca PII, secretos, contraseñas ni tokens. Los efectos remotos ocurren fuera de la transacción local y son idempotentes. | Tests de publicación, redacción e idempotencia. |
| P07 | **UI server-side por contrato.** Thymeleaf genera páginas y fragmentos; HTMX solicita HTML/redirects. Las rutas y respuestas se acuerdan en `contratos-ui-backend.md`; no se crean SPAs ni endpoints JSON sin necesidad contractual. Cada fragmento conserva permisos, locale, foco y contexto de error. | Contrato UI↔backend y pruebas MVC/HTMX. |
| P08 | **Seguridad por defecto.** Denegar salvo permiso explícito; validar rol y propiedad en el servidor; CSRF en toda mutación web, incluidos formularios HTMX y preferencias. No confiar en navegación oculta, IDs del cliente, HTML ni validación JavaScript. | Pruebas negativas de sesión, CSRF, rol y ownership. |
| P09 | **PII y límites de confianza.** No registrar ni emitir PII, credenciales o tokens en eventos, telemetría, diagramas o prompts. Ficheros quedan fuera del webroot, se validan y escanean; la integración M2M usa backend→HTTPS, nunca el navegador o acceso a BD externa. | Threat review, tests de redacción y límites de payload. |
| P10 | **UI accesible y localizada.** Textos de interfaz tienen claves ES/EN equivalentes por módulo. Usar tokens semánticos, claro/oscuro, reflow móvil, foco visible, teclado y WCAG 2.2 AA; respetar CSP nonce, mejora progresiva y recursos locales sin CDN. | Paridad i18n, contraste, reglas de plantilla, axe y teclado. |
| P11 | **Pruebas y documentación.** Cada cambio aporta el código, configuración y tests requeridos aunque falte localmente una herramienta. Identificar las clases modificadas, añadidas e impactadas y ejecutar solo sus tests relacionados, más los gates obligatorios aplicables a ese alcance; no lanzar la suite completa por defecto. La suite completa es el último recurso cuando no exista una selección más acotada y fiable o un criterio de aceptación la exija explícitamente; registrar el motivo. Corregir fallos relacionados antes de avanzar tareas dependientes. Posponer solo la ejecución que requiera estrictamente el recurso ausente, documentando test, herramienta/entorno, motivo e instrucciones reproducibles para ejecutarlo en una máquina habilitada como `POSTPONED` (`PREPARADO_NO_EJECUTADO` para PRE); nunca marcarlo como PASS ni cerrar la tarea si falta una validación obligatoria. README, INVENTARIO y DIARIO reflejan evidencia observable; actualizar ADR si cambia una decisión. | Tests/gates focales registrados, motivo si se ejecuta la suite completa, estado de diferidos y enlaces documentales válidos. |
| P12 | **Excepciones explícitas.** Una excepción de arquitectura debe indicar regla, justificación, alcance, owner, evidencia compensatoria, fecha de caducidad y aprobador. Una excepción sin owner o vencimiento se rechaza; no se convierte en precedente tácito. | Validador/checklist bloquea metadatos ausentes o caducados. |

## Ejemplos mínimos

- Correcto: un controlador de `content` llama su caso de uso y obtiene metadatos de `files` por API.
- Incorrecto: una vista de `request` incluye directamente un fragmento Thymeleaf de `customer` o consulta su repositorio.
- Correcto: un menú oculta la acción no permitida y el endpoint vuelve a validar el permiso y la propiedad.
- Incorrecto: un evento `ClientRegistered` incluye email, token de activación o contraseña.

## Revisión

Antes de editar identifica tarea, criterios, módulos owners, dependencias, rutas/contratos, datos, riesgos y tests. Ante ambigüedad que cambie seguridad, datos o contrato, detén la implementación y registra el bloqueo; no inventes una aprobación.
