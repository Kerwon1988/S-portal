# ADR-007 — Email transaccional

- Estado: Borrador
- Fecha: 2026-11-10

## Contexto
Verificación, recuperación, invitaciones, solicitudes, mensajería y alertas requieren correo; el proveedor y dominio aún no están decididos. La entrega debe ser asíncrona y no filtrar PII.

## Decisión recomendada
Publicar puerto `EmailSender` en `notification.api`, enviar por SMTP vía adaptador y outbox con payload cifrado, reintentos, idempotencia y purga tras entrega. Plantillas Thymeleaf HTML + texto plano, bilingües por locale del destinatario. Mailpit en desarrollo. En producción, preferir proveedor/región aprobados con SPF/DKIM/DMARC y gestión externa de credenciales.

## Alternativas
- SMTP síncrono dentro de transacción: acopla latencia/fallo externo a registro; rechazado.
- Llamar API propietaria en módulos de negocio: acopla proveedor; rechazado por puerto adapter.
- Proveedor específico: solo tras decisión de región, tratamiento y coste.

## Consecuencias
Outbox garantiza reintento eventual, no exactly-once extremo a extremo; emails deben tolerar duplicado raro y usar Message-ID estable. Logs solo métricas/correlation ID, no destinatario ni cuerpo.

## Verificaciones pendientes
- NTF-001..003: outbox, SMTP/plantillas y worker.
- NTF-004/005, HRD-009: notificaciones de dominio, preferencias y DNS/entregabilidad.
