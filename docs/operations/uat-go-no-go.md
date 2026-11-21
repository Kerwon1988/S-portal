# UAT y decisión go/no-go

HRD-014 no autoriza un despliegue. Estado actual: **NO-GO** por evidencias externas/legales no ejecutadas.

| Gate | Estado | Owner/acción |
|---|---|---|
| HRD-001 ASVS/ZAP | PREPARADO_NO_EJECUTADO | Seguridad: host y scope autorizados |
| HRD-002 CSP/cabeceras | preparado, test focal pendiente | Config: ejecutar tests y browser review |
| HRD-003 carga/Lighthouse | PREPARADO_NO_EJECUTADO | Arquitectura/Ops: baseline/aprobación |
| HRD-004 WCAG | PREPARADO_NO_EJECUTADO | UX/accesibilidad: revisión humana |
| HRD-005 backup/restore | PREPARADO_NO_EJECUTADO | Ops: RPO/RTO, backup y restore autorizado |
| HRD-007 monitorización | preparado, no verificado | Ops: scrape/alerta/runbook |
| HRD-008 RGPD | PREPARADO_NO_EJECUTADO | Legal/DPO: RAT, encargados, retención |
| HRD-009 DNS email | PREPARADO_NO_EJECUTADO | Operación: dominio/proveedor/prueba |
| HRD-010 secretos | PREPARADO_NO_EJECUTADO | Plataforma: provider/rotación |
| HRD-011 release/rollback | PREPARADO_NO_EJECUTADO | Ops: staging/smoke/ensayo |
| HRD-012 E2E noche | POSTPONED | QA: servidor/browser/fixtures |
| HRD-013 licencias | PREPARADO_NO_EJECUTADO | Compras/legal: assets/SQL |
| Aprobación negocio | pendiente | producto |
| Aprobación operación | pendiente | operaciones |
| Aprobación seguridad/privacidad | pendiente | seguridad/DPO |

Resultado obligatorio: NO-GO mientras exista blocker, gate pendiente obligatorio o owner externo sin firma. Registrar versión, fecha, nombres/rol, excepciones aprobadas con caducidad y evidencia redactada.
