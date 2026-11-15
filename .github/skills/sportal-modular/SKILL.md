---
name: sportal-modular
description: Use for any S-portal feature, bug fix, refactor, template, architecture, test, or configuration change. Triggers when the task touches this workspace or its modular monolith.
---

# S-portal modular delivery

## Required before editing

1. Read the assigned task and its exact roadmap dependencies.
2. Read the relevant sections of `docs/plan/03-arquitectura.md`, `docs/plan/07-seguridad-rgpd.md`, and `docs/arquitectura/NORMAS-DESARROLLO-MODULAR.md`.
3. Identify the owning module, published API/SPI, table owner, UI contract, affected paths, security boundary, and acceptance test.
4. Stop and record a blocker if a requirement changes authorization, PII handling, ownership, or a pending business/legal decision.

## Implementation rules

- Keep one server-rendered modular monolith. Follow `web → application → domain`; adapters implement ports.
- Use named module APIs or shared SPIs. Never access a foreign repository/entity/template.
- Check role and resource ownership in every handler; apply CSRF to every web mutation. UI visibility is not authorization.
- For templates, use Thymeleaf escaping, local fingerprinted assets, CSP nonce, semantic theme tokens, ES/EN keys, responsive reflow, labels, accessible errors, focus restoration and progressive enhancement.
- Do not put credentials, tokens, PII or raw request bodies in logs, events, telemetry, tests, screenshots or generated diagrams.
- Use synthetic test data and H2 for automated persistence tests. SQL Server remains unverified unless a specific authorized verification proves otherwise.
- Avoid direct browser requests to SimbioSphere; outbound integration is a typed backend client against the approved contract.

## Verification and completion

- Add the task's named test, including a negative test for its primary security or architecture rule.
- Run focused tests and relevant architecture, template, i18n, frontend and build gates.
- Update README, `INVENTARIO.md`, `DIARIO.md`, and ADRs when applicable using observable evidence; preserve unknowns as pending.
- Report completed IDs separately from partial, blocked, and unverified IDs. Never claim completion because code merely compiles.

## Change checklist

- [ ] Task dependencies are satisfied and the scope has no unresolved contract collision.
- [ ] Owner, boundaries, permissions, CSRF, validation, PII and error behavior are covered.
- [ ] Locale/theme, keyboard, focus, mobile and accessible names/errors are covered for UI.
- [ ] Tests include the expected success and rejection paths.
- [ ] Documentation and evidence agree with the workspace state.
