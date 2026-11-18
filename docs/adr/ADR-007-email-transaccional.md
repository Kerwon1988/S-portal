# ADR-007 — Email transaccional

- Estado: Borrador; implementación parcial en desarrollo, pendiente de validación
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

El código NTF-001..003 implementa outbox con payload cifrado, adapter SMTP, plantillas HTML/texto plano ES/EN, leases, reintentos y health/métricas. La configuración de producción exige credenciales y remitente externos. No se validaron compilación/tests, Mailpit, proveedor/región, entregabilidad ni retención legal productiva. La limpieza de payload y los reintentos no eliminan la posibilidad de duplicado tras aceptación SMTP y fallo antes de confirmar el envío.

## Verificaciones pendientes
- NTF-001..003: implementación añadida; compilación y tests aún pendientes por fallo TLS al resolver dependencias Maven.
- NTF-004/005, HRD-009: notificaciones de dominio, preferencias y DNS/entregabilidad.
- Aprobación de proveedor/región, credenciales, SPF/DKIM/DMARC y política de retención productiva.
