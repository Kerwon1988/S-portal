# F3 — Modelo, estados y experiencia cliente (mensajería núcleo y solicitudes): tareas

- Objetivo: definir agregados, estados y recorridos cliente para solicitudes/mensajes (hito M3; ver [roadmap](../10-roadmap-backlog.md)).
- Módulo(s) owner: `messaging` y `request`; `request` consume contratos de cliente, staff, ficheros y mensajería.

### MSG-001 — messaging: modelo de conversaciones y mensajes con migraciones
- Fase / épica / módulo owner: F3 / MSG / messaging
- Tipo: feat
- Depende de: FND-006, FIL-001
- Descripción: Cubre: RF-MSG-07; crea conversación/mensaje con contexto CONTACT o REQUEST y estado de bandeja independiente.
- Ficheros/paquetes a crear o modificar (aproximado): `messaging.{api,domain,application,infrastructure}`, `messaging.conversation`, `messaging.message`.
- Impacto modular: tablas owner `messaging`; referencia a cliente/request por IDs sin FK/JPA cruzadas; publica API de conversación. Depende de `shared`, `files::api` y `audit::api`. MSG-002 comprueba ownership mediante el SPI mínimo `shared.spi.ClientAccountOwnership`, implementado por `customer` sin crear una dependencia Java `messaging→customer`. `notification` depende de `messaging::api` para consumir eventos; `messaging` no depende de `notification`, `request` ni `identity`.
- UI: n/a (modelo; bandeja en F4).
- Seguridad: autor, timestamp y contexto obligatorios; mensajes y adjuntos privados por defecto.
- Criterios de aceptación (Given/When/Then):
  - Given mensaje nuevo de contacto o solicitud When se persiste Then conserva contexto, autor y orden UTC sin duplicar agregado.
  - Given contexto desconocido o mensaje sin conversación When valida dominio Then rechaza sin guardar ni adjuntar a otro cliente.
- Tests: TC-MSG-001: UT (agregado/orden), IT de persistencia con H2 y revisión estática de migraciones/índices, MT (API publicada). No se requiere ejecutar migraciones SQL Server.
- Documentación a actualizar: DIARIO + INVENTARIO; modelo ER de messaging
- Estimación: M (≤ 1 d)
- Riesgos: conversación no debe convertirse en solicitud automáticamente; conservar distinción D07.
- Estado local 2026-11-19: PARCIAL. Modelo, API Java, migración y persistencia H2 implementados; 2 tests H2 y compilación de todas las fuentes pasan. La migración no se ha ejecutado en SQL Server. La entrada pública de contacto se mantiene en MSG-003/F4.

### MSG-002 — Visibilidad PUBLIC/INTERNAL, API de conversación y eventos
- Fase / épica / módulo owner: F3 / MSG / messaging
- Tipo: feat
- Depende de: MSG-001, AUD-001
- Descripción: Cubre: RF-MSG-07, RF-MSG-09, RNF-SEC-01; separa mensajes públicos e internos y publica eventos con IDs solamente.
- Ficheros/paquetes a crear o modificar (aproximado): `messaging.api.ConversationApi`, `MessagePosted/MessageReceived`, policies y queries autorizadas.
- Impacto modular: `request` consume `messaging::api`; `notification` consume los eventos de `messaging::api`; `messaging` no importa `request` ni `identity`. Las comprobaciones de propiedad usan `shared.spi.ClientAccountOwnership`; el adapter owner de `customer` accede a su propio repositorio. El DAG no tiene ciclos.
- UI: contratos para hilo de cliente/empleado; notas internas solo en vista de empleado autorizada.
- Seguridad: filtrar visibilidad en query, no solo plantilla; comprobar propietario/rol y auditar lectura/cambio.
- Criterios de aceptación (Given/When/Then):
  - Given CLIENT dueño When solicita conversación Then recibe PUBLIC y eventos sin PII/tokens.
  - Given mensaje INTERNAL When cliente consulta endpoint/fragmento directo Then nunca aparece y queda registrado test negativo.
- Tests: TC-MSG-002: UT (política), MT (eventos/API), WT (vista por rol), SEC (IDOR/notas internas).
- Documentación a actualizar: DIARIO + INVENTARIO; matriz 07
- Estimación: M (≤ 1 d)
- Riesgos: filtro aplicado solo en UI permite fuga por endpoint; query debe exigir visibilidad.
- Estado local 2026-11-19: PARCIAL. Servicio, filtro de persistencia, permisos, auditoría y eventos solo con IDs implementados; 7 tests de servicio y 5 tests de arquitectura/modularidad pasan. Faltan rutas/vistas HTTP, prueba de vista por rol y consumidor de notificaciones; por ello no se cierra MSG-002.

### REQ-001 — request: modelo de solicitudes, categorías y asignaciones con migraciones
- Fase / épica / módulo owner: F3 / REQ / request
- Tipo: feat
- Depende de: FND-006, CUS-001, STF-001
- Descripción: Cubre: RF-REQ-01, RNF-DB-02; crea tipos CUSTOM_DEVELOPMENT/SIMBIOSPHERE_MODULE, categorías, propietario y asignaciones.
- Ficheros/paquetes a crear o modificar (aproximado): `request.{api,domain,application,infrastructure}`, esquema `request`, migraciones y entidades.
- Impacto modular: tablas request owner; referencia customer/staff por APIs/IDs sin repositorios cruzados; publica contratos/eventos. Ciclo: request→customer/staff permitido.
- UI: n/a (dominio; formularios en REQ-005/006).
- Seguridad: cliente owner requerido; referencias públicas no secuenciales; índices para tenant-free ownership queries.
- Criterios de aceptación (Given/When/Then):
  - Given solicitud con tipo/categoría válidos When se persiste Then owner es ClientAccount y asignaciones referencian staff ID.
  - Given tipo/category desconocida o owner ausente When guarda Then transacción falla sin registro huérfano.
- Tests: TC-REQ-001: UT (invariantes), IT de repositorios/índices con H2, revisión estática de migraciones y MT (API). No se requiere ejecutar Flyway en SQL Server.
- Documentación a actualizar: DIARIO + INVENTARIO; modelo ER
- Estimación: M (≤ 1 d)
- Riesgos: categorías deben ser configurables y localizadas, no enums de presentación rígidos.

### REQ-002 — Referencia legible SOL-AAAA-NNNNNN
- Fase / épica / módulo owner: F3 / REQ / request
- Tipo: feat
- Depende de: REQ-001
- Descripción: Cubre: RF-REQ-05; asigna identificador de presentación único por año sin exponer claves internas.
- Ficheros/paquetes a crear o modificar (aproximado): `request.application.PublicReferenceGenerator`, secuencia SQL por año e índice único.
- Impacto modular: tabla/sequence en esquema request, owner `request`; sin evento/API adicional. Ciclo: ninguno.
- UI: muestra `SOL-AAAA-NNNNNN` en confirmación, listado y correo.
- Seguridad: el identificador no sustituye autorización; consultas siguen filtrando por propietario.
- Criterios de aceptación (Given/When/Then):
  - Given solicitudes concurrentes del mismo año When se crean Then cada referencia cumple patrón y es única.
  - Given año nuevo When se asigna primer número Then secuencia inicia según política y no colisiona con años anteriores.
- Tests: TC-REQ-002: UT (formato/año), IT (concurrencia/índice único), SEC (consulta por owner).
- Documentación a actualizar: DIARIO + INVENTARIO; formato de referencia
- Estimación: S (≤ 0,5 d)
- Riesgos: reset anual en UTC/local ambiguo; fijar zona y año de negocio.

### REQ-003 — Máquina de estados CUSTOM_DEVELOPMENT
- Fase / épica / módulo owner: F3 / REQ / request
- Tipo: feat
- Depende de: REQ-001
- Descripción: Cubre: RF-EMP-04; implementa transiciones permitidas para desarrollo a medida y efectos/visibilidad definidos.
- Ficheros/paquetes a crear o modificar (aproximado): `request.domain.CustomDevelopmentStateMachine`, catálogo de transición y auditoría.
- Impacto modular: estado/tablas propiedad request; publica `RequestStatusChanged` con IDs; notification consume evento. Sin ciclo.
- UI: estados cliente traducidos; empleado ve estado interno/prioridad sin exponer notas.
- Seguridad: comando valida rol/permisos, estado origen, presupuesto/consentimiento y propiedad del recurso.
- Criterios de aceptación (Given/When/Then):
  - Given RECIBIDA y actor EMPLOYEE autorizado When transita a EN_ANALISIS Then se audita y publica evento con IDs.
  - Given transición no permitida desde CERRADA When se solicita Then responde 409/422 sin alterar estado.
- Tests: TC-REQ-003: UT (tabla completa), MT (evento/auditoría), SEC (rol y propietario).
- Documentación a actualizar: DIARIO + INVENTARIO; tabla transición en 03/07
- Estimación: M (≤ 1 d)
- Riesgos: traducir nombre de estado dentro del dominio; mantener códigos estables.

### REQ-004 — Máquina de estados SIMBIOSPHERE_MODULE
- Fase / épica / módulo owner: F3 / REQ / request
- Tipo: feat
- Depende de: REQ-001
- Descripción: Cubre: RF-EMP-04; implementa flujo de sugerencia RECIBIDA→EN_ESTUDIO→PLANIFICADA/DESCARTADA/DUPLICADA y publicación posterior.
- Ficheros/paquetes a crear o modificar (aproximado): `request.domain.SimbiosphereModuleStateMachine`, reason/link original y auditoría.
- Impacto modular: datos/estados request owner; relación SIMBIOSPHERE se guarda por ID, sin dependencia a módulo simbiosphere antes de F5. Sin ciclo.
- UI: cliente ve etiquetas simplificadas; empleado ve motivo de descarte y solicitud original duplicada.
- Seguridad: solo EMPLOYEE/ADMIN con permiso cambia estado; motivo obligatorio en DESCARTADA y vínculo válido en DUPLICADA.
- Criterios de aceptación (Given/When/Then):
  - Given EN_ESTUDIO When EMPLOYEE marca DUPLICADA con original existente Then ambas referencias quedan auditadas.
  - Given DESCARTADA sin motivo o transición desde estado terminal When se intenta Then rechazo sin mutación.
- Tests: TC-REQ-004: UT (transiciones/motivos), WT (visibilidad cliente), SEC (permiso/IDOR).
- Documentación a actualizar: DIARIO + INVENTARIO; tabla estados en 03/07
- Estimación: M (≤ 1 d)
- Riesgos: selector/votación pública es R3+; no exponer datos internos en este flujo.

### REQ-005 — Formulario de desarrollo a medida con borrador conservado
- Fase / épica / módulo owner: F3 / REQ / request
- Tipo: feat
- Depende de: REQ-002, REQ-003, IAM-004, UIK-008
- Descripción: Cubre: RF-REQ-02, RF-REQ-03, RF-REQ-04; captura título, descripción, objetivo, categoría, presupuesto, fecha y borrador autenticado.
- Ficheros/paquetes a crear o modificar (aproximado): `request.web.CustomDevelopmentController`, application command, `templates/request/new-custom.html`, bundles.
- Impacto modular: request consume `customer.api`, `files.api` después; publica ID. Sin repositorios compartidos ni ciclos.
- UI: `/cuenta/solicitudes/nueva`; borrador al login, validación accesible, ES/EN, tema y responsive.
- Seguridad: cliente con email verificado; CSRF; no descargar URLs de referencia (SSRF); validar longitudes y presupuesto.
- Criterios de aceptación (Given/When/Then):
  - Given cliente verificado When envía formulario válido Then crea CUSTOM_DEVELOPMENT, referencia SOL y confirmación.
  - Given visitante o URL de referencia privada When envía Then conserva borrador tras login y servidor no realiza petición a la URL.
- Tests: TC-REQ-005: WT (form/borrador/CSRF), IT (creación), SEC (SSRF/propietario), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; README «Solicitudes»
- Estimación: M (≤ 1 d)
- Riesgos: session draft puede perderse; guardar temporalmente con owner y TTL sin adjuntos públicos.

### REQ-006 — Formulario de sugerencia de módulo SimbioSphere
- Fase / épica / módulo owner: F3 / REQ / request
- Tipo: feat
- Depende de: REQ-004, REQ-005
- Descripción: Cubre: RF-REQ-02, RF-REQ-03; captura impacto y módulo relacionado en solicitud SIMBIOSPHERE_MODULE.
- Ficheros/paquetes a crear o modificar (aproximado): `request.web.ModuleSuggestionController`, categoría/localización y templates.
- Impacto modular: request usa tipos de negocio/módulo acordados por contrato documental; `simbiosphere` se enlaza en SIM-014. Sin ciclo en F3.
- UI: `/cuenta/solicitudes/sugerencia`; campos contextualizados y negocio opcional hasta F5.
- Seguridad: sesión/email verificado, CSRF, validar enum/lista; no replicar reglas de provisión de módulos DEMO.
- Criterios de aceptación (Given/When/Then):
  - Given cliente verificado When envía sugerencia con módulo e impacto Then crea tipo SIMBIOSPHERE_MODULE con estado RECIBIDA.
  - Given valor de módulo no permitido o cliente no propietario When envía Then rechaza sin crear ni asociar negocio por email.
- Tests: TC-REQ-006: WT (form/estado), UT (lista permitida), SEC (owner/valor), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; 05 contrato de catálogo
- Estimación: M (≤ 1 d)
- Riesgos: catálogo de BusinessModule debe cotejarse con DEMO `shared/api/BusinessModule.java:9-23,25-30,48-55,63-66,82-87`.

### REQ-007 — Adjuntos de solicitud con límites y descarga autorizada
- Fase / épica / módulo owner: F3 / REQ / request
- Tipo: feat
- Depende de: REQ-005, FIL-003
- Descripción: Cubre: RF-REQ-03, RNF-SEC-06; asocia adjuntos limpios a solicitud con límites y autorización en descarga.
- Ficheros/paquetes a crear o modificar (aproximado): `request.request_attachment`, `request.application.AttachmentService`, endpoint de descarga.
- Impacto modular: tabla relación owner `request`; bytes/metadatos los posee `files`; consume `files.api`. Ciclo: request→files permitido.
- UI: adjuntar en formulario/detalle, tamaño/tipo visibles y error accesible.
- Seguridad: ClamAV CLEAN, allowlist, límite de cantidad/tamaño; validar owner en cada GET, `attachment` y `nosniff`.
- Criterios de aceptación (Given/When/Then):
  - Given solicitud propia y fichero limpio When se adjunta y descarga Then streaming autorizado entrega bytes sin revelar path.
  - Given fichero en cuarentena o solicitud ajena When solicita descarga Then responde 404/403 y no filtra nombre ni contenido.
- Tests: TC-REQ-007: UT (límites), IT (link file/request), SEC (IDOR/cuarentena), WT.
- Documentación a actualizar: DIARIO + INVENTARIO; tabla de adjuntos
- Estimación: M (≤ 1 d)
- Riesgos: borrar solicitud puede dejar fichero huérfano; integrar política de retención/limpieza.

### REQ-008 — Mis solicitudes: listado con filtros
- Fase / épica / módulo owner: F3 / REQ / request
- Tipo: feat
- Depende de: REQ-005
- Descripción: Cubre: RF-REQ-06; lista solicitudes propias con filtros de tipo, estado, referencia y fecha paginados en servidor.
- Ficheros/paquetes a crear o modificar (aproximado): `request.web.MyRequestsController`, query service, `templates/request/list.html`, bundles.
- Impacto modular: consulta solo tablas request y `customer.api` para owner; sin joins JPA cruzados ni eventos. Ciclo: permitido.
- UI: `/cuenta/solicitudes`, tabla responsive, estado público traducido, filtros conservados al paginar.
- Seguridad: actor CLIENT ligado a cuenta; no aceptar `ownerId` del browser; default-deny.
- Criterios de aceptación (Given/When/Then):
  - Given titular con solicitudes When filtra/pagina Then solo recibe sus filas en orden estable y con referencia legible.
  - Given parámetro de otro owner o ID manipulado When consulta Then no obtiene registros ajenos ni cuenta total filtrada.
- Tests: TC-REQ-008: WT (filtros/paginación), IT (query/index), SEC (IDOR/listado), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; README «Mis solicitudes»
- Estimación: M (≤ 1 d)
- Riesgos: paginación inestable omite filas; ordenar con tie-breaker de ID interno.

### REQ-009 — Mis solicitudes: detalle, historial público y acciones (aportar información, cancelar)
- Fase / épica / módulo owner: F3 / REQ / request
- Tipo: feat
- Depende de: REQ-008, MSG-002
- Descripción: Cubre: RF-REQ-06, RF-REQ-07; muestra detalle/historial público, permite aportar información o cancelar en estados permitidos.
- Ficheros/paquetes a crear o modificar (aproximado): `request.web.RequestDetailController`, `RequestClientActions`, timeline y bundles.
- Impacto modular: consume `messaging.api` para PUBLIC; request posee estados; no lee notas internas. Sin ciclo inverso.
- UI: `/cuenta/solicitudes/{publicId}`; estado simplificado, mensajes, presupuesto y acciones visibles por estado.
- Seguridad: verificar owner en cada consulta/mutación, CSRF, validar transición y ocultar INTERNAL desde query.
- Criterios de aceptación (Given/When/Then):
  - Given cliente dueño y solicitud cancelable When añade información o cancela Then historial público/auditoría actualiza una vez.
  - Given cliente ajeno o nota INTERNAL When carga detalle Then responde 404/403 y nunca devuelve texto interno.
- Tests: TC-REQ-009: WT (detalle/acciones), SEC (IDOR/visibilidad), MT (evento/auditoría), A11Y.
- Documentación a actualizar: DIARIO + INVENTARIO; acciones cliente en 07
- Estimación: M (≤ 1 d)
- Riesgos: aceptar/rechazar presupuesto queda en QUO-003; no duplicar transición en este controlador.

### REQ-010 — Notificaciones de solicitud (nueva, estado, presupuesto)
- Fase / épica / módulo owner: F3 / REQ / request
- Tipo: feat
- Depende de: EMP-005, QUO-002, NTF-003, IAM-013
- Descripción: Cubre: RF-NOT-01; publica notificaciones de alta, cambio de estado y emisión de presupuesto usando outbox.
- Ficheros/paquetes a crear o modificar (aproximado): eventos `RequestCreated/StatusChanged/QuoteSent`, handlers y plantillas base notification.
- Impacto modular: `request` publica eventos con IDs; `notification.api` resuelve destinatarios por RecipientDirectory; sin PII en payload. Ciclo: ninguno.
- UI: email por locale; enlaces llevan a detalle autenticado y sin tokens en URL.
- Seguridad: autorizar destinatario, evitar enumeración; logs solo correlation/event ID.
- Criterios de aceptación (Given/When/Then):
  - Given solicitud nueva o presupuesto enviado When transacción confirma Then outbox crea una notificación en idioma del destinatario.
  - Given evento duplicado o actor sin permiso When handler procesa Then idempotencia evita duplicado y no revela datos a terceros.
- Tests: TC-REQ-010: MT (event/outbox), IT (duplicado/retry), SEC (recipient/PII), WT.
- Documentación a actualizar: DIARIO + INVENTARIO; eventos en 03
- Estimación: M (≤ 1 d)
- Riesgos: evento publicado antes de commit duplica correo; usar EPR/outbox y tests post-commit.

### REQ-011 — Tests IDOR y de notas internas de solicitudes
- Fase / épica / módulo owner: F3 / REQ / request
- Tipo: test
- Depende de: REQ-009, EMP-006
- Descripción: Cubre: RNF-SEC-01, RF-EMP-03; prueba aislamiento de propietarios y separación PUBLIC/INTERNAL en todos los endpoints.
- Ficheros/paquetes a crear o modificar (aproximado): `request` security integration tests, fixtures de dos clientes y empleado.
- Impacto modular: tests ejercitan `request`, `messaging.api` y reglas de autorización; no agregan dependencias de producción. Ciclo: ninguno.
- UI: recorrido cliente/empleado en vista de detalle y fragmentos HTMX.
- Seguridad: probar lectura, mutación, adjunto, timeline, CSV y notas internas con IDs de otra cuenta.
- Criterios de aceptación (Given/When/Then):
  - Given CLIENT A solicita recursos de CLIENT B When corre suite SEC Then recibe 403/404 y ningún campo de B.
  - Given nota INTERNAL When CLIENT consulta lista, detalle, HTMX y API Then respuesta omite cuerpo; EMPLOYEE autorizado sí lo recibe.
- Tests: TC-REQ-011: SEC (matriz IDOR), WT (rutas/HTMX) e IT de aislamiento entre clientes con H2. No se requieren pruebas SQL Server/Testcontainers.
- Documentación a actualizar: DIARIO + INVENTARIO; resultados en 08
- Estimación: M (≤ 1 d)
- Riesgos: probar solo controlador deja fugas en servicio/query; cubrir todas las capas públicas.
