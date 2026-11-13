# 03 — Arquitectura

## Decisiones de diseño
S-portal es un monolito modular Spring Boot/Thymeleaf, un desplegable y una BD SQL Server. `com.simbio.core` es el `groupId` y paquete raíz confirmados por el owner; `artifactId` provisional: `portal` (validar el build en FND-002). La integración de SimbioSphere es HTTP M2M; no hay acceso a su BD, scraping ni transacción distribuida. Arquitectura y contratos son planificados, no implementados.

## C4 — Contexto
```mermaid
C4Context
  title Contexto de S-portal
  Person(visitor, "Visitante", "Consulta sitio público y puede contactar")
  Person(client, "Cliente", "Gestiona perfil, solicitudes y conversación propia")
  Person(staff, "Empleado/Admin", "Gestiona CMS, clientes, solicitudes y bandeja")
  System(portal, "S-portal", "Monolito modular server-side")
  System_Ext(demo, "SimbioSphere", "SaaS multitenant; integración de aprovisionamiento M2M")
  System_Ext(mail, "SMTP", "Correo transaccional; proveedor por decidir")
  Rel(visitor, portal, "HTTPS: páginas públicas")
  Rel(client, portal, "HTTPS: área de cliente")
  Rel(staff, portal, "HTTPS: backoffice")
  Rel(portal, demo, "HTTPS + HMAC: API platform versionada")
  Rel(portal, mail, "SMTP: avisos asíncronos")
```

## C4 — Contenedores
```mermaid
C4Container
  title Contenedores lógicos
  Person(user, "Usuario", "Visitante, cliente o empleado")
  System_Boundary(sportal, "S-portal") {
    Container(app, "Aplicación web", "Java 21 / Spring Boot estable (fijado en FND-001)", "MVC, Thymeleaf, HTMX, Alpine CSP")
    ContainerDb(db, "SQL Server 2022", "SQL Server", "Esquemas por módulo; EPR de Modulith en infra")
    Container(files, "Volumen de ficheros", "Disco local fuera del webroot", "MVP; ClamAV y cuarentena")
  }
  System_Ext(demo, "SimbioSphere", "API platform M2M")
  System_Ext(smtp, "SMTP", "Proveedor por decidir; Mailpit en dev")
  Rel(user, app, "HTTPS")
  Rel(app, db, "JPA/Flyway; transacciones locales")
  Rel(app, files, "Puerto FileStorage; streaming autorizado")
  Rel(app, demo, "HTTPS/HMAC; timeouts y reintentos")
  Rel(app, smtp, "Outbox + SMTP")
```

## C4 — Componentes principales
```mermaid
C4Component
  title Componentes principales del monolito
  Container_Boundary(app, "S-portal") {
    Component(config, "config", "Web/Security/i18n/theme", "Compone configuración; no importa negocio")
    Component(errorInventory, "config.errortracking", "ErrorInventoryService", "Centraliza backend/frontend/BD; JSON fuera de SQL")
    Component(identity, "identity", "Usuarios y autenticación", "Roles, sesiones, tokens y MFA")
    Component(content, "content", "Sitio y CMS", "Páginas, medios referenciados, SEO")
    Component(customer, "customer", "Cuenta cliente", "ClientAccount y miembro OWNER")
    Component(staff, "staff", "Trabajadores", "Perfil, especialidades y estado")
    Component(request, "request", "Solicitudes", "Estados, asignaciones y presupuestos")
    Component(messaging, "messaging", "Mensajería", "Contacto y conversaciones")
    Component(notification, "notification", "Notificaciones", "Outbox, email y preferencias")
    Component(simbio, "simbiosphere", "Integración", "Saga y capa anticorrupción")
    Component(shared, "shared", "Kernel", "Value objects y SPIs mínimos")
  }
  Rel(content, shared, "Contratos comunes")
  Rel(request, content, "Copy publicado de condiciones por API")
  Rel(request, customer, "API publicada")
  Rel(request, staff, "API publicada")
  Rel(request, messaging, "API publicada")
  Rel(request, simbio, "API publicada desde F5")
  Rel(messaging, notification, "API/eventos")
  Rel(simbio, customer, "API publicada")
  Rel(identity, notification, "RecipientDirectory por SPI shared")
```

Nota: la línea `content→notification` no es una dependencia Java; la composición se realiza por contrato y no debe implementarse como llamada directa. `config` compone SPI de navegación y seguridad sin importar módulos de negocio.

## Catálogo de módulos
Las flechas en la columna «Depende de» indican dependencias Java permitidas hacia interfaces `api` o SPI, nunca repositorios/entidades ajenas. Cada módulo publica `api` como named interface.

| Módulo | Responsabilidad / API publicada | Eventos | Tablas owner (propuesta) | Depende de |
|---|---|---|---|---|
| `audit` | Registro inmutable; `AuditRecorder` | ninguno | `audit.audit_entry` | `shared` |
| `files` | Puerto `FileStorage`, metadata y escaneo | ninguno | `files.file_metadata`, `files.file_variant` | `shared` |
| `notification` | `EmailSender`, envío/plantillas | consumidores de eventos; no publica PII | `notification.outbox_message`, `notification.preference` | `shared` |
| `content` | CMS, render, SEO y páginas públicas | `PagePublished` (IDs) | `content.page`, `page_translation`, `content_block`, `page_version`, `team_member`, `legal_document_version`, `slug_redirect` | `shared`, `files`, `audit` |
| `legal` | Consentimientos, derechos, retención | `DataSubjectErasureRequested` | `legal.consent_record`, `data_subject_request`, `retention_policy` | `shared`, `content`, `audit` |
| `identity` | Usuarios, autenticación, permisos, tokens, MFA | `ClientRegistered`, `EmailVerified`, `InvitationAccepted` (IDs) | `identity.user_account`, `role`, `permission`, `one_time_token`, `mfa_factor` | `shared`, `legal`, `notification`, `audit` |
| `customer` | ClientAccount, miembros y registro | `ClientRegistered` (IDs) | `customer.client_account`, `account_member` | `shared`, `identity`, `legal`, `audit` |
| `staff` | Perfil, especialidades y estado de trabajador | consume `InvitationAccepted` | `staff.staff_profile`, `staff_specialty` | `shared`, `identity`, `audit` |
| `simbiosphere` | Saga, API M2M y vínculos externos | `ProvisioningStateChanged` (IDs) | `simbiosphere.provisioning`, `account_link`, `linked_business` | `shared`, `customer`, `identity`, `notification`, `audit` |
| `messaging` | Conversaciones CONTACT/REQUEST y visibilidad | `MessagePosted`, `MessageReceived` (IDs) | `messaging.conversation`, `message`, `message_attachment`, `thread_access_token` | `shared`, `files`, `customer`, `notification`, `audit` |
| `request` | Solicitudes, asignación, estados y presupuestos | `RequestStatusChanged` (IDs) | `request.request`, `assignment`, `quote`, `quote_version`, `request_attachment`, `status_history` | `shared`, `content` (lectura publicada), `customer`, `staff`, `messaging`, `files`, `simbiosphere`, `notification`, `audit` |
| `shared` | Kernel mínimo y SPIs (`CurrentUser`, `RecipientDirectory`, `PersonalDataContributor`, `NavContribution`, `ErrorReporter`) | n/a | ninguna | ninguno; OPEN |
| `config` | Web, security, i18n, tema, rate limit, observabilidad, `ErrorInventoryService` y visor ADMIN | n/a | ninguna; inventario JSON externo a SQL | `shared`; OPEN, sin dependencia de negocio |

`site` se integra en `content`; presupuestos son parte del agregado `request`; `staff` y `customer` conservan owners distintos. No se crean módulos vacíos.

## DAG de dependencias
```mermaid
flowchart LR
  audit --> shared
  files --> shared
  request --> content
  notification --> shared
  content --> shared
  content --> files
  content --> audit
  legal --> shared
  legal --> content
  legal --> audit
  identity --> shared
  identity --> legal
  identity --> notification
  identity --> audit
  customer --> shared
  customer --> identity
  customer --> legal
  customer --> audit
  staff --> shared
  staff --> identity
  staff --> audit
  simbiosphere --> shared
  simbiosphere --> customer
  simbiosphere --> identity
  simbiosphere --> notification
  simbiosphere --> audit
  messaging --> shared
  messaging --> files
  messaging --> customer
  messaging --> notification
  messaging --> audit
  request --> shared
  request --> customer
  request --> staff
  request --> messaging
  request --> files
  request --> simbiosphere
  request --> notification
  request --> audit
```
El grafo es acíclico. `audit`, `files` y `notification` son hojas de negocio que solo dependen de `shared`. `config` usa `shared` y no aparece como dependencia de negocio.

## Capas, paquetes y datos
- Cada módulo vive bajo `com.simbio.core.<module>`; raíz con `package-info.java` y `@ApplicationModule(allowedDependencies=...)`.
- `api`: contratos/eventos/DTO públicos y `@NamedInterface("api")`; no depende de `web` ni `infrastructure`.
- `application`: casos de uso, transacciones locales y puertos; `domain`: entidades JPA, invariantes y value objects; `infrastructure`: adaptadores; `web`: MVC/Thymeleaf/HTMX, sin repositorios.
- Reglas ArchUnit: `web→application→domain`; infraestructura implementa puertos; controladores no acceden a repositorios; cada tabla tiene un owner y esquema SQL propio.
- Comunicación síncrona solo por `api`; comunicación asíncrona vía Spring Modulith Event Publication Registry (EPR). Eventos contienen IDs, no PII, credenciales ni tokens. Esquema EPR: `infra`.

## Composición frontend
- Plantillas de módulo en `templates/<module>/`; fragmentos compartidos en `templates/fragments/`; layouts `public`, `account`, `backoffice`.
- Composición inter-módulo por fragmentos HTMX URL con permiso propio en cada endpoint; nunca por import Java de plantillas/repositorios. Menú por `shared.spi.NavContribution` compuesto en `config`.
- Tailwind CLI y assets locales con huella; HTMX y Alpine CSP locales; JavaScript mínimo por componente. Tokens CSS semánticos, sin colores directos en plantillas.
- Bundles `i18n/<module>/messages_{es,en}.properties`; páginas públicas `/{lang}/...`, áreas privadas `/cuenta/...` y `/gestion/...`. CSP nonce, CSRF HTMX, tema claro/oscuro y mejora progresiva son transversales.

## Límites
- `config` no es módulo de negocio: seguridad, web, i18n, tema, observabilidad y rate limiting.
- `config.errortracking.ErrorInventoryService` centraliza errores recibidos por `shared.spi.ErrorReporter`, handlers, aspecto DB y endpoint frontend. Persistir fuera de SQL permite registrar indisponibilidad de la BD; `/error` es un adaptador de despacho, no el registrador central.
- El inventario operativo `logs-error/error-inventory.json` queda fuera del webroot y no pertenece a ningún esquema SQL. Persistencia multi-réplica, volumen y retención deben confirmarse antes de escalar.
- Actuator se recomienda en management port separado `8091`, accesible solo por red/probes internas y no publicado; exponer únicamente health/readiness y métricas autorizadas. Confirmar configuración Boot 4/plataforma en FND-001/FND-010.
- No hay llamada HTTP a DEMO dentro de transacción local. La saga y estados se detallan en [05-integracion-simbiosphere.md](05-integracion-simbiosphere.md).
- Contratos UI, permisos, tablas y módulos detectados se reflejan en [04-modelo-datos.md](04-modelo-datos.md), [06-ux-ui.md](06-ux-ui.md) y el inventario planificado en [12-gobernanza-modular.md](12-gobernanza-modular.md).
