# ASVS L2 — checklist de S-portal

Estado: **PREPARADO_NO_EJECUTADO** para una revisión independiente y ZAP autorizado. Scope: host propio de PRE, datos sintéticos, sin DEMO ni terceros.

| Área ASVS L2 | Evidencia local | Gate/estado | Owner |
|---|---|---|---|
| Arquitectura/confianza | Normas P01–P12, DAG Modulith, ArchUnit | Tests focales | config/transversal |
| Autenticación/sesión | CSRF, sesión segura, MFA admin, recovery | Security tests/E2E | identity |
| Control de acceso | rutas centralizadas, ownership server-side, IDOR negatives | MVC/services | cada módulo |
| Validación/salida | Bean Validation, Thymeleaf escaped, sanitizer CMS | Unit/IT/template tests | owner endpoint |
| Comunicaciones | CSP enforce, HSTS, TLS reverse proxy preparado | SecurityHeadersTest | infra/config |
| Datos/criptografía | AES-GCM PII, HMAC, claves por entorno | Crypto tests; rotación pendiente | config/security |
| Ficheros | tamaño/tipo, ClamAV, owner-only download | Files/Messaging tests | files |
| API/M2M | HMAC/time-window/idempotency, backend only | SimbioSphere local contract tests | simbiosphere |
| Logging/errores | PII masking, correlation IDs, inventory privado | Observability tests | config |
| Configuración | perfil prod fail-closed, Actuator separado, no secrets | ProdProfile/Actuator tests | config |

## Proceso

1. Ejecutar únicamente contra un host PRE aprobado por escrito y rate-limitado.
2. Usar cuentas efímeras y no conservar PII, tokens, cookies ni cuerpos de respuesta.
3. Registrar hallazgo con CVSS/ASVS, ruta, owner, evidencia redactada y fecha.
4. High/Critical bloquea `go/no-go` salvo excepción aprobada con mitigación, owner y caducidad.
5. No ejecutar pruebas activas contra DEMO, producción o terceros sin autorización adicional.

## Evidencia pendiente

- TC-HRD-001: revisión humana ASVS y ZAP baseline: `PREPARADO_NO_EJECUTADO`.
- Motivo: no hay host PRE/credenciales/autorización disponibles.
- Reproducir: desplegar el candidato, aprobar scope y ejecutar el workflow `security-baseline.yml`; adjuntar reporte redactado.
