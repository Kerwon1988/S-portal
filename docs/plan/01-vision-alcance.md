# 01 — Visión y alcance

## Visión
S-portal es un portal corporativo bilingüe con CMS ligero y un área privada para clientes y trabajadores. Ayuda a negocios a reducir gestión con información, solicitudes y comunicación centralizadas; conecta alta de cliente con SimbioSphere mediante una integración M2M segura. Los agentes de IA son una visión futura, no una capacidad MVP.

Nombre empresarial: **Simbio** (confirmado por el owner; “Symbio” es incorrecto). Nombre del SaaS: **SimbioSphere** (D01). La razón social, dominios y datos legales siguen pendientes; se parametrizan y no se inventan. Fuente provisional de imágenes de marca: `C:\Users\x455035\1Git\AprendiendoIA\Symbio\symbio-frontend\public\resources\img`; revisar permiso/licencia de cada asset antes de incluirlo.

## Objetivos verificables
| Objetivo | Indicador de salida |
|---|---|
| Web pública utilizable en ES/EN y todos los tamaños | Páginas públicas publicadas por locale; Lighthouse ≥90 móvil, LCP <2,5 s, CLS <0,1, INP <200 ms |
| Tema accesible y consistente | Claro/oscuro con tokens; contraste AA automatizado; teclado/axe en flujos críticos |
| Administración editorial segura | ADMIN con `CONTENT_EDIT` edita/publica contenido; sanitización allowlist y restauración snapshot |
| Identidad y gestión | Registro/verificación, invitaciones, MFA ADMIN, búsqueda/paginación y acciones auditadas |
| Solicitudes y operación | Recorrido cliente→solicitud→asignación→presupuesto→aceptación probado; aislamiento owner/notes INTERNAL |
| Mensajería | Contacto distinto de solicitud; bandeja interna y respuesta; adjuntos/audios validados |
| Integración conjunta | Saga/outbox, reintentos idempotentes y estados visibles contra API DEMO y stubs; ninguna llamada remota dentro de transacción |
| Gobernanza sostenible | Chequeos Spring Modulith/ArchUnit sin ciclos, cobertura ≥80 % domain/application, paridad i18n y docs enlazadas |
| Registro operativo de errores | Inventario deduplicado de backend, frontend y BD; funciona con SQL Server caído, payload saneado y visor/resolución ADMIN-only |

Los indicadores se verifican en [08-calidad-testing.md](08-calidad-testing.md), tareas y gates; no constituyen compromiso de calendario.

## Actores
| Actor | Necesidad / autorización |
|---|---|
| Visitante anónimo | Navegar sitio, cambiar locale/tema, contactar y empezar registro |
| Cliente (`CLIENT`, titular `OWNER`) | Perfil propio, solicitudes de su ClientAccount, mensajes PUBLIC, presupuesto y vínculo SimbioSphere propio |
| Empleado (`EMPLOYEE`) | Bandeja y solicitudes de trabajo permitidas, asignación/estado/presupuesto según permisos |
| Administrador (`ADMIN`) | Administración del sistema y usuarios, CMS, auditoría, aprovisionamiento y trabajadores; también empleado |
| SimbioSphere (principal M2M) | API de plataforma limitada a provisioning read/write; nunca sesión de usuario |
| Agente IA futuro | R3+; solo borrador revisado por humano, sin permisos/ejecución MVP |

## Alcance MVP (F0–F6)
- F0: build, módulos, CI, SQL Server, seguridad, tema/i18n/layouts/menú y gobernanza viva.
- F1: web pública ES/EN, CMS, legales/SEO y núcleo identity requerido para acceso editorial.
- F2: registro/verificación, cuentas CLIENT, invitaciones, MFA ADMIN, gestión de clientes/trabajadores y derechos RGPD.
- F3: solicitudes de desarrollo/sugerencia, estados, mensajería núcleo, asignación, presupuesto y portal empleado.
- F4: contacto, adjuntos/audio, bandeja, enlace mágico, notificaciones de dominio y preferencias.
- F5: saga M2M, panel SimbioSphere, activación just-in-time; pista DEMO externa corre en paralelo.
- F6: hardening, accesibilidad, rendimiento, operación, procedimiento de backup/restauración documentado y Go/No-Go.

MVP no significa pago/cobro. El plan `DEMO` gratuito de SimbioSphere no permite anunciar precios por módulo/sede ni checkout: evidencia `DEMO-orders-thymeleaf/README.md:11-14,58-66`. No se promete selector de módulos; DEMO decide defaults por tipo una vez implementado DEMO-08.

## Releases posteriores
- **R2:** kanban; email entrante; implementación de vínculo existente con prueba de posesión; lifecycle DEMO; selector módulos/catálogo; notificaciones in-app; diff CMS; MFA EMPLOYEE obligatorio si se decide; almacenamiento objeto/Blob.
- **R3+:** SSO OIDC, WhatsApp Business API, agentes IA, pagos/facturación, votación pública/roadmap, PWA y más idiomas.
- No se construyen módulos vacíos ni se presentan estos elementos como existentes.

## Fuera de alcance explícito
- Migrar/reemplazar la web PHP `symbiosi-main` (D02).
- Leer/escribir BD DEMO, scraping de `/onboarding` o reutilizar CSRF; compartir credenciales/login.
- Cobro de servicios o suscripción en MVP; envío automático de IA; WhatsApp Business API; email entrante.
- Calendario de ejecución o estimación de fecha de salida: el roadmap contiene esfuerzo y camino crítico, no compromisos de calendario.

## Supuestos y dependencias
- Se mantiene stack decidido D09/D10: Java 21, Boot estable con soporte OSS, Spring MVC, Thymeleaf, HTMX, Alpine CSP, Tailwind local, Spring Security/Data JPA/Flyway/Modulith/ArchUnit/Playwright y SQL Server 2022. Las pruebas automatizadas de persistencia usan H2; no se exige Testcontainers ni validación MSSQL.
- Boot 4.1.2/Modulith 2.1.2 y versiones front son datos verificados al 2026-11-10; compatibilidad se cierra con FND-001 antes de build.
- API de provisioning DEMO no existe hoy; contrato/implementación de DEMO son dependencia externa y se planifican por separado en [05](05-integracion-simbiosphere.md) y [tareas/DEMO-backlog.md](tareas/DEMO-backlog.md).
- Dominio, datos legales, proveedor email, WhatsApp, plataforma, edición SQL Server, CAPTCHA, licencias y aprobación de assets siguen las respuestas/pendientes de [13](13-preguntas-abiertas.md).
- Retención/SLA en documentos son propuestas hasta aprobación legal/operación.

## Decisiones cerradas
D01–D12 se mantienen sin reabrir: producto SimbioSphere; portal nuevo; REST M2M + logins separados; defaults de módulo por DEMO; ClientAccount/OWNER; aislamiento de solicitudes y notas internas; bandeja+email+`wa.me`; CMS ES/EN MVP; SQL Server 2022; stack Java 21/Boot estable; README/INVENTARIO/DIARIO; skill propia y gates. Un problema grave se registra como riesgo y alternativa, no como reapertura tácita.
