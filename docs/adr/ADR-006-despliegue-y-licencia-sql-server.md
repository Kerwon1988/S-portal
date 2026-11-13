# ADR-006 — Despliegue y licencia de SQL Server

- Estado: Borrador
- Fecha: 2026-11-10

## Contexto
D09 fija SQL Server 2022 como DB. Se necesita un despliegue simple de un monolito/una BD, entornos separados, backups restaurables y elección de edición/licencia sin inventar costes ni características.

## Decisión recomendada
Proponer contenedor multi-stage tras reverse proxy TLS, SQL Server 2022 y `docker compose` para entornos Docker habilitados con SQL Server, Mailpit y ClamAV. H2 es el único motor de pruebas automatizadas; Docker/SQL Server no se requieren para los gates de desarrollo y la compatibilidad productiva queda sin verificar. El tag/CU/digest y EULA se confirman al preparar el despliegue. Edición productiva (Express/Standard/Azure SQL), almacenamiento, TDE, límites/capacidad, costes y plataforma se deciden con fuentes oficiales. No exponer SQL ni management port públicamente.

## Alternativas
- Express: alternativa de bajo coste, pero confirmar límites y características (incluido TDE) en documentación oficial.
- Standard: considerar si requisitos de capacidad/funciones lo requieren; no dar precio sin cotización vigente.
- Azure SQL: valorar servicio gestionado, región, residencia y coste con arquitectura/plataforma.
- Kubernetes/OpenShift u otra plataforma: no asumir hasta que el owner de despliegue confirme entorno.

## Consecuencias
Flyway mantiene un único árbol SQL Server; las pruebas H2 usan esquema generado y no ejecutan ni certifican migraciones SQL Server. No se requiere prueba de restauración SQL Server para los gates de desarrollo. Backup cifrado fuera del host; Docker no sustituye HA/RPO/RTO. Tag `:2022` del prompt queda VERIFICAR antes del despliegue, no se presupone disponible.

## Verificaciones pendientes
- HRD-006: tag/CU y EULA compatibles con la plataforma de despliegue.
- FND-005/006: configuración Compose validada estáticamente; las migraciones se mantienen forward-only y no se ejecutan en SQL Server como gate de desarrollo.
- HRD-005/006/011/013: documentar procedimiento de backup, despliegue, rollback, licencia y edición; la prueba real de restauración SQL Server no es requisito de desarrollo.
