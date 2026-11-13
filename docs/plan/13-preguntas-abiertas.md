# 13 — Preguntas abiertas

Las dudas se revisaron una a una con el owner el 2026-11-11. Las respuestas seleccionadas son la referencia vigente; cualquier dato marcado pendiente sigue requiriendo confirmación antes de publicar, desplegar o borrar datos. Un cambio de D01–D12 no se presume.
Las tablas de preguntas siguientes conservan opciones de la planificación inicial; si una fila contradice el registro de respuestas, prevalece el registro superior.

## Registro de respuestas del owner
Las recomendaciones aceptadas se consideran decisiones de planificación, no aprobaciones legales/de producción. **Acordado** indica que el criterio está resuelto; **Pendiente** identifica un dato, aprobación o validación externa aún necesaria.

| # | Respuesta registrada | Estado |
|---:|---|---|
| 1 | El nombre empresarial correcto es **Simbio**; “Symbio” es incorrecto. Razón social y datos fiscales oficiales no se inventan. | Acordado; datos legales pendientes |
| 2 | Una URL base configurable por entorno; no publicar dominios provisionales. | Acordado; dominios pendientes |
| 3 | SimbioSphere local: `http://localhost:8081/`; PRE: `https://orders.simbio.info`; producción aún sin confirmar. | Parcial; producción pendiente |
| 4 | `com.simbio.core` es `groupId` y paquete raíz Java; `artifactId` provisional `portal`. | Acordado; validar en FND-002 |
| 5 | Proponer Docker, reverse proxy TLS y secretos externos; plataforma concreta se confirma antes del despliegue. | Propuesta aceptada; plataforma pendiente |
| 6 | SQL Server 2022 como motor; comparar edición/licencia y soporte TDE antes de decidir. | Motor acordado; edición/TDE pendientes |
| 7 | Fijar en CI una CU o digest verificable, no una etiqueta flotante. | Acordado |
| 8 | CI aprobada por la organización, SBOM y SCA; CVSS ≥7 es umbral inicial sujeto a política. | Criterio acordado; herramienta/política pendientes |
| 9 | No fijar ni prometer RPO/RTO hasta acordarlos y probar restauraciones. | Pendiente de Operaciones |
| 10 | Actuator en 8091, interno/no publicado y con endpoints mínimos. | Acordado |
| 11 | SMTP desacoplado, Mailpit local; proveedor/región/dominio pendientes; preparar SPF, DKIM y DMARC. | Criterio acordado; proveedor pendiente |
| 12 | Click-to-chat configurable en MVP; número pendiente; sin WhatsApp Business API en MVP. | Parcial; número pendiente |
| 13 | ALTCHA autoalojado, honeypot y rate limit; revisar privacidad antes de añadir terceros. | Acordado como propuesta |
| 14 | No recibir email en MVP; bandeja interna y correo saliente; reply-by-email posterior. | Acordado |
| 15 | Tomar assets provisionalmente de `C:\Users\x455035\1Git\AprendiendoIA\Symbio\symbio-frontend\public\resources\img`. | Fuente indicada; aprobación/licencias por asset pendientes |
| 16 | Mostrar solo perfiles aprobados; ocultar bloque del equipo si no hay contenido validado. | Acordado |
| 17 | Se retiran las fuentes web personalizadas de F0; se usa la pila del sistema, sin archivos tipográficos ni CDN. | Acordado por el owner el 2026-11-12 |
| 18 | Textos legales en borrador hasta aprobación legal. | Acordado |
| 19 | No enviar ni persistir la contraseña de SimbioSphere; activación just-in-time con token de un uso. | Acordado |
| 20 | MFA opt-in para EMPLOYEE y obligatorio para ADMIN; reevaluar EMPLOYEE antes de R2. | Acordado |
| 21 | Mantener retenciones como propuesta y no purgar irreversiblemente sin aprobación legal. | Pendiente de Legal |
| 22 | Un día hábil como valor configurable interno; no publicarlo como SLA todavía. | Criterio acordado; validación operativa pendiente |
| 23 | Confirmar países; pedir NIF/CIF según flujo y validar sintaxis local, sin consulta externa por defecto. | Parcial; países pendientes |
| 24 | EUR provisional; IVA y condiciones requieren aprobación comercial/legal antes de uso vinculante. | Parcial; aprobación pendiente |
| 25 | Sin campana in-app en MVP; email ahora y campana en R2. | Acordado |
| 26 | Crear tarea documental explícita para RF-DOC-04 y actualizar trazabilidad/conteos. | Acordado; tarea GOV-006 añadida |
| 27 | Inventario JSON privado fuera de webroot/SQL; logs estructurados/APM complementarios. | Acordado |
| 28 | Límites configurables, deduplicación, alertas antes de purgar no resueltos, backups cifrados y sin datos crudos por defecto. | Criterio acordado; valores pendientes de medición |
| 29 | Single-writer/una instancia para el inventario JSON en MVP; coordinar storage antes de escalar. | Acordado |
| 30 | Captura amplia de errores de backend, frontend y BD, con stack/cadena causal y contexto técnico saneado, clasificados y limitados; no guardar secretos ni PII en bruto. | Acordado |
| 31 | Contexto frontend técnico, saneado y acotado; excluir formularios, DOM, body, cookies, tokens y headers. | Acordado |
| 32 | Acceso ADMIN con permiso dedicado y auditoría; CLIENT/EMPLOYEE sin acceso. | Acordado |
| 33 | Elegir versión estable y soportada de Spring Boot al iniciar F0; no depender de RC. | Acordado como criterio |
| 34 | Spike y matriz de compatibilidad antes de fijar dependencias. | Acordado |
| 35 | SBOM y revisión/aprobación previa de licencias de dependencias y assets incluidos. La tipografía del portal usa fuentes del sistema. | Acordado |
| 36 | Ciclo de vida extendido de SimbioSphere fuera de MVP, en R2; diseñar idempotencia/compensación. | Acordado |
| 37 | Sin pagos en MVP; añadir tarea CMS para el copy de gratuidad/condiciones y publicarlo solo aprobado. | Acordado; CMS-013 añadida |
| 38 | Mostrar solo `modulesApplied`; no replicar reglas/catálogo; selector completo en R2. | Acordado |
| 39 | Propuesta de estados: cancelación cliente antes de `EN_DESARROLLO`, pausa por staff con motivo y cierre confirmado por cliente tras revisión. | Propuesta aceptada; aprobación de negocio pendiente |
| 40 | SSO, IA, pagos, kanban, WhatsApp API, email entrante, diff CMS, PWA e idiomas extra fuera de MVP; mantener en R2/R3+. | Acordado |
| 41 | En F0, persistir el tema en cookie/localStorage para usuarios anónimos y autenticados; diferir la persistencia en perfil hasta integrar el módulo de Identidad. | Acordado por el owner |

## Empresa, producto y URLs
| Pregunta | Opción por defecto / supuesto | Cierra |
|---|---|---|
| ¿Razón social, NIF, dirección, nombre comercial y marca definitiva? | Nombre empresarial **Simbio** (no “Symbio”); razón social/datos legales oficiales no se inventan. Mantener campos parametrizados y textos legales en borrador. | CMS-010, CMS-012, LGL-001, HRD-008 |
| ¿Dominio público y dominios de cada entorno? | Configurar `PORTAL_BASE_URL` por entorno; dominios del portal pendientes. No publicar canonical/JSON-LD con host inventado. | SEO-001/002, FND-007 |
| ¿URL de SimbioSphere en local, PRE y producción? | Local `http://localhost:8081/`; PRE `https://orders.simbio.info`; producción pendiente. Configurar por entorno y no reutilizar PRE en producción sin aprobación. | SIM-003, SIM-011, HRD-006 |
| ¿Paquete Maven/group/artifact y nombre mostrado? | `groupId` y paquete raíz `com.simbio.core`; `artifactId` provisional `portal`. Nombre empresarial Simbio. Validar build/paquete en FND-002. | FND-002, FND-007, ADR-001 |

## Proveedores y operación
| Pregunta | Opción por defecto / supuesto | Cierra |
|---|---|---|
| ¿Plataforma/orquestador de despliegue, proxy y gestor de secretos? | Docker tras reverse proxy TLS y secrets externos son propuesta; plataforma concreta se decide antes de HRD-006/010. | ADR-006, HRD-006/010 |
| ¿Edición/licencia SQL Server y necesidad de TDE? | SQL Server 2022; comparar Express/Standard/Azure SQL con fuentes oficiales. No suponer soporte TDE, límites ni precios. | FND-001, ADR-006, ADR-009, HRD-013 |
| ¿Qué tag SQL Server 2022 y EULA admite la plataforma de despliegue? | Confirmar tag/digest y EULA al preparar el despliegue; `2022-latest` puede cambiar. No es requisito para los gates de desarrollo. | ADR-006, HRD-006 |
| ¿Qué CI y análisis de dependencias se usarán? | Usar CI aprobada por la organización, generar SBOM y realizar SCA; CVSS ≥7 como umbral inicial, sujeto a política corporativa. | FND-008, HRD-001 |
| ¿RPO/RTO, horario de backup y retención? | Pendiente; no prometer continuidad hasta definir objetivos. El plan documenta restauración, sin exigir un simulacro SQL Server. | HRD-005, HRD-014 |
| ¿Puerto de management 8091 aceptado por plataforma? | Recomendación: puerto separado, interno y no publicado; Actuator limitado a health/readiness/Prometheus. | FND-010, HRD-007, 09-operacion-despliegue.md |

## Email, canales y anti-abuso
| Pregunta | Opción por defecto / supuesto | Cierra |
|---|---|---|
| ¿Proveedor SMTP, región de datos y dominio de envío? | Puerto SMTP desacoplado; proveedor EU-hosted por decidir; Mailpit en local; sin credenciales hasta elección. | ADR-007, NTF-002, HRD-009 |
| ¿Número de WhatsApp de empresa? | `wa.me` click-to-chat con configuración vacía hasta confirmación; WhatsApp Business API permanece R3+. | MSG-006 |
| ¿Captcha/protección anti-bot preferida? | ALTCHA autoalojado + honeypot; Cloudflare Turnstile solo como alternativa tras revisar encargo/transferencias. | IAM-006, MSG-003 |
| ¿Email entrante necesario al lanzamiento? | No; bandeja interna + aviso/email saliente son MVP; reply-by-email R2. | D07, MSG-010 y backlog R2-INB |

## Marca, contenido y licencias
| Pregunta | Opción por defecto / supuesto | Cierra |
|---|---|---|
| ¿Vídeo hero, póster, variantes de logo para claro/oscuro? | El cliente aporta assets; sin ellos se publica solo logo estático, sin bloquear MVP. | WEB-002, UIK-006 |
| ¿Fotos, bios, cargos y enlaces del equipo? | No inventar personas; página muestra solo miembros aprobados y puede ocultar bloque vacío. | CMS-009, WEB-005 |
| ¿Se incluirán fuentes web personalizadas en el portal? | No; se usa `system-ui, sans-serif` y no se distribuyen archivos de fuentes ni se consultan CDN. | UIK-003 |
| ¿Fuente legalmente aprobada para textos de privacidad/términos/cookies? | CMS crea estructura y borradores; no publicar contenido legal sin revisión del responsable. | CMS-010, LGL-001, HRD-008 |

## Seguridad, negocio y privacidad
| Pregunta | Opción por defecto / supuesto | Cierra |
|---|---|---|
| ¿Opción de contraseña en SimbioSphere A/B/C? | Decidida A: no enviar ni persistir password; activación just-in-time de un uso. B/C descartadas por exposición/acoplamiento; registrar decisión en ADR-002. | ADR-002, DEMO-06a/06b |
| ¿MFA para EMPLOYEE en MVP? | Opcional opt-in en MVP; ADMIN obligatorio; reevaluar obligatorio en R2 según evaluación de riesgo. | IAM-009, ADR-002 |
| ¿Cuáles son los plazos legales de retención? | Tabla de 07 es propuesta (cuenta, solicitudes, contacto, audio, logs, auditoría, outbox, tokens y consentimientos); no purgar irreversiblemente hasta aprobación. | LGL-003, HRD-008 |
| ¿SLA de primera respuesta y calendario hábil? | 1 día hábil es valor inicial configurable (**SUPUESTO**); confirmar feriados/horarios antes de publicar compromiso. | EMP-007 |
| ¿Países de clientes y validación fiscal necesaria? | NIF/CIF opcional al registro, requerido al aceptar presupuesto en países definidos; validación sintáctica local, no consulta a autoridad. | CUS-004, QUO-003 |
| ¿Quién aprueba categoría/moneda/IVA y términos de presupuesto? | EUR como propuesta del prompt; fiscalidad, IVA, validez y categoría requieren revisión legal/comercial. | QUO-001/002, HRD-008 |
| ¿Se requiere notificación in-app en MVP? | No; email en MVP, campana R2. Las tareas NTF-005 cubren preferencias, no implementación de campana. | RF-NOT-02, R2-NTF |
| ¿Cómo se cierra RF-DOC-04 si ninguna fila del roadmap la incluye en `Cubre`? | GOV-006 mantiene ADR versionados con formato, enlaces y verificaciones; RF-DOC-04 tiene cobertura directa en el catálogo. | GOV-006, RF-DOC-04 |

## Inventario operativo de errores
| Pregunta | Opción por defecto / supuesto | Cierra |
|---|---|---|
| ¿Formato/almacenamiento del inventario y logs? | `ErrorInventoryService` mantiene JSON privado fuera del webroot y SQL (`logs-error/error-inventory.json`); los logs JSON de aplicación siguen siendo el canal técnico. El inventario no reemplaza logs/APM. | ERR-001/002, ADR-012 |
| ¿Cap máximo, retención y política de purga del JSON? | Empezar con límites configurables por tamaño y entradas, deduplicación, edad/cap como criterios de purga; no fijar cifra hasta medir y aprobar responsable. Backup del volumen cifrado; nunca guardar stack/raw message por defecto. | ERR-001, HRD-005/007, ADR-012 |
| ¿Despliegue tendrá una instancia o varias réplicas? | MVP asume single-writer/una instancia para JSON; varias réplicas requieren storage coordinado, lock probado o servicio externo evaluado antes de escalar. | ERR-001, HRD-006/010, ADR-012 |
| ¿Qué fallos se inventarían y con qué severidad? | Captura amplia de backend, frontend y BD, clasificada por origen/tipo/severidad; incluir stack/cadena causal saneados y contexto tecnico acotado. Aplicar deduplicacion, limites y exclusiones explicitas de secretos/PII; revisar el ruido de errores esperados durante pruebas. | ERR-002, ERR-003, ERR-004, ADR-012 |
| ¿Qué contexto frontend se permite enviar y con qué consentimiento? | Opt-in explícito: banda no modal ES/EN, denegada por defecto, aceptar/rechazar y revocación; decisión versionada solo en localStorage, sin identidad ni registro backend. Fallo al guardar mantiene denegado. El copy inicial es provisional y requiere revisión legal antes de producción. Tras consentir: endpoint same-origin con CSRF/rate-limit; release, locale, ruta sin query, tipo/código, correlación y stack saneado/truncado. Nunca formularios, DOM, body, cookies, tokens ni headers. | ERR-003, ADR-012 |
| ¿Quién ve y resuelve los errores? | ADMIN con permiso dedicado; CLIENT/EMPLOYEE no acceden al inventario. Auditoría de resolución; acceso a archivo solo por el proceso. | ERR-004, IAM-012 |

## Stack y artefactos externos
| Pregunta | Opción por defecto / supuesto | Cierra |
|---|---|---|
| ¿Boot 4.2 será GA y conviene adoptarlo? | No bloquear por RC: brief verifica Boot 4.1.2 como estable a 2026-11-10; confirmar la última estable/soporte al ejecutar FND-001. | ADR-001, FND-001 |
| ¿Son compatibles Thymeleaf, Layout Dialect, extras-security, Flyway, Argon2, Bucket4j y Resilience4j con Boot 4? | Comprobar dependencias y pruebas con H2 en el spike; la compatibilidad SQL Server/Flyway en ese motor queda sin verificar y no bloquea el desarrollo. | ADR-001, FND-001 |
| ¿Qué controles de licencia del producto y proveedor se requieren? | SBOM y revisión documental; no introducir assets/dependencias de licencia desconocida. El portal no incluye fuentes web. | GOV-001, FND-008, HRD-013 |

## Huecos del catálogo y fases futuras
| Pregunta | Opción por defecto / supuesto | Cierra |
|---|---|---|
| ¿MVP necesita sincronizar suspensión/reactivación/transferencia/borrado DEMO? | No; RF-SIM-08 es extensión R2. El catálogo no tiene tarea portal atómica explícita; incluirla en refinamiento R2 antes de compromiso. | DEMO-09 (R2), R-23 |
| ¿Quién gestiona copy de gratuidad/cobro futuro de solicitudes en CMS? | Sin pagos en MVP; CMS-013 gestiona el copy de gratuidad/condiciones y su publicación queda condicionada a aprobación legal/comercial. | CMS-013, RF-REQ-07 |
| ¿Notificaciones in-app (RF-NOT-02) se desglosan? | No en MVP; R2-NTF es épica y NTF-005 solo cubre preferencias. Desglosar al planificar R2. | R2-NTF, R-23 |
| ¿Módulos por defecto y catálogo DEMO quedan visibles/actualizados? | DEMO-08 define defaults por tipo; DEMO-12 (R2) publica catálogo/selector. Portal muestra `modulesApplied`; no replica reglas. | DEMO-08, DEMO-12 (R2) |
| ¿MFA EMPLOYEE pasa a obligatorio en MVP? | No; default opt-in acordado en brief, con reevaluación R2. | IAM-009, R2-MFA |
| ¿Desde qué estados se permite cancelar/pausar un desarrollo y quién confirma cierre? | Default propuesto en 07: cliente cancela antes de EN_DESARROLLO; staff pausa en análisis/desarrollo con motivo; cliente confirma EN_REVISION_CLIENTE→ENTREGADA→CERRADA. Revisar con negocio antes de implementar estados. | REQ-003, EMP-005; tabla de transiciones en 07 |
| ¿SSO OIDC, WhatsApp Business API, IA, pagos, kanban, email entrante, diff CMS, PWA y más idiomas entran en MVP? | No; conservar R2/R3+ del roadmap, no prometer disponibilidad. | 10-roadmap-backlog.md §7 |
