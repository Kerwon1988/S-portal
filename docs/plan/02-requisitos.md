# 02 — Requisitos

Catálogo de requisitos trazado a [idea](../idea.md), tareas y pruebas `TC-<ID>`; las tareas y fases se rigen por el [roadmap](10-roadmap-backlog.md). `MVP` comprende F0–F6; `R2` y `R3+` son posteriores al MVP. Prioridad MoSCoW: Must, Should, Could, Won't.

## Requisitos funcionales

### RF-UI-01 — Responsive mobile-first y táctil
- Prioridad: Must
- Fase: MVP / F0
- Tareas: UIK-006
- Criterios: Given viewport de 320 px When se muestran páginas clave Then no hay desbordamiento y cada objetivo táctil mide al menos 44 × 44 px.

### RF-UI-02 — Tema claro/oscuro
- Prioridad: Must
- Fase: MVP / F0
- Tareas: UIK-002, UIK-004
- Criterios: Given preferencia del sistema y luego una selección manual When se renderiza una página Then aplica el tema elegido, persiste en localStorage/cookie para usuarios anónimos y autenticados y evita FOUC con nonce CSP; la persistencia en perfil queda diferida a la integración con Identidad.

### RF-UI-03 — Paleta de marca y contraste AA
- Prioridad: Must
- Fase: MVP / F0
- Tareas: UIK-002
- Criterios: Given tokens en ambos temas When se ejecuta el test de contraste Then texto normal alcanza 4,5:1 y componentes 3:1; el lima no se usa como texto sobre fondo claro.

### RF-UI-04 — Tipografía del sistema sin recursos externos
- Prioridad: Must
- Fase: MVP / F0
- Tareas: UIK-003
- Criterios: Given cualquier página When se genera el build Then marca, titulares y cuerpo usan la pila `system-ui, sans-serif`, sin archivos de fuentes, `@font-face`, preload ni peticiones externas de tipografías.

### RF-UI-05 — i18n ES/EN
- Prioridad: Must
- Fase: MVP / F0
- Tareas: UIK-005
- Criterios: Given una visita sin preferencia When se elige locale Then se conserva la página al cambiar idioma, las claves ES/EN coinciden y fechas se muestran en `Europe/Madrid` desde UTC.

### RF-UI-06 — Menú por rol y desbordamiento
- Prioridad: Must
- Fase: MVP / F0
- Tareas: UIK-007
- Criterios: Given cada rol e idioma When los enlaces no caben tras `document.fonts.ready` Then pasan todos al menú derecho; teclado, Esc, clic fuera y focus trap funcionan.

### RF-UI-07 — Layouts, errores y footer
- Prioridad: Must
- Fase: MVP / F0
- Tareas: UIK-006
- Criterios: Given rutas públicas, privadas y errores When se renderizan Then usan layout por área, skip link, footer legal y páginas de marca 403/404/500 en ES/EN.

### RF-UI-08 — HTMX accesible y protegido
- Prioridad: Must
- Fase: MVP / F0
- Tareas: UIK-008
- Criterios: Given filtro, paginación o mutación HTMX When se envía Then hay mejora progresiva, CSRF, errores y toasts accesibles sin perder foco.

### RF-WEB-01 — Home con hero LCP y vídeo condicional
- Prioridad: Should
- Fase: MVP / F1
- Tareas: WEB-001, WEB-002
- Criterios: Given escritorio, táctil, `Save-Data` o movimiento reducido When carga el hero Then el logo estático es LCP y el vídeo solo se reproduce según política; el fallo muestra el póster.

### RF-WEB-02 — Secciones de home y propuesta de valor
- Prioridad: Must
- Fase: MVP / F1
- Tareas: WEB-001, CMS-012
- Criterios: Given la home en ES/EN When se publica Then muestra propuesta, servicios, CTAs de SimbioSphere/desarrollo y visión de IA sin prometer funciones existentes.

### RF-WEB-03 — Página SimbioSphere
- Prioridad: Must
- Fase: MVP / F1
- Tareas: WEB-003
- Criterios: Given la página publicada When presenta capacidades y plan DEMO Then cada afirmación está contrastada con evidencia del brief y no anuncia módulos, precios ni cobros no disponibles.

### RF-WEB-04 — Página de desarrollo a medida
- Prioridad: Must
- Fase: MVP / F1
- Tareas: WEB-004
- Criterios: Given la página en ES/EN When se consulta Then enumera landing, web corporativa, aplicación web y otros, describe el proceso y enlaza al formulario.

### RF-WEB-05 — Sobre nosotros
- Prioridad: Must
- Fase: MVP / F1
- Tareas: WEB-005, CMS-009
- Criterios: Given miembros publicados en CMS When se visita la página Then se muestran nombre, rol, bio, foto y enlaces con textos alternativos adecuados.

### RF-WEB-06 — Textos legales versionados
- Prioridad: Must
- Fase: MVP / F1
- Tareas: CMS-010, WEB-006
- Criterios: Given aviso legal, privacidad, cookies y términos When se publica una versión Then conserva vigencia/versionado; el banner aparece solo ante cookies no esenciales.

### RF-WEB-07 — SEO técnico bilingüe
- Prioridad: Must
- Fase: MVP / F1
- Tareas: CMS-004, SEO-001, SEO-002
- Criterios: Given página CMS por locale When se publica Then ofrece title/description, canonical, hreflang, OpenGraph, JSON-LD, sitemap por idioma, robots y 301 al cambiar slug.

### RF-WEB-08 — Cookies y analítica
- Prioridad: Must
- Fase: MVP / F1
- Tareas: WEB-006
- Criterios: Given solo cookies técnicas When se visita el sitio Then no aparece banner; si se activa analítica no esencial, requiere elección previa y se documenta el consentimiento.

### RF-CMS-01 — Contenido bilingüe por bloques
- Prioridad: Must
- Fase: MVP / F1
- Tareas: CMS-001, CMS-004, CMS-006, CMS-009
- Criterios: Given páginas y bloques hero, tarjetas, texto, imagen, CTA o FAQ When se guardan Then tienen slug y contenido ES/EN con fallback definido.

### RF-CMS-02 — Publicación y vista previa
- Prioridad: Must
- Fase: MVP / F1
- Tareas: CMS-002, CMS-005
- Criterios: Given borrador o contenido publicado When un editor autorizado previsualiza Then no altera el público hasta publicar y registra autor/fecha.

### RF-CMS-03 — Restaurar versión anterior
- Prioridad: Must
- Fase: MVP / F1
- Tareas: CMS-007
- Criterios: Given dos publicaciones When se restaura la anterior Then se crea una nueva versión con snapshot recuperado; el diff completo queda para R2.

### RF-CMS-04 — Editor y sanitización
- Prioridad: Must
- Fase: MVP / F1
- Tareas: CMS-003, CMS-006
- Criterios: Given Markdown/bloques con HTML no permitido When se guarda y renderiza Then el servidor aplica allowlist y elimina scripts/eventos antes de `th:utext`.

### RF-CMS-05 — Biblioteca de medios
- Prioridad: Must
- Fase: MVP / F1
- Tareas: CMS-008, FIL-004
- Criterios: Given imagen subida When supera validación, antivirus y redimensionado Then genera variantes WebP y exige `alt` por idioma; SVG se rechaza o sanea.

### RF-CMS-06 — Estado de traducciones y fallback
- Prioridad: Must
- Fase: MVP / F1
- Tareas: CMS-004, CMS-011
- Criterios: Given una página con traducción ausente When se lista o visita Then el editor ve el pendiente y el público aplica la política de fallback sin URL rota.

### RF-CMS-07 — Permisos, auditoría y caché
- Prioridad: Must
- Fase: MVP / F1
- Tareas: AUD-001, CMS-002
- Criterios: Given usuario sin `CONTENT_EDIT` When intenta publicar Then recibe 403; una publicación autorizada se audita e invalida la caché publicada.

### RF-CMS-08 — Contenido semilla ES/EN
- Prioridad: Must
- Fase: MVP / F1
- Tareas: CMS-012
- Criterios: Given base de datos vacía When se inicializa el contenido Then home y páginas acordadas existen en ambos idiomas sin duplicar la semilla.

### RF-IAM-01 — Roles y permisos
- Prioridad: Must
- Fase: MVP / F1
- Tareas: IAM-001, IAM-012, IAM-014
- Criterios: Given un actor CLIENT, EMPLOYEE o ADMIN When accede a una acción Then se aplica permiso granular y el primer ADMIN solo nace por bootstrap protegido.

### RF-IAM-02 — Registro de cliente autoservicio
- Prioridad: Must
- Fase: MVP / F2
- Tareas: CUS-001, CUS-002, CUS-004, IAM-006, LGL-001
- Criterios: Given datos válidos de persona/cuenta When se registra Then crea OWNER, conserva aceptación versionada y no premarca consentimiento comercial.

### RF-IAM-03 — Política de contraseñas
- Prioridad: Must
- Fase: MVP / F2
- Tareas: CUS-002, IAM-002
- Criterios: Given contraseña fuera de 12–64 caracteres When se registra o cambia Then se rechaza; las válidas usan `DelegatingPasswordEncoder` con Argon2id.

### RF-IAM-04 — Verificación de email
- Prioridad: Must
- Fase: MVP / F2
- Tareas: IAM-003, IAM-007
- Criterios: Given token de verificación When se consume antes de caducar Then verifica una vez; expirado o reutilizado falla y el reenvío está limitado.

### RF-IAM-05 — Login y sesión
- Prioridad: Must
- Fase: MVP / F1
- Tareas: IAM-004
- Criterios: Given credenciales erróneas repetidas When se intenta acceder Then hay respuesta genérica y bloqueo progresivo; sesión usa `SPORTAL_SESSION` y logout la invalida.

### RF-IAM-06 — MFA TOTP
- Prioridad: Must
- Fase: MVP / F2
- Tareas: IAM-009
- Criterios: Given ADMIN sin segundo factor When inicia sesión Then requiere TOTP RFC 6238; EMPLOYEE puede activarlo en MVP y se reevalúa para R2.

### RF-IAM-07 — Recuperación y cambio de contraseña
- Prioridad: Must
- Fase: MVP / F2
- Tareas: IAM-008
- Criterios: Given petición de recuperación When se emite el enlace Then el token se guarda hasheado, caduca, es de un uso y la respuesta no revela si existe la cuenta.

### RF-IAM-08 — Perfil y derechos de datos
- Prioridad: Must
- Fase: MVP / F2
- Tareas: IAM-011, LGL-002
- Criterios: Given cliente autenticado When consulta preferencias o solicita exportación/baja Then solo modifica sus datos y recibe exportación o anonimización trazable.

### RF-IAM-09 — Alta de trabajadores por invitación
- Prioridad: Must
- Fase: MVP / F2
- Tareas: IAM-010, STF-001, STF-002
- Criterios: Given ADMIN invitando a trabajador When este acepta enlace vigente Then define su contraseña; no se envía contraseña predefinida ni se permite alta pública.

### RF-IAM-10 — Alta de clientes por ADMIN
- Prioridad: Must
- Fase: MVP / F2
- Tareas: IAM-010, CUS-003
- Criterios: Given ADMIN crea una invitación When el cliente acepta términos y define contraseña Then se activa su cuenta; el aprovisionamiento SimbioSphere espera esa aceptación.

### RF-SIM-01 — Campos del alta conjunta
- Prioridad: Must
- Fase: MVP / F5
- Tareas: SIM-009, DEMO-04, DEMO-07
- Criterios: Given tipo RESTAURANT, RETAIL, SERVICES o REPAIR_SHOP When se completa el alta Then valida slugs y términos independientes del portal.

### RF-SIM-02 — Módulos delegados en DEMO
- Prioridad: Must
- Fase: MVP / F5
- Tareas: SIM-009, DEMO-08
- Criterios: Given módulo omitido/null When DEMO aplica defaults Then selecciona por tipo; `[]` significa cero opcionales y el portal no replica reglas.

### RF-SIM-03 — Saga de aprovisionamiento
- Prioridad: Must
- Fase: MVP / F5
- Tareas: SIM-001, SIM-005, SIM-006, SIM-007, DEMO-03a, DEMO-03b
- Criterios: Given alta local When email verificado Then outbox dispara llamada idempotente fuera de transacción; estados y errores son recuperables.

### RF-SIM-04 — Contraseña independiente en SimbioSphere
- Prioridad: Must
- Fase: MVP / F5
- Tareas: SIM-008, DEMO-06a, DEMO-06b
- Criterios: Given aprovisionamiento exitoso When el titular pulsa activar Then DEMO entrega enlace de un uso just-in-time; el portal nunca envía ni almacena esa contraseña.

### RF-SIM-05 — Verificación de email sin duplicidad
- Prioridad: Must
- Fase: MVP / F5
- Tareas: SIM-005, DEMO-06a
- Criterios: Given email verificado en portal When se crea titular en DEMO Then la API M2M transmite evidencia auditable y el usuario no repite verificación.

### RF-SIM-06 — Vínculo de cuentas
- Prioridad: Must
- Fase: MVP (diseño) / F5; implementación existente R2
- Tareas: CUS-001, SIM-001, SIM-012, DEMO-05, DEMO-09
- Criterios: Given cuentas vinculadas When se persiste la relación Then se soportan varios tenants por cuenta y nunca se vincula solo por email.

### RF-SIM-07 — Panel Mi SimbioSphere
- Prioridad: Must
- Fase: MVP / F5
- Tareas: SIM-008, SIM-010
- Criterios: Given cliente con alta conjunta When abre su panel Then ve estado, negocios, acceso y acción de activación; solo ADMIN puede reintentar manualmente.

### RF-SIM-08 — Sincronización de ciclo de vida
- Prioridad: Won't
- Fase: R2 / extensión de F5
- Tareas: R2-LCS (backlog), DEMO-09; sin tarea portal atómica explícita
- Criterios: Given suspensión, reactivación, transferencia o borrado en DEMO When llega webhook firmado o consulta Then el vínculo se actualiza con auditoría y política de reintento.

### RF-SIM-09 — Capa anticorrupción
- Prioridad: Must
- Fase: MVP / F1 y F5
- Tareas: SIM-002, SIM-003, SIM-004, DEMO-01, DEMO-02a, DEMO-02b, DEMO-03c, DEMO-10
- Criterios: Given fallo remoto, RFC 7807 o timeout When se llama la API Then cliente tipado aplica HMAC, límites/circuit breaker y mapea error sin registrar PII.

### RF-SIM-10 — Alternativa temporal de alta
- Prioridad: Should
- Fase: MVP / F5
- Tareas: SIM-011
- Criterios: Given API DEMO no disponible y feature flag habilitado When el cliente continúa Then se ofrece `/onboarding` sin fingir vínculo automático y se registra la deuda.

### RF-USR-01 — Listados de clientes y trabajadores
- Prioridad: Must
- Fase: MVP / F2
- Tareas: USR-001
- Criterios: Given ADMIN en gestión When busca por estado, rol, verificación o vínculo Then filtra, ordena y pagina en servidor; CSV queda limitado a ADMIN.

### RF-USR-02 — Fichas de usuario
- Prioridad: Must
- Fase: MVP / F2
- Tareas: USR-002, AUD-002, SIM-010
- Criterios: Given ficha de cliente o trabajador When se carga Then muestra cuenta, vínculo, solicitudes, conversaciones y auditoría mediante paneles autorizados.

### RF-USR-03 — Acciones administrativas auditadas
- Prioridad: Must
- Fase: MVP / F2
- Tareas: USR-003, USR-004, STF-002
- Criterios: Given cambio de rol/estado o baja When ADMIN confirma Then audita actor, fecha y antes/después; impide quitar último ADMIN o degradarse a sí mismo.

### RF-REQ-01 — Tipos y categorías de solicitud
- Prioridad: Must
- Fase: MVP / F3
- Tareas: REQ-001
- Criterios: Given solicitud nueva When se clasifica Then es `SIMBIOSPHERE_MODULE` o `CUSTOM_DEVELOPMENT` y usa categorías configurables bilingües.

### RF-REQ-02 — Puntos de entrada y formularios
- Prioridad: Must
- Fase: MVP / F3
- Tareas: REQ-005, REQ-006
- Criterios: Given CTA de sugerencia o desarrollo When el cliente inicia Then abre formulario del tipo correcto y conserva el borrador al autenticarse.

### RF-REQ-03 — Datos y adjuntos de solicitud
- Prioridad: Must
- Fase: MVP / F3
- Tareas: REQ-005, REQ-006, REQ-007, SIM-014
- Criterios: Given formulario de solicitud When se envía Then valida título, objetivo, categoría, presupuesto, fecha y adjuntos; nunca descarga URLs de referencia.

### RF-REQ-04 — Acceso autenticado y borrador
- Prioridad: Must
- Fase: MVP / F3
- Tareas: REQ-005
- Criterios: Given visitante sin sesión When intenta enviar Then va a login/registro y al autenticarse recupera el borrador sin exponerlo a otra cuenta.

### RF-REQ-05 — Referencia legible
- Prioridad: Must
- Fase: MVP / F3
- Tareas: REQ-002
- Criterios: Given solicitud persistida When se confirma Then recibe una referencia única `SOL-AAAA-NNNNNN` sin usarla como clave interna expuesta.

### RF-REQ-06 — Mis solicitudes
- Prioridad: Must
- Fase: MVP / F3
- Tareas: REQ-008, REQ-009, QUO-003
- Criterios: Given CLIENT consulta detalle When se lista o cambia Then solo ve solicitudes propias, estados/historial público, mensajes públicos y acciones permitidas.

### RF-REQ-07 — Gratuidad provisional y cobro futuro
- Prioridad: Must
- Fase: MVP / F3
- Tareas: CMS-013, REQ-009
- Criterios: Given texto CMS bilingüe aprobado When se muestra el detalle Then refleja las condiciones vigentes de forma versionada; sin aprobación mantiene copy seguro configurable y no activa pagos ni facturación.

### RF-EMP-01 — Panel de empleado
- Prioridad: Must
- Fase: MVP / F3
- Tareas: EMP-001
- Criterios: Given EMPLOYEE o ADMIN When abre el panel Then ve nuevas, sin asignar, por estado, propias, vencidas por SLA y mensajes sin leer.

### RF-EMP-02 — Listado y filtros de solicitudes
- Prioridad: Must
- Fase: MVP / F3
- Tareas: EMP-002
- Criterios: Given empleado autorizado When filtra por tipo, categoría, estado, asignado, cliente, fechas o importe Then orden/página son servidor y CSV queda protegido.

### RF-EMP-03 — Detalle de solicitud y presupuesto
- Prioridad: Must
- Fase: MVP / F3
- Tareas: EMP-003, EMP-005, EMP-006, MSG-013, REQ-011, QUO-001, QUO-002, QUO-004
- Criterios: Given detalle de empleado When consulta o gestiona Then ve cronología, asignación, presupuesto versionado, conversación y notas internas no visibles al cliente.

### RF-EMP-04 — Máquinas de estado
- Prioridad: Must
- Fase: MVP / F3
- Tareas: REQ-003, REQ-004, EMP-005
- Criterios: Given transición no permitida para tipo/rol When se solicita Then se rechaza sin efecto; las transiciones válidas registran precondiciones y visibilidad.

### RF-EMP-05 — Asignación y SLA
- Prioridad: Must
- Fase: MVP / F3
- Tareas: EMP-004, EMP-007
- Criterios: Given responsable y colaboradores When se asignan Then se notifica al responsable y el panel calcula carga y SLA de primera respuesta configurable.

### RF-MSG-01 — Formulario público de contacto
- Prioridad: Must
- Fase: MVP / F4
- Tareas: MSG-003, MSG-004
- Criterios: Given contacto anónimo o autenticado When envía motivo, mensaje y adjuntos permitidos Then se registra consentimiento, límites antiabuso y acuse; el mensaje no crea solicitud.

### RF-MSG-02 — Nota de audio
- Prioridad: Should
- Fase: MVP / F4
- Tareas: MSG-005
- Criterios: Given HTTPS y permiso de micrófono When se graba hasta 2 minutos Then hay previsualización, webm/opus o mp4/aac y fallback a fichero.

### RF-MSG-03 — Botones de canal
- Prioridad: Must
- Fase: MVP / F4
- Tareas: MSG-006
- Criterios: Given número y correo configurados When se elige WhatsApp o email Then abre `wa.me` o `mailto:` con texto seguro; no simula ingreso en la bandeja.

### RF-MSG-04 — Contacto distinto de solicitud
- Prioridad: Must
- Fase: MVP / F4
- Tareas: MSG-003, MSG-013
- Criterios: Given conversación de contacto When EMPLOYEE la convierte Then se crea solicitud enlazada y auditada sin duplicar ni alterar el hilo original.

### RF-MSG-05 — Bandeja de conversaciones
- Prioridad: Must
- Fase: MVP / F4
- Tareas: MSG-007, MSG-008, MSG-009
- Criterios: Given empleado autorizado When filtra o responde Then puede asignar, etiquetar, archivar, restaurar y enviar adjuntos/plantillas según permiso.

### RF-MSG-06 — Respuesta sin cuenta
- Prioridad: Must
- Fase: MVP / F4
- Tareas: MSG-010
- Criterios: Given email de respuesta When el destinatario usa enlace mágico vigente Then entra solo al hilo previsto; token ≥128 bits, hasheado, revocable y de caducidad.

### RF-MSG-07 — Módulo único de mensajería
- Prioridad: Must
- Fase: MVP / F3
- Tareas: MSG-001, MSG-002
- Criterios: Given conversación de contacto o solicitud When se persiste Then usa contexto CONTACT/REQUEST y visibilidad PUBLIC/INTERNAL sin fuga entre módulos.

### RF-MSG-08 — Actualización casi en tiempo real
- Prioridad: Should
- Fase: MVP / F4
- Tareas: MSG-011
- Criterios: Given hilo abierto When llega mensaje nuevo Then polling HTMX actualiza contenido y contador sin recarga completa ni exponer notas internas.

### RF-MSG-09 — Preparación para IA
- Prioridad: Should
- Fase: MVP (puertos); R3+ (IA)
- Tareas: MSG-002, MSG-012
- Criterios: Given mensaje entrante When se publica evento Then contiene solo IDs y permite borrador pendiente de aprobación; no llama proveedor de IA en MVP.

### RF-NOT-01 — Emails transaccionales bilingües
- Prioridad: Must
- Fase: MVP / F2 y F4
- Tareas: NTF-002, NTF-004, REQ-010
- Criterios: Given destinatario ES o EN When se envía aviso de cuenta, solicitud o conversación Then recibe plantilla Thymeleaf de marca y texto plano en su idioma.

### RF-NOT-02 — Notificaciones in-app
- Prioridad: Won't
- Fase: R2 / extensión de F4
- Tareas: R2-NTF (backlog; sin tarea atómica ni TC en catálogo)
- Criterios: Given notificación pendiente When se habilite R2 Then campana y contador respetan lectura y permisos; no se exige en el MVP.

### RF-NOT-03 — Outbox, preferencias y logs
- Prioridad: Must
- Fase: MVP / F2 y F4
- Tareas: IAM-013, NTF-001, NTF-003, NTF-005
- Criterios: Given evento notificable When falla SMTP Then outbox reintenta; preferencias por categoría se aplican y logs excluyen destinatario/PII en claro.

### RF-NOT-04 — Entregabilidad de email
- Prioridad: Must
- Fase: MVP / F2 y F6
- Tareas: NTF-002, HRD-009
- Criterios: Given entorno local y dominio productivo When se prueba envío Then local usa Mailpit y producción verifica SPF, DKIM, DMARC y entrega.

### RF-DOC-01 — README raíz
- Prioridad: Must
- Fase: MVP / F0
- Tareas: GOV-004
- Criterios: Given clon del repositorio When se sigue README Then se encuentran propósito, arquitectura, módulos, arranque, configuración, tests y enlaces.

### RF-DOC-02 — Inventario vivo
- Prioridad: Must
- Fase: MVP / F0
- Tareas: GOV-004
- Criterios: Given módulo, ruta, permiso, tabla, evento o test planificado When cambia su estado Then inventario lo refleja con owner y estado normalizado.

### RF-DOC-03 — DIARIO de cambios
- Prioridad: Must
- Fase: MVP / F0
- Tareas: GOV-004
- Criterios: Given cambio completado When se integra Then DIARIO añade fecha, tarea, módulo, tipo, resumen, estado y evidencias en orden inverso.

### RF-DOC-04 — ADR versionados
- Prioridad: Must
- Fase: MVP / F0–F6
- Tareas: GOV-006
- Criterios: Given decisión técnica relevante When se registra Then el ADR usa nombre acordado, estado/fecha, alternativas, consecuencias y tarea de verificación pendiente.

### RF-DOC-05 — Inventario y diagramas Modulith
- Prioridad: Must
- Fase: MVP / F0
- Tareas: GOV-005
- Criterios: Given módulos detectados por Modulith When se valida documentación Then cada módulo figura en inventario y los diagramas se generan/actualizan.

### RF-GOV-01 — Normas, instrucciones y skill
- Prioridad: Must
- Fase: MVP / F0
- Tareas: FND-001, FND-003, GOV-001, GOV-002, GOV-003
- Criterios: Given feature, fix o refactor When empieza el trabajo Then skill e instrucciones exigen análisis modular, TDD, seguridad y actualización documental.

### RF-GOV-02 — Gates modulares y de CI
- Prioridad: Must
- Fase: MVP / F0
- Tareas: FND-004, FND-008
- Criterios: Given ciclo modular, clave i18n, plantilla o cobertura inválidos When corre CI Then el gate falla y bloquea la integración.

### RF-ERR-01 — Registro central de errores backend, frontend y base de datos
- Prioridad: Must
- Fase: MVP / F0
- Tareas: ERR-001, ERR-002, ERR-003
- Criterios: Given error reportable de backend, frontend o BD When se captura Then el inventario conserva origen, severidad, recurrencia, stack/cadena causal y contexto técnico suficiente para diagnóstico asistido por IA, saneado, acotado y deduplicado, sin depender de SQL Server ni guardar secretos/PII en bruto. Los errores esperados se clasifican en vez de descartarse silenciosamente; reparación automática queda fuera del MVP.

### RF-ERR-02 — Consulta y resolución del inventario de errores
- Prioridad: Must
- Fase: MVP / F1
- Tareas: ERR-004
- Criterios: Given ADMIN autorizado When consulta errores Then puede filtrar/ver resumen y marcar resolución; roles no autorizados reciben 403 y no acceden al inventario.

## Requisitos no funcionales

### RNF-SEC-01 — Autorización e IDOR
- Prioridad: Must
- Fase: MVP / F1–F3
- Tareas: HRD-001, IAM-012, MSG-002, REQ-011
- Criterios: Given usuario con rol válido pero sin propiedad When consulta/revoca recurso ajeno Then recibe 403/404 y el test IDOR confirma que no hay datos filtrados.

### RNF-SEC-02 — CSRF
- Prioridad: Must
- Fase: MVP / F0
- Tareas: FND-009
- Criterios: Given mutación web sin token When llega POST/HTMX Then responde 403; token válido de `SPORTAL_XSRF` permite la acción autorizada.

### RNF-SEC-03 — Cabeceras y CSP
- Prioridad: Must
- Fase: MVP / F0; enforcement F6
- Tareas: FND-009, HRD-002
- Criterios: Given respuesta HTML When se inspecciona Then CSP usa nonce sin `unsafe-inline/eval`, con HSTS, `frame-ancestors`, nosniff y política de referrer.

### RNF-SEC-04 — Cookies seguras
- Prioridad: Must
- Fase: MVP / F0–F1
- Tareas: FND-009, IAM-004
- Criterios: Given sesión autenticada When se emite cookie Then usa `SPORTAL_SESSION`, HttpOnly, Secure en prod y SameSite=Lax, distinta de DEMO.

### RNF-SEC-05 — Credenciales, tokens y cifrado
- Prioridad: Must
- Fase: MVP / F0–F2; rotación F6
- Tareas: IAM-002, IAM-003, HRD-010
- Criterios: Given contraseña, token o PII sensible When se almacena Then se usa Argon2id/hash de token/cifrado aprobado; no aparece secreto en repo ni logs.

### RNF-SEC-06 — Subida y descarga segura
- Prioridad: Must
- Fase: MVP / F1, F3 y F4
- Tareas: FIL-001, FIL-002, FIL-003, REQ-007
- Criterios: Given fichero no permitido o infectado When se valida Then se rechaza o pone en cuarentena; descarga autorizada usa nombre aleatorio y `attachment`.

### RNF-SEC-07 — Abuso y rate limiting
- Prioridad: Must
- Fase: MVP / F1–F4
- Tareas: IAM-005, IAM-006
- Criterios: Given exceso por IP/email en login, registro o contacto When supera cuota Then recibe limitación sin revelar cuentas ni depender de captcha de terceros por defecto.

### RNF-SEC-08 — Auditoría y logs sin PII
- Prioridad: Must
- Fase: MVP / F1
- Tareas: AUD-001, AUD-002, FND-010
- Criterios: Given acción administrativa o fallo de integración When se registra Then incluye actor/acción/correlation ID y enmascara PII, tokens y credenciales.

### RNF-SEC-09 — Secretos y dependencias
- Prioridad: Must
- Fase: MVP / F0; operación F6
- Tareas: FND-007, FND-008, HRD-001, HRD-010, IAM-014
- Criterios: Given build/CI When se detecta secreto literal o dependencia sobre umbral acordado Then falla; producción obtiene secretos de entorno/gestor y permite rotación.

### RNF-PRV-01 — Consentimientos versionados
- Prioridad: Must
- Fase: MVP / F2
- Tareas: CMS-010, LGL-001
- Criterios: Given aceptación legal When se confirma Then conserva versión, fecha, origen y evidencia; el consentimiento comercial es separado y no premarcado.

### RNF-PRV-02 — Minimización y retención
- Prioridad: Must
- Fase: MVP / F2; verificación F6
- Tareas: LGL-003, HRD-008
- Criterios: Given dato que alcanza plazo aprobado When corre purga Then se anonimiza/elimina según tabla de retención y queda evidencia sin conservar contenido innecesario.

### RNF-PRV-03 — Derechos de datos
- Prioridad: Must
- Fase: MVP / F2
- Tareas: LGL-002, USR-004
- Criterios: Given solicitud del titular When se exporta o ejerce baja Then se incluyen datos propios y se anonimiza PII preservando registros legítimos seudonimizados.

### RNF-PRV-04 — RAT y encargados
- Prioridad: Must
- Fase: MVP / F6
- Tareas: HRD-008
- Criterios: Given proveedor de hosting/email/antivirus/captcha When se prepara producción Then RAT y contrato de encargado documentan finalidad, datos y transferencias.

### RNF-PRV-05 — Cookies de privacidad
- Prioridad: Must
- Fase: MVP / F1
- Tareas: WEB-006
- Criterios: Given sitio con solo cookies técnicas When se visita Then no activa consentimiento no esencial; analítica con cookies queda bloqueada hasta elección válida.

### RNF-A11Y-01 — WCAG 2.2 AA
- Prioridad: Must
- Fase: MVP / F0–F6
- Tareas: UIK-008, UIK-009, HRD-004
- Criterios: Given navegación por teclado o lector When se ejecutan axe y revisión manual Then hay landmarks, skip link, foco visible, errores asociados, contraste AA y movimiento reducido.

### RNF-PERF-01 — Rendimiento web y Core Web Vitals
- Prioridad: Must
- Fase: MVP / F1 y F6
- Tareas: WEB-007, HRD-003
- Criterios: Given páginas públicas en móvil When corre Lighthouse CI Then cada página alcanza ≥90, LCP <2,5 s, CLS <0,1 e INP <200 ms.

### RNF-PERF-02 — Assets optimizados
- Prioridad: Must
- Fase: MVP / F0–F1
- Tareas: FIL-004, WEB-007
- Criterios: Given assets de producción When se construye el sitio Then tienen huella/caché, imágenes responsive WebP/AVIF, lazy loading y recursos locales sin CDN.

### RNF-PERF-03 — Consultas de datos
- Prioridad: Must
- Fase: MVP / F1–F6
- Tareas: HRD-003
- Criterios: Given listados de volumen When se consulta Then paginan y ordenan en servidor con índices documentados y sin cargar el conjunto completo.

### RNF-DB-01 — SQL Server y migraciones
- Prioridad: Must
- Fase: MVP / F0
- Tareas: FND-001, FND-005, FND-006
- Criterios: Given entorno de test limpio When se ejecuta la suite automatizada Then la persistencia se prueba solo con H2 y se conserva un único árbol Flyway dirigido a SQL Server para producción. No se requieren Docker, Testcontainers ni SQL Server para validar tareas o cerrar gates; la compatibilidad del dialecto de producción queda sin verificar.

### RNF-DB-02 — Esquema y tipos SQL
- Prioridad: Must
- Fase: MVP / F0–F1
- Tareas: FND-006, CMS-001, REQ-001
- Criterios: Given tabla de módulo When se crea Then pertenece a esquema owner, usa NVARCHAR, UTC `datetime2(3)` y precisión monetaria `DECIMAL(19,4)` según aplique.

### RNF-DB-03 — Claves e identificadores
- Prioridad: Must
- Fase: MVP / F0
- Tareas: FND-006
- Criterios: Given entidad persistida When se expone una URL Then usa `public_id` UUID no secuencial; `BIGINT IDENTITY` queda interno y su índice público es único.

### RNF-DB-04 — Edición y licencia SQL Server
- Prioridad: Must
- Fase: MVP / F0 y F6
- Tareas: HRD-013
- Criterios: Given decisión de despliegue When se aprueba ADR-006 Then compara edición, límites y cifrado con fuentes verificadas sin inventar costes ni afirmar TDE no soportado.

### RNF-OPS-01 — Monitorización y alertas
- Prioridad: Must
- Fase: MVP / F0 y F6
- Tareas: FND-010, NTF-003, HRD-007
- Criterios: Given caída de ClamAV, cola de email o aprovisionamiento fallido When se supera umbral Then hay métrica, alerta y correlation ID sin PII.

### RNF-OPS-02 — Backups y restauración
- Prioridad: Must
- Fase: MVP / F6
- Tareas: HRD-005
- Criterios: Given la estrategia de backup de producción When se documenta Then quedan definidos procedimiento, integridad esperada y RPO/RTO aprobados. No se exige ejecutar una restauración SQL Server en los gates de desarrollo porque no hay acceso a esa plataforma.

### RNF-OPS-03 — Entornos y despliegue
- Prioridad: Must
- Fase: MVP / F0 y F6
- Tareas: FND-002, FND-005, FND-007, HRD-006, HRD-011, HRD-014
- Criterios: Given dev/test/prod When se construye o despliega Then hay configuración reproducible, secretos externos, TLS y puertos separados de DEMO. Las pruebas locales y CI de persistencia usan H2; no se requieren Docker, SQL Server ni Testcontainers como gates. SQL Server 2022 sigue siendo el destino productivo, con compatibilidad no verificada por pruebas.

### RNF-TST-01 — Pruebas modulares y por capa
- Prioridad: Must
- Fase: MVP / F0
- Tareas: FND-004
- Criterios: Given dependencias fuera del DAG o acceso directo prohibido When corren los tests focales de Modulith y ArchUnit Then fallan con el módulo y regla infractora.

### RNF-TST-02 — Contrato con DEMO
- Prioridad: Must
- Fase: MVP / F1 y F5
- Tareas: SIM-004, SIM-013, DEMO-11
- Criterios: Given OpenAPI acordado When corren stubs y proveedor Then validan payload, HMAC, errores, idempotencia e aislamiento sin acceder a BD DEMO.

### RNF-TST-03 — Recorridos E2E
- Prioridad: Must
- Fase: MVP / F2–F5
- Tareas: UIK-009, E2E-001, E2E-002, E2E-003, E2E-004, HRD-012
- Criterios: Given flujos críticos When Playwright los ejecuta Then cubre invitación/CMS, solicitud/presupuesto, contacto/respuesta y alta conjunta.

### RNF-TST-04 — Matriz visual
- Prioridad: Must
- Fase: MVP / F0; completa F6
- Tareas: UIK-009, HRD-012
- Criterios: Given páginas clave When se comparan capturas Then cubren tema claro/oscuro, móvil/escritorio y ES/EN con umbral de diferencia aprobado.

### RNF-TST-05 — Autorización e i18n transversales
- Prioridad: Must
- Fase: MVP / F0–F3
- Tareas: IAM-012, UIK-005
- Criterios: Given matriz de roles y bundles When corren tests transversales Then cada endpoint/acción está autorizado y ES/EN tienen el mismo conjunto de claves.

### RNF-TST-06 — Cobertura domain/application
- Prioridad: Must
- Fase: MVP / F0
- Tareas: FND-008
- Criterios: Given módulo de negocio When JaCoCo calcula cobertura Then domain/application alcanza al menos 80 % y el gate falla bajo el umbral.
