# 06 — UX/UI

## Mapa del sitio
| Área | URL/páginas | Rol | i18n | Tema / viewport |
|---|---|---|---|---|
| Público | `/`, `/{lang}/simbiosphere`, `/{lang}/servicios/desarrollo`, `/{lang}/sobre-nosotros`, `/{lang}/contacto`, `/{lang}/legal/*`, slugs CMS | Anónimo; CTA a registro/login | ES/EN en URL, canonical/hreflang | Claro/oscuro; móvil, tablet, escritorio |
| Autenticación | `/acceso`, `/registro`, `/verificar-email`, `/recuperar`, `/restablecer` | Anónimo; invitado acepta invitación | ES/EN por locale/resolución | Claro/oscuro; formularios responsive |
| Cuenta | `/cuenta/perfil`, `/cuenta/notificaciones`, `/cuenta/solicitudes`, `/cuenta/solicitudes/nueva`, `/cuenta/datos`, `/cuenta/simbiosphere` | CLIENT (OWNER) | Perfil → cookie → Accept-Language | Claro/oscuro; móvil primero |
| Gestión | `/gestion`, `/gestion/clientes`, `/gestion/trabajadores`, `/gestion/contenido`, `/gestion/auditoria`, `/gestion/solicitudes`, `/gestion/bandeja`, `/gestion/provisioning`, `/gestion/errores` | EMPLOYEE/ADMIN según permiso; errores solo ADMIN | Perfil/cookie; mensajes en locale destinatario | Claro/oscuro; tablas utilizables en tablet |
| Errores | 403, 404, 500 | Según ruta | Locale de sesión/URL | Logo, tema, navegación segura |

## Inventario de páginas y wireframes
- **Home (`/{lang}`):** cabecera sticky; hero con logo estático como LCP y vídeo condicional; propuesta de valor + slogan; servicios; SimbioSphere con CTA de alta/sugerencia; desarrollo a medida; visión de IA futura (sin promesas); CTA final; footer legal/contacto.
- **SimbioSphere:** qué es, para qué tipos de negocio, capacidades cotejadas con [05](05-integracion-simbiosphere.md), alta/login y sugerencia; precio DEMO sin anunciar pagos por módulo/sede.
- **Desarrollo a medida:** landing/web corporativa/app web/otros, proceso y CTA a formulario con estado del borrador.
- **Sobre nosotros:** historia y valores CMS; equipo con foto, nombre, rol, bio, enlaces y alt por idioma.
- **Contacto:** motivo/mensaje, consentimiento, adjunto y nota de audio; canales `wa.me`/`mailto:` separados de la bandeja.
- **Cuenta:** resumen de solicitudes; tabla filtrable; detalle con historial/mensajes PUBLIC, presupuesto/acciones; panel SimbioSphere con estado. Notas INTERNAL nunca se muestran.
- **Gestión:** dashboard denso de carga/SLA; listados con filtros y paginación de servidor; detalle con timeline, responsable/colaboradores, presupuesto, conversación y paneles por URL; inbox con hilo, asignación, etiquetas, papelera; CMS con preview/historial; visor ADMIN de errores por source/severidad/estado con detalle saneado.

## Navegación por rol
| Rol | Elementos principales |
|---|---|
| Anónimo | Inicio, SimbioSphere, Servicios, Sobre nosotros, Contacto; Acceder/Crear cuenta; selector de idioma, tema |
| CLIENT | Inicio de cuenta, Mis solicitudes, Nueva solicitud, Mi SimbioSphere, Perfil, Datos/privacidad, Notificaciones, Salir |
| EMPLOYEE | Panel, Solicitudes, Bandeja, Perfil; solo rutas cubiertas por permisos asignados |
| ADMIN | Menú empleado + Clientes, Trabajadores, CMS, Auditoría, Aprovisionamientos, Errores y configuración autorizada |

Cada módulo aporta entradas mediante `shared.spi.NavContribution`; `config` compone el árbol por rol. La visibilidad del enlace es solo UI: cada endpoint vuelve a validar rol, permiso y propiedad.

### Comportamiento responsive/accesible del menú
- Medir anchura total de enlaces y contenedor con `ResizeObserver` tras `document.fonts.ready`; al no caber, todas las opciones van al botón hamburguesa a la derecha (no se ocultan enlaces sueltos).
- Sin JS: fallback CSS muestra botón/drawer navegable. Cabecera sticky; selector de idioma, conmutador de tema, acceso/usuario y enlace de acceso a SimbioSphere.
- Botón con `aria-expanded`/`aria-controls`; abrir/cerrar con teclado, Esc y clic fuera; focus trap, bloqueo de scroll y restauración del foco; `aria-current` para activo.

## Tokens y contraste WCAG 2.x
Fuente provisional de imágenes y logos indicada por el owner: `C:\Users\x455035\1Git\AprendiendoIA\Symbio\symbio-frontend\public\resources\img`. Revisar autorización/licencia y adecuación de cada recurso antes de incorporarlo; conservar fallback estático si no hay assets aprobados.
Los valores y ratios proceden del cálculo del brief común. Texto normal ≥4,5:1; bordes/controles no textuales ≥3:1. Lima como texto sobre fondo claro está prohibido; el botón primario lima usa texto oscuro y borde oliva.

| Tema/uso | Foreground / background | Ratio | Criterio |
|---|---|---:|---|
| Oscuro: texto | `#f5f5e8` / `#232310`, `#2a2a12`, `#333318` | 14,48 / 13,28 / 11,74 | AAA |
| Oscuro: texto muted | `#c4c4ab` / `#232310`, `#333318` | 8,97 / 7,27 | AA |
| Oscuro: acento/enlace | `#d0df05` / `#232310`, `#333318` | 10,79 / 8,75 | AA; no oliva como texto |
| Oscuro: botón primario | `#bcc907` con texto `#232310` | 8,71 | AA |
| Oscuro: borde | `#7d7d52` / `#232310`, `#333318` | 3,73 / 3,03 | No textual |
| Oscuro: estados | danger `#ff8f84`; success `#8fd694`; warning `#ffd166`; info `#7cc4ff` sobre `#232310` | 7,21 / 9,26 / 11,04 / 8,49 | AA |
| Claro: texto | `#232310` / `#f5f5e8`, `#ffffff`, `#ebebd9` | 14,48 / 15,92 / 13,20 | AAA |
| Claro: texto muted | `#505038` / `#f5f5e8`, `#ffffff` | 7,51 / 8,25 | AA |
| Claro: acento/enlace/foco | `#3e4202` / `#f5f5e8`, `#ffffff` | 9,63 / 10,58 | AA; contraste oliva/lima 5,79 |
| Claro: botón primario | `#bcc907` con texto `#232310` | 8,71 | AA; lima como texto sola: 1,66 / 1,83 (no usar) |
| Claro: borde | `#6f6f4c` / `#f5f5e8`, `#ffffff` | 4,71 / 5,18 | No textual |
| Claro: estados | danger `#b3261e`; success `#1b6e2a`; warning `#7a4b00`; info `#0b5394` sobre blanco | 5,95 / 5,77 / 6,74 / 7,13 | AA |

Variables semánticas: `--color-bg`, `--color-surface`, `--color-surface-alt`, `--color-text`, `--color-text-muted`, `--color-accent-text`, `--color-primary`, `--color-on-primary`, `--color-border`, `--color-focus`, `--color-danger`, `--color-success`, `--color-warning`, `--color-info`. Tailwind mapea tokens; no se escriben colores directos en plantillas.

## Tema, tipografía y componentes
- Tema inicial por `prefers-color-scheme`, sobreescrito por la selección guardada en `localStorage` y la cookie técnica `sp_theme` para usuarios anónimos y autenticados. La persistencia en perfil queda diferida al módulo de Identidad; `shared.spi.ThemePreference` conserva el punto de integración opcional. Anti-FOUC inline en `<head>` con nonce CSP y `<meta name="color-scheme" content="dark light">`; `color-scheme` adapta inputs/scrollbars.
- Tipografía: `system-ui, sans-serif` para marca, titulares y cuerpo. No se distribuyen fuentes web ni se cargan fuentes desde CDN. El menú recalcula el desbordamiento al completar `document.fonts.ready`; se verifica el reflow con las métricas del sistema.
- Componentes base: formularios con errors asociados, tablas/paginación de servidor, toasts en live region, confirmación destructiva, selector locale/tema, drawer, indicador de estados y enlaces de descarga con nombre accesible.
- HTMX para filtros/paginación/mutaciones parciales con CSRF, estados de carga y error; mejora progresiva sin JavaScript. Alpine CSP local y JS mínimo por componente.

## i18n
- Bundles por módulo `i18n/<module>/messages_{es,en}.properties`; gate compara claves. Primera visita usa `Accept-Language`; selector conserva página equivalente; público usa `/{lang}` con canonical/hreflang; privado usa preferencia perfil → cookie `sp_lang` → header.
- Slugs de CMS por locale, fallback publicado y 301 al cambiar slug; no mezclar idioma de contenido con locale de datos personales.
- Correos en idioma del destinatario; formatos por locale; persistencia UTC, presentación `Europe/Madrid`. CMS define política explícita de fallback y detecta traducción pendiente.

## Hero con vídeo
Logo estático optimizado es el LCP. Escritorio con `(hover: hover) and (pointer: fine)`: reproducir al pasar/foco de teclado y pausar al salir; pantalla táctil: autoplay `muted`, `playsinline` tras carga sin bloquear LCP, con botón de pausa. Respetar `prefers-reduced-motion` y `Save-Data`; si autoplay falla, mostrar póster. Máximo 5 s o control de pausa WCAG 2.2.2, sin audio, MP4 H.264 + WebM, objetivo ~1,5 MB y `preload="none"/"metadata"`. Vídeo y variantes de logo son dependencias del cliente; sin assets, se mantiene hero estático.

## Accesibilidad
WCAG 2.2 AA: teclado completo, skip link, landmarks, foco visible, errores vinculados, contraste en ambos temas, objetivos táctiles ≥44 px, zoom, `prefers-reduced-motion`, captions/texto alternativo y controles de audio. axe-core en E2E y auditoría manual HRD-004. Matriz visual: claro/oscuro × móvil/escritorio × ES/EN en páginas críticas.
