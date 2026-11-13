# F1 — Visor ADMIN del inventario de errores

- Objetivo: permitir inspección y resolución operativa sin exponer errores a clientes ni depender de SQL.
- Módulo(s) owner: `config.errortracking` y `config.web`; consumes identidad solo por controles de seguridad.

### ERR-004 — Visor ADMIN del inventario de errores y resolución
- Fase / épica / módulo owner: F1 / ERR / config
- Tipo: feat
- Depende de: ERR-001, ERR-002, ERR-003, IAM-012, UIK-008
- Descripción: Cubre: RF-ERR-02; ofrece `/gestion/errores` con filtros por fuente, severidad y estado, detalle resumido y acciones de resolución/ignorado.
- Ficheros/paquetes a crear o modificar (aproximado): `config.web.ErrorInventoryController`, vistas/fragmentos, DTO resumido y bundles ES/EN.
- Impacto modular: `config` consulta su servicio/inventario local; sin tabla SQL ni lectura de módulos. Ciclo: ninguno.
- UI: listado paginado y detalle accesibles, claro/oscuro; muestra source, tipo, hash, primera/última ocurrencia y conteo.
- Seguridad: ADMIN únicamente; denegar CLIENT/EMPLOYEE; sanitizar stack, ruta y notas de resolución; no ofrecer borrado masivo ni secretos.
- Criterios de aceptación (Given/When/Then):
  - Given ADMIN autenticado When filtra por source/severidad/estado Then ve solo resumen permitido y puede marcar RESOLVED con nota saneada.
  - Given rol no ADMIN o storage no disponible When accede/actualiza Then recibe 403/estado degradado sin revelar archivo ni datos internos.
- Tests: TC-ERR-004: WT (role/filter/resolve), SEC (IDOR/redacción), A11Y, IT (storage unavailable).
- Documentación a actualizar: DIARIO + INVENTARIO; README operaciones/errores
- Estimación: M (≤ 1 d)
- Riesgos: inventario puede crecer sin límite; mostrar disponibilidad, aplicar retención/cap acordados y alertar por volumen.
