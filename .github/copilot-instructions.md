# S-portal coding instructions

- Treat [the modular rules](../docs/arquitectura/NORMAS-DESARROLLO-MODULAR.md) and [the approved roadmap](../docs/plan/10-roadmap-backlog.md) as authoritative.
- Before editing, identify the task ID, acceptance criteria, owner, dependencies, affected contracts/files, security controls, tests and documentation.
- Preserve the server-rendered modular monolith: Thymeleaf/HTMX, one deployable and one database; no SPA, microservices, cross-module repositories/entities/templates, or direct browser-to-SimbioSphere calls.
- Enforce `web → application → domain`; keep infrastructure behind ports and cross-module calls behind named APIs/SPIs.
- Require server-side authorization by role and resource ownership, CSRF for every mutation, validation, safe output encoding, and no secrets/PII in logs, events, tests or documentation.
- For UI changes, use local fingerprinted assets, nonce-compatible scripts, semantic theme tokens, ES/EN keys, responsive layouts, keyboard/focus behavior and WCAG 2.2 AA tests.
- Use H2 for automated persistence tests. Do not claim SQL Server compatibility, Docker execution or external-environment validation without evidence.
- Run the smallest relevant test first, then the required build gate. Update README, INVENTARIO and DIARIO from observable workspace evidence; update ADRs when an accepted decision changes.
- Keep [INVENTARIO.md](../INVENTARIO.md) aligned with the modules, routes, owners, and validation evidence actually present in the workspace.
- Do not infer completion from a plan entry. Leave blocked or externally unverified work explicitly open.
- Do not run Git commands or inspect Git history as part of this workflow.
