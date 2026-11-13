# ADR-004 — Estrategia de IDs en SQL Server

- Estado: Borrador
- Fecha: 2026-11-10

## Contexto
Se requiere clave interna eficiente en SQL Server sin exponer IDs secuenciales en URLs; también hay referencias legibles para solicitudes y eventos entre módulos.

## Decisión recomendada
Usar `id BIGINT IDENTITY` como PK clustered interna y `public_id UNIQUEIDENTIFIER` (UUIDv4 generado por aplicación) con índice único no clustered para URLs/API. La solicitud tiene `reference_code` único `SOL-AAAA-NNNNNN`, distinto de la clave. Fechas UTC `datetime2(3)`, importes `DECIMAL(19,4)` y `currency CHAR(3)`.

## Alternativas
- UUIDv4 como clustered PK: no exponer secuencia, pero aumenta fragmentación y tamaño de índices.
- UUIDv7: orden temporal útil, pero requiere confirmar soporte/generación y aceptación de privacidad; no fijarlo sin spike.
- BIGINT público: compacto, pero permite enumeración; rechazado como identificador de URL.

## Consecuencias
Toda API/URL utiliza `public_id`; queries siguen validando rol/propiedad. Índices públicos son únicos no clustered. La collation SQL Server, generación de `SOL-AAAA-NNNNNN` y estrategia concurrente no se verifican con la suite H2 y quedan como limitación conocida.

## Verificaciones pendientes
- FND-001: fijar driver y configuración por dependencias/build; no se exige verificar SQL Server ni su collation.
- FND-006/REQ-002: pruebas automatizadas con H2 y revisión estática de migraciones; no se afirma validación de índices o secuencias en SQL Server.
