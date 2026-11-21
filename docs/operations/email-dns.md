# DNS de correo y entregabilidad

El dominio/proveedor no están aprobados. No se inventan registros, claves ni dominios.

Procedimiento pendiente:

1. Aprobar dominio y proveedor; verificar ownership DNS y plan de rollback.
2. Crear SPF con includes/terminador acordado, sin exponer secretos.
3. Crear selector DKIM, almacenar privada en secret manager y registrar pública autorizada.
4. Publicar DMARC `none` temporal si Operaciones/Seguridad lo aprueban, monitorizar reportes sin direcciones completas y endurecer política por decisión explícita.
5. Enviar prueba sintética a buzón controlado; verificar SPF, DKIM y alignment en cabeceras redactadas.
6. Rotar DKIM según política y conservar overlap/rollback.

Estado SPF/DKIM/DMARC y entregabilidad: **PREPARADO_NO_EJECUTADO**. Mailpit local no valida DNS ni proveedor real.
