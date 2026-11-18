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

## Implementación observada — 2026-11-13

Existe un avance parcial de FIL-001 con `FileStorage`, metadata JPA y adaptador local bajo un directorio configurable. Las pruebas focalizadas se añadieron pero no pudieron ejecutarse por fallo PKIX del repositorio Maven corporativo. El ADR permanece en borrador: la ruta durable, volumen, backup, antivirus y entrega HTTP siguen pendientes de validación/aprobación.

## Actualización observada — 2026-11-17

El workspace incluye ahora validación Tika, cuarentena/estado CLEAN con adaptador ClamAV fail-closed, la API `files.api.ImageVariants`, generación de variantes WebP y la migración aditiva de `size_bytes`; existen tests unitarios y H2, pero no se pudieron compilar ni ejecutar. Tika 3.2.3, Commons IO 2.20.0, WebP 0.1.6, CommonMark 0.24.0 y OWASP Java HTML Sanitizer 20240325.1 están en el repositorio local activo. La ejecución offline más reciente se bloquea al cargar el descriptor de Maven Enforcer porque el POM local `org.apache.maven:maven-parent:42` está marcado con otro ID de repositorio. El JDK activo es 17.0.21 frente al requisito 21. El ADR y FIL-001..004 permanecen sin validar; el volumen/backup durables, entrega HTTP y compatibilidad real con SQL Server siguen pendientes.
