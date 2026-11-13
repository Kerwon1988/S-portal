# ADR-005 — Almacenamiento de ficheros

- Estado: Borrador
- Fecha: 2026-11-10

## Contexto
CMS, solicitudes y contacto requieren medios/adjuntos, escaneo antivirus, variantes de imagen y descarga controlada. El backend debe evitar rutas públicas directas y permitir migración posterior de almacenamiento.

## Decisión recomendada
Definir puerto `FileStorage`; MVP guarda en disco/volumen Docker fuera del webroot, con nombre aleatorio, metadata/owner en módulo `files`, validación Tika+allowlist+tamaño, ClamAV con cuarentena y streaming autorizado con `Content-Disposition: attachment`/`nosniff`. Rechazar SVG activo; redimensionar a WebP y quitar EXIF. La relación al fichero la mantiene el módulo funcional owner.

## Alternativas
- Guardar bytes en SQL Server: simplifica backups atómicos, pero aumenta tamaño de BD/IO y acopla entrega de medios a DB; no recomendado.
- Blob/almacenamiento de objetos desde el día uno: escalable, pero requiere proveedor/coste/credenciales aún no decididos; dejar adaptador R2.
- Servir desde webroot: rechazado por exposición y falta de autorización.

## Consecuencias
Backup incluye volumen y DB; purgas coordinan metadata/bytes. Ficheros en cuarentena no se descargan. R2 puede añadir adaptador sin cambiar contratos de `files.api`.

## Verificaciones pendientes
- FND-005/FIL-001..004: volumen dev, MIME, antivirus, límites y variantes.
- ADR-006/HRD-005/HRD-006: persistencia, backup, plataforma y volumen.
