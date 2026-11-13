# F6 — Endurecimiento y salida (seguridad, rendimiento, operación y privacidad): tareas

- Objetivo: cerrar gates y evidencias de producción antes de UAT/salida (hito M6; ver [roadmap](../10-roadmap-backlog.md)).
- Módulo(s) owner: transversal; tareas coordinadas con `config`, `infra`, `legal` y owners funcionales.

### HRD-001 — Revisión ASVS L2 y ZAP baseline con correcciones
- Fase / épica / módulo owner: F6 / HRD / transversal
- Tipo: test
- Depende de: E2E-002, E2E-003, E2E-004
- Descripción: Cubre: RNF-SEC-01, RNF-SEC-09; ejecuta checklist OWASP ASVS L2 y ZAP baseline autorizado contra entorno de preproducción.
- Ficheros/paquetes a crear o modificar (aproximado): checklist/report `docs/security/`, workflow ZAP, issues de remediación y evidencias sin PII.
- Impacto modular: revisión cross-module de APIs/UI; hallazgos se asignan al owner; sin cambiar límites ni añadir tablas. Ciclo: ninguno.
- UI: recorridos autenticados/anónimos y vistas críticas con rol por matriz.
- Seguridad: scope solo host de test propio, credenciales efímeras y rate limitado; no escanear DEMO ni terceros.
- Criterios de aceptación (Given/When/Then):
  - Given build y entorno aprobado When corre ASVS/ZAP Then genera reporte reproducible con severidad, owner y evidencia redactada.
  - Given hallazgo blocker/high abierto When se evalúa release Then gate bloquea o registra excepción aprobada con mitigación/fecha.
- Tests: TC-HRD-001: SEC (ASVS), IT (ZAP baseline), evidencia E2E vinculada.
- Documentación a actualizar: DIARIO + INVENTARIO; informe en 08/11
- Estimación: M (≤ 1 d)
- Riesgos: escaneo activo puede alterar datos; usar preproducción aislada y datos sintéticos.

### HRD-002 — CSP en report-only → enforce y auditoría de cabeceras
- Fase / épica / módulo owner: F6 / HRD / config
- Tipo: test
- Depende de: FND-009, E2E-004
- Descripción: Cubre: RNF-SEC-03; analiza reportes, elimina violaciones y cambia CSP a enforce con nonce sin `unsafe-*`.
- Ficheros/paquetes a crear o modificar (aproximado): `config/security/CspPolicy`, cabeceras, informe de violaciones y tests de regresión.
- Impacto modular: política transversal `config`; los módulos declaran recursos permitidos, sin dependencia nueva. Sin tablas/eventos. Ciclo: ninguno.
- UI: revisar HTMX/Alpine CSP, anti-FOUC, audio/medios y errores en todos los layouts.
- Seguridad: nonce por request, `frame-ancestors`, HSTS, nosniff, Referrer/Permissions-Policy; no usar `unsafe-inline/eval`.
- Criterios de aceptación (Given/When/Then):
  - Given suite E2E/errores CSP corregidos When se despliega enforce Then páginas y flujos pasan sin violaciones no justificadas.
  - Given script inline no autorizado When se sirve Then navegador lo bloquea y test verifica ausencia de bypass.
- Tests: TC-HRD-002: WT (cabeceras/nonce), E2E (flujos), SEC (fixtures violación bloqueada).
- Documentación a actualizar: DIARIO + INVENTARIO; 09 cabeceras
- Estimación: S (≤ 0,5 d)
- Riesgos: actualizar policy antes de inventariar inline legítimo rompe UX; transición basada en reportes.

### HRD-003 — Pruebas de carga y presupuestos de rendimiento
- Fase / épica / módulo owner: F6 / HRD / transversal
- Tipo: test
- Depende de: WEB-007, E2E-004
- Descripción: Cubre: RNF-PERF-01, RNF-PERF-03; mide concurrencia, latencia, consultas y recursos frente a umbrales acordados.
- Ficheros/paquetes a crear o modificar (aproximado): scripts carga `tests/performance/`, escenarios anon/auth, dashboards y reporte baseline.
- Impacto modular: cargas sobre rutas públicas/gestión; perf findings vuelven al owner request/content; sin cambios de contrato. Ciclo: ninguno.
- UI: Lighthouse móvil en páginas públicas y prueba de navegación repetida.
- Seguridad: entorno aislado, datos sintéticos, límites de concurrencia; no cargar servicio DEMO sin autorización expresa.
- Criterios de aceptación (Given/When/Then):
  - Given perfil de carga documentado When corre prueba Then reporta p95/p99, throughput, errores, CPU/DB y CWV contra objetivos.
  - Given tasa de error o latencia por encima del umbral acordado When CI procesa resultados Then gate falla y conserva artefacto sin PII.
- Tests: TC-HRD-003: PERF (carga/API/SQL), Lighthouse y baseline comparativo.
- Documentación a actualizar: DIARIO + INVENTARIO; 08 presupuesto de rendimiento
- Estimación: M (≤ 1 d)
- Riesgos: SLA/carga real pendientes; umbral final debe aprobarse antes de interpretar resultado.

### HRD-004 — Auditoría de accesibilidad WCAG 2.2 AA completa
- Fase / épica / módulo owner: F6 / HRD / transversal
- Tipo: test
- Depende de: UIK-009, E2E-004
- Descripción: Cubre: RNF-A11Y-01; complementa axe con revisión manual teclado, lector, contraste y movimiento en flujos críticos.
- Ficheros/paquetes a crear o modificar (aproximado): checklist WCAG, reporte de hallazgos, fixtures visuales y remediaciones asignadas.
- Impacto modular: auditoría de plantillas de todos los owners; fixes respetan APIs y ownership. Sin tablas/eventos. Ciclo: ninguno.
- UI: público, registro, CMS, cliente, empleado, contacto/audio y errores; tema × locale × viewport.
- Seguridad: revisar foco/labels de errores en pantallas protegidas sin grabar datos personales.
- Criterios de aceptación (Given/When/Then):
  - Given recorridos esenciales When se prueban solo teclado/lector Then no hay bloqueos y foco/estado se anuncia.
  - Given combinación claro/oscuro When se mide contraste y reducción de movimiento Then ratios AA y controles operan sin animación forzada.
- Tests: TC-HRD-004: A11Y (axe + manual), VIS (matriz), E2E teclado.
- Documentación a actualizar: DIARIO + INVENTARIO; evidencias en 08
- Estimación: M (≤ 1 d)
- Riesgos: axe no certifica conformidad; registrar revisión humana y excepciones justificadas.

### HRD-005 — Documentación de backup y restauración
- Fase / épica / módulo owner: F6 / HRD / infra
- Tipo: infra
- Depende de: FND-005, HRD-006
- Descripción: Cubre: RNF-OPS-02; documenta backup cifrado, retención, procedimiento de restauración y alertas. No requiere acceso ni simulacro SQL Server como criterio de desarrollo.
- Ficheros/paquetes a crear o modificar (aproximado): runbook `docs/operations/backup-restore.md`, configuración/plantilla y alertas documentadas; sin evidencia de restore real.
- Impacto modular: operación de SQL Server y esquema completo; sin cambiar owners de datos. Ciclo: ninguno.
- UI: n/a (operación).
- Seguridad: cifrar backup, acceso mínimo, almacenar fuera de host y destruir copia de prueba al vencer.
- Criterios de aceptación (Given/When/Then):
  - Given una estrategia de backup aprobada When se documenta Then el runbook especifica cifrado, permisos, retención, pasos de restauración y campos pendientes de RPO/RTO sin inventarlos.
  - Given una restauración fallida o credencial ausente When se documenta el procedimiento Then quedan descritos la alerta y el escalado sin exponer secretos.
- Tests: TC-HRD-005: revisión documental del runbook y SEC (cifrado/permisos de configuración). No se requiere IT ni simulacro de restauración SQL Server.
- Documentación a actualizar: DIARIO + INVENTARIO; README/09 operación; ADR-009
- Estimación: M (≤ 1 d)
- Riesgos: RPO/RTO y la restauración real no se validan en este plan por falta de acceso; documentar como limitación operativa, no como gate de desarrollo.

### HRD-006 — Dockerfile multi-stage, compose de producción y TLS
- Fase / épica / módulo owner: F6 / HRD / infra
- Tipo: infra
- Depende de: FND-008, FND-007
- Descripción: Cubre: RNF-OPS-03; prepara imagen multi-stage no-root y topología de despliegue tras reverse proxy TLS.
- Ficheros/paquetes a crear o modificar (aproximado): `Dockerfile`, compose/manifest prod, configuración proxy/healthchecks y SBOM.
- Impacto modular: infraestructura del monolito; una imagen/BD, sin distribuir módulos en servicios. Sin APIs/eventos/tablas nuevas.
- UI: assets compilados y rutas públicas bajo base URL definida por entorno.
- Seguridad: usuario no-root, imagen fijada, secretos externos, puertos internos no públicos, TLS/HSTS y filesystem mínimo.
- Criterios de aceptación (Given/When/Then):
  - Given build reproducible When escanea imagen Then ejecuta no-root, no incluye toolchain/secreto y healthcheck responde.
  - Given perfil prod sin secret/config requerida When arranca Then falla closed y no expone SQL Server/Actuator públicamente.
- Tests: TC-HRD-006: IT (imagen/health), SEC (Dockerfile/secrets/puertos), smoke HTTPS.
- Documentación a actualizar: DIARIO + INVENTARIO; README «Despliegue»
- Estimación: M (≤ 1 d)
- Riesgos: proxy/plataforma aún por decidir; compose no equivale a estrategia HA.

### HRD-007 — Monitorización: dashboards y alertas
- Fase / épica / módulo owner: F6 / HRD / infra
- Tipo: infra
- Depende de: FND-010, NTF-003, SIM-010
- Descripción: Cubre: RNF-OPS-01; publica métricas de aprovisionamiento, cola email, rate limit, EPR y salud ClamAV.
- Ficheros/paquetes a crear o modificar (aproximado): dashboards Prometheus, reglas alertas, runbooks y tests de métricas.
- Impacto modular: observabilidad consume métricas agregadas de módulos; no consulta tablas de negocio. Sin ciclos.
- UI: dashboards de operación con estado por módulo, no datos de usuario.
- Seguridad: endpoint Actuator protegido; métricas sin etiquetas con email/publicId/tenant slug y acceso restringido.
- Criterios de aceptación (Given/When/Then):
  - Given aprovisionamientos FAILED antiguos o cola sobre umbral When se evalúan reglas Then alerta incluye correlation ID y runbook.
  - Given etiqueta que contiene PII When corre test de métricas Then falla cardinalidad/privacidad gate.
- Tests: TC-HRD-007: IT (Prometheus scrape/alert), SEC (labels sin PII), smoke dashboard.
- Documentación a actualizar: DIARIO + INVENTARIO; runbooks 09
- Estimación: M (≤ 1 d)
- Riesgos: Actuator puerto separado/protegido es decisión por reconciliar; no publicar `/env`.

### HRD-008 — RGPD documental: RAT, encargados y retención verificada
- Fase / épica / módulo owner: F6 / HRD / docs
- Tipo: docs
- Depende de: LGL-003, USR-004
- Descripción: Cubre: RNF-PRV-02, RNF-PRV-04; completa RAT, encargados, finalidades, transferencias y plazos aprobados por responsable.
- Ficheros/paquetes a crear o modificar (aproximado): `docs/privacy/RAT.md`, matriz de encargados, tabla retención y acta de aprobación.
- Impacto modular: documento transversal con owner por tratamiento; no agrega API/tabla ni cambia política automáticamente. Ciclo: ninguno.
- UI: revisar formularios/consentimientos visibles y enlaces legales vigentes.
- Seguridad: minimizar categorías, controlar acceso documental y excluir credenciales/datos reales.
- Criterios de aceptación (Given/When/Then):
  - Given proveedor hosting/email/captcha/antivirus When responsable valida expediente Then RAT identifica encargado, datos, región y contrato.
  - Given plazo aún no aprobado When se publica documentación Then queda marcado propuesta/pendiente y no se ejecuta purga irreversible.
- Tests: TC-HRD-008: revisión documental trazable, UT (tabla retención↔jobs), evidencia de aprobación.
- Documentación a actualizar: DIARIO + INVENTARIO; enlaces a privacidad
- Estimación: M (≤ 1 d)
- Riesgos: obligaciones dependen de jurisdicción/contrato; requiere validación DPO/legal.

### HRD-009 — DNS de email (SPF/DKIM/DMARC) y prueba de entregabilidad
- Fase / épica / módulo owner: F6 / HRD / infra
- Tipo: infra
- Depende de: NTF-003, HRD-006
- Descripción: Cubre: RF-NOT-04; configura y valida SPF, DKIM y DMARC del dominio de envío aprobado.
- Ficheros/paquetes a crear o modificar (aproximado): runbook DNS/mail, configuración proveedor, reporte de entrega y alertas.
- Impacto modular: DNS/proveedor externo de notification; sin tablas ni llamadas directas de negocio. Ciclo: ninguno.
- UI: n/a (entregabilidad de email).
- Seguridad: política DMARC acordada, DKIM rotado; logs y reportes sin direcciones completas.
- Criterios de aceptación (Given/When/Then):
  - Given dominio aprobado When se publica DNS y se envía mensaje de prueba Then SPF/DKIM/DMARC pasan y Mailpit sigue funcional en local.
  - Given dominio/proveedor aún no decidido When se prepara release Then gate queda pendiente y no se inventan registros/valores.
- Tests: TC-HRD-009: IT (SMTP/deliverability), SEC (DNS alignment), reporte externo guardado.
- Documentación a actualizar: DIARIO + INVENTARIO; 09 correo
- Estimación: S (≤ 0,5 d)
- Riesgos: proveedor/dominio faltante impide validar; cerrar 13 antes de salida.

### HRD-010 — Gestión de secretos de producción y rotación de claves
- Fase / épica / módulo owner: F6 / HRD / infra
- Tipo: infra
- Depende de: FND-007, HRD-006
- Descripción: Cubre: RNF-SEC-09, RNF-SEC-05; conecta gestor/plataforma de secretos y define rotación para DB, SMTP, HMAC y cifrado.
- Ficheros/paquetes a crear o modificar (aproximado): configuración de deployment, policy de secret manager, runbook de rotación y `SecretScan`.
- Impacto modular: secretos consumidos por `config`/adapters vía propiedades; no se guardan en tablas ni repositorio. Sin ciclos.
- UI: n/a (operación).
- Seguridad: mínimos privilegios, doble clave en rotación, masking, auditoría de acceso y revocación probada.
- Criterios de aceptación (Given/When/Then):
  - Given secreto rotado When despliega nueva versión Then servicio acepta transición y revoca clave anterior tras periodo aprobado.
  - Given secreto literal en git/image/log When corre scan Then pipeline bloquea release y rota credencial expuesta.
- Tests: TC-HRD-010: IT (binding/rotación), SEC (scan/logs/policies), evidencia de revocación.
- Documentación a actualizar: DIARIO + INVENTARIO; README/09 operación
- Estimación: M (≤ 1 d)
- Riesgos: gestor depende de plataforma no elegida; dejar provider adapter abstracto hasta decisión.

### HRD-011 — Runbook de release/rollback y smoke tests post-despliegue
- Fase / épica / módulo owner: F6 / HRD / infra
- Tipo: docs
- Depende de: HRD-006, HRD-005
- Descripción: Cubre: RNF-OPS-03; documenta secuencia de release, migración forward-only, rollback compatible y smoke tests.
- Ficheros/paquetes a crear o modificar (aproximado): `docs/operations/release-rollback.md`, scripts smoke no destructivos y checklist.
- Impacto modular: operación del monolito y una BD; incluye health/readiness e integración externa en modo controlado. Sin ciclos.
- UI: smoke público/login/contacto/gestion sin datos reales; verificar rutas/locale/tema.
- Seguridad: no imprimir secretos; rollback no revierte migración destructiva sin plan aprobado.
- Criterios de aceptación (Given/When/Then):
  - Given release candidato When se sigue runbook en staging Then health/readiness, endpoints clave y migraciones quedan verificadas.
  - Given smoke falla When se activa rollback Then se restaura versión compatible y se registra decisión/impacto sin perder datos.
- Tests: TC-HRD-011: IT (smoke), ensayo documental rollback, SEC (salida sin secretos).
- Documentación a actualizar: DIARIO + INVENTARIO; README «Release»
- Estimación: M (≤ 1 d)
- Riesgos: estrategia/hosting no decidido; procedimiento depende de ADR-006 y compatibilidad DB.

### HRD-012 — Suite E2E nocturna y matriz visual completa
- Fase / épica / módulo owner: F6 / HRD / test
- Tipo: test
- Depende de: E2E-001, E2E-002, E2E-003, E2E-004
- Descripción: Cubre: RNF-TST-03, RNF-TST-04; programa recorridos Playwright y capturas visuales nocturnas contra build fijado.
- Ficheros/paquetes a crear o modificar (aproximado): workflow nocturno, matriz fixtures, artefactos de reporte y baseline versionado.
- Impacto modular: verificación cross-module por URL/API; sin tablas productivas ni dependencia nueva. Ciclo: ninguno.
- UI: claro/oscuro × móvil/escritorio × ES/EN en páginas clave.
- Seguridad: entorno de prueba aislado, cuentas sintéticas, artefactos redactados y retención limitada.
- Criterios de aceptación (Given/When/Then):
  - Given build desplegado en test When se ejecuta suite nocturna Then corre 4 E2E, axe y matriz visual y publica resultado.
  - Given regresión visual o fallo de autorización When falla gate Then señala viewport/locale/rol y bloquea promoción.
- Tests: TC-HRD-012: E2E (Playwright), VIS (diff), A11Y (axe), SEC (roles/IDOR).
- Documentación a actualizar: DIARIO + INVENTARIO; README «CI»
- Estimación: M (≤ 1 d)
- Riesgos: snapshots inestables por fonts/time; fijar navegador, clock y fuentes.

### HRD-013 — Revisión de licencias (assets y SQL Server)
- Fase / épica / módulo owner: F6 / HRD / docs
- Tipo: docs
- Depende de: WEB-002
- Descripción: Cubre RNF-DB-04; inventaría licencias de assets incluidos y edición SQL Server antes de producción. La tipografía web queda fuera del alcance al usarse fuentes del sistema.
- Ficheros/paquetes a crear o modificar (aproximado): `docs/compliance/licenses.md`, SBOM, atribuciones y evidencia de edición/terms SQL.
- Impacto modular: documento transversal, sin APIs/eventos/tablas. Ciclo: ninguno.
- UI: registrar licencia/autoría de imágenes y vídeo incluidos.
- Seguridad: no incorporar assets sin derechos; verificar condiciones de redistribución, TDE y backups según edición.
- Criterios de aceptación (Given/When/Then):
  - Given cada dependencia/asset When se revisa inventario Then constan licencia, origen, uso y evidencia verificable.
  - Given un asset incluido o SQL edition sin permiso/feature confirmados When se aprueba release Then sustituye o bloquea, sin afirmar precios.
- Tests: TC-HRD-013: revisión documental/SBOM, gate de archivos de licencia faltantes.
- Documentación a actualizar: DIARIO + INVENTARIO; ADR-006/13
- Estimación: S (≤ 0,5 d)
- Riesgos: licencia/coste requiere fuente oficial y aprobación; nunca estimar en el plan sin evidencia.

### HRD-014 — UAT y checklist de salida a producción
- Fase / épica / módulo owner: F6 / HRD / transversal
- Tipo: test
- Depende de: HRD-001, HRD-002, HRD-003, HRD-004, HRD-005, HRD-007, HRD-008, HRD-009, HRD-010, HRD-011, HRD-012, HRD-013
- Descripción: Cubre: RNF-OPS-03; reúne aceptación funcional, seguridad, operación, privacidad, rendimiento, restauración y owners de dependencias externas.
- Ficheros/paquetes a crear o modificar (aproximado): checklist UAT firmable, matriz evidencias y decisión go/no-go en `docs/operations/`.
- Impacto modular: gate transversal; no cambia módulos/datos/contratos. Ciclo: ninguno.
- UI: validar recorridos por rol, locales, tema y viewport con representantes de negocio.
- Seguridad: ningún blocker sin excepción aprobada; secretos/PII no incluidos en evidencia ni capturas.
- Criterios de aceptación (Given/When/Then):
  - Given todas las evidencias obligatorias y dependencias listas When owner firma UAT Then checklist registra aprobado, versión y fecha.
  - Given backup/restore fallido, blocker abierto o API DEMO no disponible When se decide release Then resultado es NO-GO con responsable/acción.
- Tests: TC-HRD-014: UAT (firmas por rol), smoke final, revisión de gates CIs.
- Documentación a actualizar: DIARIO + INVENTARIO; README/09 release
- Estimación: S (≤ 0,5 d)
- Riesgos: UAT no sustituye pruebas automatizadas; salida depende de aprobación de negocio/operación.
