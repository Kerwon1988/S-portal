# ADR-002 — Identidad, contraseña en SimbioSphere y autenticación M2M

- Estado: Borrador
- Fecha: 2026-11-10

## Contexto
D03 fija logins separados y una API M2M nueva en DEMO, adaptada sobre `OwnerOnboardingOrchestratorApi`. DEMO no tiene API REST de alta; `/onboarding` es MVC+CSRF y no devuelve IDs (`OwnerOnboardingController.java:29,36,46,63-68,74-84`). El alta debe esperar a verificar email y no exponer el reintento inseguro por email (`PlatformOwnerOnboardingService.java:38-41,67-84`).

## Decisión recomendada
- Contraseña: opción A. No enviar ni sincronizar contraseña; crear propietario sin password conocido y emitir enlace just-in-time de un uso (15 min) para que el titular la defina. S-portal no persiste el token de activación. Mantener `ClientAccount` y `CustomerAccount` como identidades separadas, vinculadas por referencia externa y prueba de posesión; nunca por coincidencia de email.
- M2M: HMAC-SHA256 con key ID, timestamp ±300 s, firma de método/path/query/hash del body/`Idempotency-Key`, scopes `provisioning:write/read`, rate limit y rotación con dos claves activas. Secretos desde gestor externo; CSRF se exime solo en cadena `/api/platform/v1/**`.
- Idempotencia de plataforma separada de la actual tenant+sede; la misma clave/payload reproduce la respuesta y la misma clave con payload distinto devuelve 409.

## Alternativas
- B: enviar password por TLS durante registro: duplica credenciales, aumenta impacto de filtración y es incompatible con provisionar tras verificación si el portal solo retiene hash. Rechazada.
- C: importar hash BCrypt: acopla algoritmos y hace circular material sensible. Rechazada.
- OAuth2 client credentials: alternativa si HMAC no cumple política corporativa; requiere IdP/infra adicional y verificación de soporte.
- OIDC/SSO: solo R3+ y ADR-010.

## Consecuencias
La API M2M debe aceptar evidencia de email verificado y persistir aceptación de términos. `modules` omitido/null debe distinguirse de `[]` (defaults por tipo frente a cero opcionales). Las llamadas remotas van fuera de transacciones; saga local y EPR/outbox. API y provider tests se acuerdan antes de implementación.

En PROMPT 09 se añadieron flujos locales para verificar email, recuperar/cambiar la contraseña e invitar a definir una credencial propia; el token es opaco, de un uso y se persiste hasheado. No se sincronizan contraseñas con SimbioSphere y el correo de perfil no se cambia desde el formulario actual. No se modificó DEMO ni se afirma alta/provisión externa. La compilación y los tests de estos flujos siguen pendientes de validación Maven.

## Verificaciones pendientes
- FND-001: Argon2id/DelegatingPasswordEncoder y crypto.
- DEMO-01/02a/02b/03a/03b/03c/06a/06b/07/08: contrato, HMAC, idempotencia, activación, evidencia y defaults.
- SIM-002/003/004/005/006/008/012/013: OpenAPI, adapter, saga, enlace y pruebas.
