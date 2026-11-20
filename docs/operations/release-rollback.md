# Release y rollback

## Gate previo

No se autoriza release con blocker High/Critical, gate focal fallido, migración no revisada, secreto/PII en artefactos o dependencia externa pendiente. La lista de evidencias vive en `uat-go-no-go.md`.

## Release

1. Fijar imagen por digest, versión, changelog, owner y SHA de configuración sin secretos.
2. Revisar migraciones como forward-only; prohibido usar `flyway clean` fuera de BD desechable.
3. Cifrar/validar configuración externa y credenciales; comprobar least privilege.
4. Backup aprobado y registro de destino/RPO; no imprimir credenciales.
5. Desplegar con health/readiness; ejecutar smoke público/login/contacto/gestión con datos sintéticos.
6. Verificar métricas, cola, logs redactados y errores durante ventana de observación.
7. Registrar decisión y responsable. HRD-014 no concede autorización de despliegue.

## Rollback

- Parar promoción al primer fallo de smoke, salud, migración o seguridad.
- Volver a imagen anterior compatible; no revertir automáticamente el esquema.
- Si una migración no es backward-compatible, mantener aplicación nueva o activar plan de reversión aprobado por separado.
- Confirmar integridad/health, registrar impacto y escalado; nunca borrar datos para forzar compatibilidad.

## Estado

Ensayo documental, staging y rollback real: **PREPARADO_NO_EJECUTADO** por falta de plataforma/credenciales/autorización. `scripts/smoke-release.mjs` está preparado para una URL HTTPS autorizada.
