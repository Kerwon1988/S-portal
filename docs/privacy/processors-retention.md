# Encargados y retención

## Encargados

| Servicio/categoría | Proveedor/región/contrato | Transferencia | Estado |
|---|---|---|---|
| Hosting/SQL Server | pendiente | pendiente | NO APROBADO |
| SMTP transaccional | pendiente | pendiente | NO APROBADO |
| DNS/email | pendiente | pendiente | NO APROBADO |
| Anti-bot | local en implementación actual | n/a | legal a validar |
| Antivirus | ClamAV local; despliegue/operador pendiente | n/a | pendiente |
| DEMO/SimbioSphere | integración separada | solo backend M2M; acuerdo pendiente | SIM externo pendiente |

## Retención

No ejecutar purgas irreversibles con plazo no aprobado. `notification` conserva un valor técnico configurable; `messaging` está bloqueado por defecto al no existir aprobación. Los jobs de owner deben trazarse contra esta matriz cuando legal/DPO firme cada plazo.

Aprobación legal/DPO: **PREPARADO_NO_EJECUTADO**. No se ejecuta purga ni se afirma cumplimiento RGPD.
