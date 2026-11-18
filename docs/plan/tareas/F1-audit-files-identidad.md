# F1 — Auditoría, ficheros e identidad (núcleo para CMS y backoffice): tareas

- Objetivo: incorporar auditoría/ficheros y la identidad mínima para proteger CMS y gestión (hito M1; ver [roadmap](../10-roadmap-backlog.md)).
- Módulo(s) owner: `audit`, `files` e `identity`; `config` solo aporta seguridad transversal.

### Verificación y cierre de Prompt 05

Para cerrar las tareas incluidas en Prompt 05 deben cumplirse sus criterios funcionales y de seguridad, y pasar la compilación Java 21 y todos los tests requeridos. Las pruebas automatizadas de persistencia usan H2; SQL Server y Testcontainers no son gates de aceptación.

El ciclo completo del build y Enforcer se registran como evidencia independiente y no son gates adicionales para Prompt 05. Si un problema local de resolución Maven impide ejecutar el ciclo estándar, se acepta una invocación offline reproducible que compile y ejecute todos los tests requeridos; deben registrarse el comando y resultados, sin afirmar que el ciclo completo o Enforcer pasó cuando no ocurrió. No se permite omitir tests ni modificar los marcadores de caché. Si la resolución Maven impide compilar o ejecutar los tests, o alguno falla, las tareas afectadas siguen abiertas.

### AUD-001 — Módulo audit: modelo, API AuditRecorder y retención
- Fase / épica / módulo owner: F1 / AUD / audit
- Tipo: feat
- Depende de: FND-006, FND-003
- Descripción: Cubre: RNF-SEC-08, RF-CMS-07; crea registro inmutable de actor, acción, entidad, fecha y correlation ID, con retención propuesta de 3 años.
- Ficheros/paquetes a crear o modificar (aproximado): `com.simbio.core.audit.{api,application,domain,infrastructure}`, migración `audit.audit_entry`, `AuditRecorder`.
- Impacto modular: publica `audit.api.AuditRecorder`; identity/content/request consumen API; tabla solo owner `audit`; eventos con IDs. Ciclo: ninguno (no importa negocio).
- UI: n/a (registro y contrato; consulta en AUD-002).
- Seguridad: no guardar contraseñas, tokens ni PII en before/after; solo ADMIN/servicio autorizado puede consultar.
- Criterios de aceptación (Given/When/Then):
  - Given acción administrativa válida When se registra Then persiste actor/acción/entidad/correlation ID y el evento no admite actualización.
  - Given entrada con token o email When se normaliza Then el valor sensible se enmascara y la prueba detecta cualquier fuga.
- Tests: TC-AUD-001: UT (redacción/inmutabilidad), MT (`AuditRecorder`), IT (índice/retención).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Auditoría»
- Estimación: M (≤ 1 d)
- Riesgos: auditoría con payload completo acumula PII; persistir referencias mínimas.

### AUD-002 — UI de auditoría: listado y panel por entidad
- Fase / épica / módulo owner: F1 / AUD / audit
- Tipo: feat
- Depende de: AUD-001, UIK-008, IAM-012
- Descripción: Cubre: RNF-SEC-08, RF-USR-02; ofrece búsqueda paginada de eventos y panel cronológico para una entidad.
- Ficheros/paquetes a crear o modificar (aproximado): `audit.web`, `audit.application`, `templates/audit/`, `i18n/audit/messages_{es,en}.properties`.
- Impacto modular: usa `audit` propio y `identity.api` para actor; `audit.audit_entry` único owner. Ciclo: ninguno (solo lectura local).
- UI: `/gestion/auditoria`; filtros por actor/fecha/acción, locale, tema y tabla responsive.
- Seguridad: permiso `AUDIT_VIEW`; validar paginación y no permitir IDOR por entidad.
- Criterios de aceptación (Given/When/Then):
  - Given ADMIN con `AUDIT_VIEW` When consulta por entidad Then obtiene eventos paginados y solo campos sanitizados.
  - Given CLIENT o EMPLOYEE sin permiso When solicita `/gestion/auditoria` Then recibe 403 y no obtiene datos.
- Tests: TC-AUD-002: WT (403/permiso), SEC (IDOR), A11Y (filtros/tabla).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Auditoría»
- Estimación: M (≤ 1 d)
- Riesgos: consulta sin índices puede exponer datos o degradar DB; limitar filtros.

### FIL-001 — files: puerto de almacenamiento, disco local y descarga en streaming
- Fase / épica / módulo owner: F1 / FIL / files
- Tipo: feat
- Depende de: FND-006, FND-003
- Descripción: Cubre: RNF-SEC-06; define puerto `FileStorage`, adaptador local fuera del webroot y descarga autorizada en streaming.
- Ficheros/paquetes a crear o modificar (aproximado): `files.api.FileStorage`, `files.infrastructure.LocalFileStorage`, `files.file_metadata`, configuración de volumen.
- Impacto modular: publica contrato `files.api`; tabla `files.file_metadata` owner `files`; no consume módulos. Ciclo: ninguno (hoja de negocio).
- UI: n/a (puerto y adaptador; formularios llegan en CMS/MSG).
- Seguridad: nombre UUID aleatorio, path normalizado, límite de tamaño, `attachment` y `nosniff`; acceso verificado por módulo dueño.
- Criterios de aceptación (Given/When/Then):
  - Given stream válido When se almacena Then bytes quedan fuera del webroot con nombre aleatorio y metadatos asociados.
  - Given usuario sin permiso al recurso When descarga Then recibe 404/403 sin revelar la ruta física.
- Tests: TC-FIL-001: UT (path/stream), IT (persistencia y descarga), SEC (autorización).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Ficheros»; ADR-005
- Estimación: M (≤ 1 d)
- Riesgos: disco local requiere volumen/backups y espacio monitorizado.

### FIL-002 — files: validación Tika, allowlist, límites y SVG prohibido
- Fase / épica / módulo owner: F1 / FIL / files
- Tipo: feat
- Depende de: FIL-001
- Descripción: Cubre: RNF-SEC-06; valida tamaño, extensión y magic bytes con Tika contra allowlist por uso antes de persistir.
- Ficheros/paquetes a crear o modificar (aproximado): `files.application.FileValidationService`, `FilePolicy`, MIME allowlist y pruebas de payload.
- Impacto modular: amplía `files.api`; metadatos siguen en `files`; sin eventos ni dependencias nuevas. Ciclo: ninguno.
- UI: errores de carga asociados al campo, sin revelar detalle del parser.
- Seguridad: SVG rechazado por defecto; bloquear doble extensión, MIME falsificado, traversal y archivos ejecutables.
- Criterios de aceptación (Given/When/Then):
  - Given PNG cuyo magic bytes coincide When se valida bajo límite Then se acepta y registra MIME detectado.
  - Given SVG, MIME falsificado o fichero sobre límite When se valida Then se rechaza antes de escribir en disco.
- Tests: TC-FIL-002: UT (allowlist, límites, magic bytes; fixtures maliciosos DEBEN fallar), SEC.
- Documentación a actualizar: DIARIO + INVENTARIO; README «Tipos permitidos»
- Estimación: M (≤ 1 d)
- Riesgos: Tika consume recursos con muestras grandes; limitar bytes analizados.

### FIL-003 — files: antivirus ClamAV y cuarentena
- Fase / épica / módulo owner: F1 / FIL / files
- Tipo: feat
- Depende de: FIL-002, FND-005
- Descripción: Cubre: RNF-SEC-06; envía cargas admitidas a ClamAV y no las hace descargables hasta marcar estado limpio.
- Ficheros/paquetes a crear o modificar (aproximado): `files.infrastructure.ClamAvScanner`, estado `QUARANTINED/CLEAN/REJECTED`, métricas.
- Impacto modular: `files` llama adaptador ClamAV; tabla de metadatos owner `files`; no publica evento con PII. Ciclo: ninguno.
- UI: estado de carga y error genérico; no exponer nombre de firma ni ruta.
- Seguridad: fallo/corte de antivirus = fail closed; cuarentena fuera del webroot, timeout y límite de tamaño.
- Criterios de aceptación (Given/When/Then):
  - Given fichero limpio When scanner responde Then cambia a CLEAN y queda disponible solo con permiso.
  - Given detección o scanner no disponible When termina escaneo Then queda REJECTED/QUARANTINED y no puede descargarse.
- Tests: TC-FIL-003: UT (estados/timeout), IT (ClamAV healthy y caído), SEC (bloqueo cuarentena).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Antivirus»
- Estimación: M (≤ 1 d)
- Riesgos: ClamAV consume RAM y actualiza firmas; alertar por falta de salud.

### FIL-004 — files: redimensionado y variantes WebP
- Fase / épica / módulo owner: F1 / FIL / files
- Tipo: feat
- Depende de: FIL-002
- Descripción: Cubre: RF-CMS-05, RNF-PERF-02; produce variantes WebP limitadas para la biblioteca, conservando original solo según política.
- Ficheros/paquetes a crear o modificar (aproximado): `files.application.ImageVariantService`, `files.infrastructure`, campos de variante y configuración de dimensiones.
- Impacto modular: `files` consume sus metadatos; `content` consulta por API; owner de variantes `files`. Ciclo: ninguno.
- UI: imagen responsive por `<picture>/srcset`; CMS solicita `alt` en cada locale.
- Seguridad: decodificar formatos allowlist, quitar EXIF/GPS, limitar píxeles y no servir SVG activo.
- Criterios de aceptación (Given/When/Then):
  - Given imagen válida con dimensiones altas When se procesa Then genera variantes WebP bajo límites y sin EXIF de ubicación.
  - Given imagen corrupta o bomba de píxeles When se procesa Then falla en cuarentena sin agotar memoria ni publicar original.
- Tests: TC-FIL-004: UT (dimensiones/EXIF), IT (variantes), PERF (peso/srcset).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Medios»
- Estimación: M (≤ 1 d)
- Riesgos: decoder vulnerable o consumo excesivo; fijar versión y límites de píxeles.

### IAM-001 — identity: modelo de usuarios, roles y permisos con migraciones y seed
- Fase / épica / módulo owner: F1 / IAM / identity
- Tipo: feat
- Depende de: FND-006
- Descripción: Cubre: RF-IAM-01; modela usuario global, roles ADMIN/EMPLOYEE/CLIENT y permisos granulares con seed idempotente.
- Ficheros/paquetes a crear o modificar (aproximado): `identity.{domain,application,infrastructure}`, esquema `identity`, migraciones y `Role/Permission`.
- Impacto modular: `identity` publica tipos/APIs en `identity.api`; tablas `identity.user_account`, `role`, `permission`; sin eventos. Ciclo: consume solo `shared`.
- UI: n/a (modelo y seed; login en IAM-004).
- Seguridad: email indexado de forma segura, estado/verificación separados; no sembrar usuarios ni claves reales.
- Criterios de aceptación (Given/When/Then):
  - Given migración repetida When seed se ejecuta Then roles y permisos quedan una sola vez con ADMIN también tratado como trabajador.
  - Given migración incompleta o rol desconocido When valida dominio Then transacción falla sin asignar privilegio implícito.
- Tests: TC-IAM-001: UT (matriz de roles/permisos), IT (migración/seed idempotente).
- Documentación a actualizar: DIARIO + INVENTARIO; ADR-009 si cambia PII
- Estimación: M (≤ 1 d)
- Riesgos: matriz incompleta abre privilegios; IAM-012 añade tests rol × endpoint.

### IAM-002 — Política de contraseñas y DelegatingPasswordEncoder (Argon2id)
- Fase / épica / módulo owner: F1 / IAM / identity
- Tipo: feat
- Depende de: IAM-001
- Descripción: Cubre: RF-IAM-03, RNF-SEC-05; centraliza política de 12–64 caracteres y encoder Argon2id vía `DelegatingPasswordEncoder`.
- Ficheros/paquetes a crear o modificar (aproximado): `identity.application.PasswordPolicy`, `PasswordEncoderConfig`, propiedades de coste y pruebas.
- Impacto modular: `identity` usa interfaz Spring Security en infraestructura; no expone hash por API. Sin eventos ni tablas nuevas. Ciclo: ninguno.
- UI: errores de contraseña localizados en bundles de identity; sin revelar reglas internas del verificador.
- Seguridad: evitar límite BCrypt de 72 bytes; lista comprometida offline, sin composición arbitraria ni logs de credenciales.
- Criterios de aceptación (Given/When/Then):
  - Given password de 12–64 caracteres When se valida Then se codifica con Argon2id y el hash no contiene el texto recibido.
  - Given contraseña vacía, corta o >64 When se valida Then se rechaza con error genérico y no persiste credencial.
- Tests: TC-IAM-002: UT (límites/encoder), SEC (hash no reversible/logs sin secreto).
- Documentación a actualizar: DIARIO + INVENTARIO; ADR-002
- Estimación: M (≤ 1 d)
- Riesgos: Argon2/BouncyCastle y coste requieren VERIFICAR en FND-001 y calibrar en despliegue.

### IAM-005 — Rate limiting con Bucket4j (login, registro, recuperación, contacto)
- Fase / épica / módulo owner: F1 / IAM / config
- Tipo: feat
- Depende de: FND-009
- Descripción: Cubre: RNF-SEC-07; aplica cuotas por IP/email en login, registro, recuperación y contacto con almacenamiento compartido por instancia.
- Ficheros/paquetes a crear o modificar (aproximado): `config/security/RateLimitFilter`, `RateLimitProperties`, métricas y `RateLimitTest`.
- Impacto modular: `config` expone política transversal; sin API de negocio, eventos ni tablas propias. Ciclo: ninguno (no importa módulos).
- UI: respuesta 429 localizada, `Retry-After` y estilo accesible sin revelar cuota interna.
- Seguridad: proxy confiable explícito; no aceptar `X-Forwarded-For` arbitrario; no loguear email/IP en claro.
- Criterios de aceptación (Given/When/Then):
  - Given IP que supera cuota configurada When accede a login Then recibe 429/Retry-After y no se ejecuta autenticación.
  - Given cabecera forwarded desde proxy no confiable When se calcula cliente Then se ignora y no permite evadir el límite.
- Tests: TC-IAM-005: UT (ventanas/cuotas), WT (429), SEC (forged forwarded header).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Rate limiting»
- Estimación: M (≤ 1 d)
- Riesgos: límites por proceso se evaden en réplicas; usar backend compartido (VERIFICAR).

### IAM-004 — Login, logout, sesión, bloqueo progresivo y cookie propia
- Fase / épica / módulo owner: F1 / IAM / identity
- Tipo: feat
- Depende de: IAM-002, IAM-005, FND-009, UIK-006
- Descripción: Cubre: RF-IAM-05, RNF-SEC-04; implementa login/logout con mensajes genéricos, bloqueo progresivo y sesión regenerada al autenticar.
- Ficheros/paquetes a crear o modificar (aproximado): `identity.web`, `identity.application.AuthenticationService`, `templates/identity/login.html`, bundles.
- Impacto modular: `identity` publica contexto actual por SPI `shared`; `config` conecta filter chain sin dependencia inversa. Sin tablas nuevas.
- UI: `/acceso`, retorno seguro al destino local; tema, ES/EN, móvil y mensajes asociados a formulario.
- Seguridad: solo email verificado; cookie `SPORTAL_SESSION` HttpOnly/Secure/SameSite=Lax; prevenir fijación y enumeración.
- Criterios de aceptación (Given/When/Then):
  - Given credenciales válidas y email verificado When inicia sesión Then rota ID de sesión y redirige solo a destino relativo permitido.
  - Given usuario inexistente, contraseña errónea o no verificado When intenta login Then ve mensaje indistinguible y bloqueo progresivo.
- Tests: TC-IAM-004: WT (login/logout/CSRF), IT (cookie/rotación), SEC (enumeración/fijación).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Acceso»
- Estimación: M (≤ 1 d)
- Riesgos: DEMO usa nombres genéricos de sesión/CSRF (`config/SecurityConfig.java:113,122`); portal necesita cookies propias y puerto separado en localhost.

### IAM-012 — Matriz de autorización y test rol × endpoint
- Fase / épica / módulo owner: F1 / IAM / identity
- Tipo: test
- Depende de: IAM-004
- Descripción: Cubre: RF-IAM-01, RNF-SEC-01, RNF-TST-05; centraliza permisos por ruta/acción y pruebas positivas/negativas para cada rol.
- Ficheros/paquetes a crear o modificar (aproximado): `identity.api.Permission`, `config/security/AuthorizationRules`, `AuthorizationMatrixTest`, CSV/tabla matriz.
- Impacto modular: `config` aplica permisos publicados por `identity.api`; módulos protegen sus recursos en application. Sin tablas/eventos. Ciclo: ninguno.
- UI: denegación 403 de marca; navegación oculta es solo presentación, no autorización.
- Seguridad: roles ADMIN/EMPLOYEE/CLIENT y permisos granulares; comprobar propiedad del recurso además del rol.
- Criterios de aceptación (Given/When/Then):
  - Given ADMIN, EMPLOYEE y CLIENT When corren escenarios de matriz Then cada endpoint/acción coincide con permiso esperado.
  - Given CLIENT solicita recurso de otro titular When se ejecuta consulta Then retorna 403/404 y ningún dato del recurso.
- Tests: TC-IAM-012: WT (matriz endpoint), SEC (IDOR/deny-by-default), UT (permisos).
- Documentación a actualizar: DIARIO + INVENTARIO; 07-seguridad
- Estimación: M (≤ 1 d)
- Riesgos: nueva ruta sin fila de matriz; test debe fallar ante endpoint no clasificado.

### IAM-014 — Bootstrap del primer ADMIN por propiedad de arranque (sin credenciales en el repositorio)
- Fase / épica / módulo owner: F1 / IAM / identity
- Tipo: infra
- Depende de: IAM-002, IAM-012
- Descripción: Cubre: RF-IAM-01, RNF-SEC-09; provisiona primer ADMIN solo desde propiedad externa temporal y deshabilita bootstrap después.
- Ficheros/paquetes a crear o modificar (aproximado): `identity.application.AdminBootstrap`, propiedades `portal.bootstrap.*`, test de inicio y runbook.
- Impacto modular: `identity` crea usuario/rol mediante dominio; no publica credencial ni evento con secreto. Sin tablas nuevas. Ciclo: ninguno.
- UI: n/a (arranque controlado; gestión posterior por UI protegida).
- Seguridad: secreto solo por variable/gestor, hash Argon2id y eliminación/rotación obligatoria tras primer uso.
- Criterios de aceptación (Given/When/Then):
  - Given base vacía y secreto externo válido When arranca entorno autorizado Then crea exactamente un ADMIN y registra auditoría sin credencial.
  - Given propiedad ausente, perfil prod o bootstrap ya consumido When vuelve a arrancar Then no crea ni restablece cuenta privilegiada.
- Tests: TC-IAM-014: IT (creación idempotente), SEC (sin literal/segundo uso).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Bootstrap seguro»
- Estimación: S (≤ 0,5 d)
- Riesgos: propiedad persistente crea puerta trasera; consumir y deshabilitar en el primer arranque.
