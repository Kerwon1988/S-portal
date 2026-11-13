# ADR-010 — SSO OIDC con IdP compartido

- Estado: Propuesto/Futuro (R3+)
- Fecha: 2026-11-10
- Tarea(s): `R3-SSO` (pista futura del roadmap)

## Contexto
MVP conserva logins independientes de S-portal y SimbioSphere (D03). Un IdP compartido podría reducir duplicidad de credenciales, pero requiere capacidad del sistema DEMO, modelo de cuentas compartidas, operación del IdP y análisis de migración.

## Decisión propuesta
No implementar OIDC/SSO en MVP ni cambiar el login actual. En una fase R3+, evaluar IdP OIDC con authorization code + PKCE, issuer/audience/scopes, mapping explícito a `ClientAccount` y tenants, logout/session lifecycle, recuperación y plan de migración. No vincular por email; exigir prueba de posesión y políticas anti-account-takeover.

## Alternativas
- Mantener logins separados: decisión MVP; simple y compatible con estado actual de DEMO.
- Compartir password/hash: rechazado; no sincronizar credenciales ni acoplar hash.
- Proxy que reutiliza sesión/cookie: rechazado; cookies/sesiones comparten dominio localhost y no constituyen federación segura.

## Consecuencias
Cambio futuro requiere threat model, modelo de identidad, UX de enlaces de cuenta, fallback de IdP, revocación y pruebas de aislamiento. La integración M2M de aprovisionamiento permanece separada de autenticación de usuario.

## Verificaciones pendientes
- R3: capacidad de DEMO/IdP y aprobación de owners de ambos productos.
- ADR-002: decisión de identidad M2M y vínculo seguro sigue vigente hasta nuevo ADR aprobado.
