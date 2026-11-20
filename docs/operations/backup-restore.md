# Backup y restauración

Owner: infra/operaciones. Este runbook no certifica SQL Server ni afirma un restore real.

## Política requerida

- Copias cifradas en repositorio separado del host y con control de acceso mínimo.
- Backup completo + diferenciales/log según RPO aprobado; frecuencia y retención siguen **PENDIENTES DE APROBACIÓN**.
- RPO/RTO, región, edición y custodio son decisiones pendientes; no se inventan.
- Claves de cifrado de backup separadas de la BD y sujetas a rotación/auditoría.
- Aplicar principio de mínimo privilegio; ninguna credencial en comandos, logs o tickets.

## Restauración

1. Declarar incidente y preservar la evidencia; identificar versión de aplicación y LSN/fecha objetivo.
2. Obtener aprobación del owner operativo y usar cuenta temporal con mínimo privilegio.
3. Crear destino aislado; nunca restaurar sobre la instancia activa sin plan aprobado.
4. Restaurar backup completo, diferenciales y logs ordenados, validando checksums.
5. Ejecutar `DBCC CHECKDB`, controles de fila/esquema sin PII y compatibilidad de la versión Flyway aprobada.
6. Arrancar el candidato en mantenimiento, ejecutar health/readiness y smoke sintético.
7. Rotar/revocar credenciales usadas y borrar la copia temporal al vencer la ventana aprobada.

Ante fallo o credencial ausente: detener, registrar correlation ID del procedimiento, alertar a Operaciones y DPO si existe exposición, y no continuar por atajos.

## Evidencia

- TC-HRD-005 documental: preparado.
- Restore real SQL Server: **PREPARADO_NO_EJECUTADO**; requiere plataforma, backup cifrado, RPO/RTO y autorización.
