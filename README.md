# S-portal

S-portal is a server-rendered modular monolith for Simbio. The current implementation is the F0 foundation; business modules and production UI are not yet present.

## Stack and local build

- Java 21; Maven Wrapper pinned to Maven 3.6.3.
- Spring Boot 4.1.2 and Spring Modulith 2.1.2; package root `com.simbio.core`.
- H2 is test-scoped by default and is the only database used by automated tests. The opt-in `local-h2` Maven profile adds H2 at runtime and loads `application-local-h2.yml`; it deactivates the default `frontend-build` Maven profile so local startup does not resolve or run the frontend plugin. CI explicitly activates both `ci` and `frontend-build`. Production builds do not activate `local-h2`. SQL Server 2022 remains the production target, but its compatibility has not been validated.

Run the build from PowerShell with a Java 21 JDK configured:

```powershell
.\mvnw.cmd -B verify
```

Use `-o` only after all required artifacts are available in the local Maven cache. Keep repository settings local and do not commit them. Builds and dependency resolution use Java 21 with the pinned Maven Wrapper. No minimum Maven version beyond the pinned wrapper is required.

## Profiles and local services

The application listens on port `8090`; the management server uses port `8091`. Configuration is split across `dev`, `test`, `local-h2`, and `prod` profiles. `local-h2` uses an in-memory H2 database and generated schema; production requires an HTTPS `PORTAL_BASE_URL`, and secrets are supplied through environment variables and must not be committed.

`docker-compose.dev.yml` describes optional SQL Server 2022, Mailpit, and ClamAV services. Docker is not required for the build or tests. Copy `.env.example` to a local `.env`, supply local-only values, and review the Compose configuration before starting services. Do not commit `.env`.

H2 tests and the `local-h2` profile use generated schemas. The Flyway tree under `src/main/resources/db/migration/<module>/` is intended for SQL Server and is checked statically; migrations are not run against SQL Server in development.

## Security and observability

The foundation configures default-deny Spring Security, CSRF, session/CSRF cookies, security headers, and a nonce-based CSP. Production logging is JSON; correlation IDs are validated, and message PII such as email addresses and `token=` values is masked. Production log exception traces are replaced with `***`, so they do not expose stack details.

Readiness and other configured Actuator endpoints use the management port. Environment details and application-port Actuator routes are not exposed. The security and Actuator behavior is covered by automated tests.

Backend and database failures are captured by `config.errortracking.ErrorInventoryService` through the `shared.spi.ErrorReporter` port. The JSON inventory defaults to `logs-error/error-inventory.json`, outside the web root and SQL Server, and can be relocated with `PORTAL_ERROR_TRACKING_PATH` to a private persistent volume. The initial storage model is single-writer; do not share it between application replicas. Stored records omit exception messages, SQL text/values, request query strings, and request bodies; stack data contains class and method names only. The inventory is bounded by configurable entry, file-size, and field-length limits. When full, it evicts the oldest resolved/ignored record; if all records remain open, new distinct records fail explicitly and the original application error is preserved. Retention and production limits still require operational approval.

The local fingerprinted `error-reporter.js` submits allowlisted JavaScript, resource, network, and HTMX failures to `POST /api/telemetry/errors` only after explicit opt-in. A non-modal ES/EN banner is denied by default, offers accept/reject, and can be reopened from the footer to change or withdraw the choice. The versioned preference stays in browser `localStorage` only; no identity or server-side consent record is created, and a storage failure keeps reporting disabled. Consent copy is provisional and needs legal review before production. The endpoint keeps CSRF enabled, requires a matching `Origin`, accepts at most 4 KiB of uncompressed JSON, and applies an in-memory token bucket (burst 30, refill 30/minute); the browser additionally caps reports at five per minute. The server fixes the source to `FRONTEND`, validates event/error names, and stores only an allowlisted asset-frame summary, locale, correlation ID, and redacted path. Free-form error messages, DOM/form data, cookies, headers, and query strings are not sent or persisted. The rate limiter and JSON inventory are process-local/single-writer and need an approved shared design before scaling.

## CI and verification limits

The GitHub Actions workflow is configured to run the Maven `ci` profile, tests, JaCoCo coverage checks, and OWASP Dependency-Check. Configure `NVD_API_KEY` as a GitHub Actions secret for the scan. Running the remote workflow, obtaining a scan result, and reviewing licenses are not prerequisites for closing the F0 foundation tasks.

The F0/PROMPT 04 verification with JDK 21, Maven offline, and `local-h2` completed with 76 Surefire tests and 3 Failsafe integration tests passing; the configured JaCoCo checks passed. No Dependency-Check result is claimed. SQL Server remains an unverified production target, and Docker/SQL Server execution is outside the development gates.

See [ADR-001](docs/adr/ADR-001-spring-boot-4-1-y-gobernanza-modulith.md) for the stack decision and [the F0 task sheet](docs/plan/tareas/F0-fundaciones.md) for scope and acceptance criteria.

## Frontend build and UI foundation

The Maven lifecycle installs the pinned Node/npm toolchain, runs `npm ci`, compiles Tailwind CSS 3.4.19, and writes fingerprinted CSS/JavaScript plus an asset manifest into `target/classes/static/assets`. Playwright 1.62.1 is used for E2E. HTMX 2.0.11 and Alpine CSP 3.17.5 are served locally; HTMX indicator styles are defined in the local CSS rather than injected inline, honoring CSP. Templates do not load scripts, styles, or fonts from a CDN. Theme tokens are in `src/main/frontend/css/tokens.css`; typography uses the platform `system-ui` stack and bundles no font files.

The initial shell is server-rendered at `/es` and `/en`. Public/account/backoffice layouts share accessible navigation, skip link, language/theme controls, security metadata, and branded error views. Theme selection persists in a technical cookie and local storage for anonymous and authenticated users; profile persistence is deferred to the future Identity integration through the optional `ThemePreference` SPI. Account and backoffice business routes remain default-denied and are not implemented by the UI foundation.

### Navigation menu

The primary navigation is composed from `shared.spi.NavContribution` and constrained to the available header width. When all links do not fit, they move to the accessible drawer; the layout is remeasured after fonts load and on resize. Keyboard focus handling and the overflow state are covered by Playwright. Hiding a link is not authorization: server-side permissions remain authoritative.

## E2E and accessibility

With Node 22 and npm 10 available:

```powershell
npm ci
npm run e2e:install
$env:PORTAL_E2E_BASE_URL = "http://localhost:8090"
npm run e2e
```

The 15 Playwright tests cover the public shell across ES/EN, light/dark, mobile/desktop, theme persistence, language switching, navigation overflow/focus, HTMX CSRF/errors, consent-gated frontend error reporting, screenshots under `visual/`, and axe checks. All 15 passed against the updated local H2 application on 2026-11-13. Tests fail with an explicit precondition message when `PORTAL_E2E_BASE_URL` is not set; a missing server is not reported as a successful test.

PROMPT 04 validation: `npm run build` passed; `mvn -o -s settings-s-portal.xml -Plocal-h2 verify` passed with 76 Surefire tests, 3 Failsafe integration tests, and JaCoCo checks; all 15 Playwright tests passed against the updated local H2 application, including opt-in, rejection/revocation, and browser-storage failure. The full suite exposed and led to fixing a Thymeleaf 3.1 incompatibility: the layout no longer accesses the disabled `#request` expression object and receives the correlation ID via `WebViewAdvice`. The startup log showed the application needed about 36 seconds to start; earlier probes ran too soon. Consent copy still needs legal review before production; single-writer/rate-limit scaling and retention also need operational approval.

## Governance and architecture documentation

- [P01–P12 modular development rules](docs/arquitectura/NORMAS-DESARROLLO-MODULAR.md)
- [Current route/UI contract](docs/arquitectura/rutas-ui.md)
- [Module diagram](docs/arquitectura/modulos.mmd)
- [Technical inventory](INVENTARIO.md) and [development diary](DIARIO.md)
- [ADR register/template](docs/adr/TEMPLATE.md)
- [Copilot repository instructions](.github/copilot-instructions.md) and the [S-portal delivery skill](.github/skills/sportal-modular/SKILL.md)

PROMPT 03 is complete under the revised scope (UIK-002..UIK-009 and GOV-001..GOV-006). The latest `mvn clean verify -Plocal-h2` passes (73 tests, no failures or errors; JaCoCo passes), as do the focused UI/GOV tests, direct frontend build, and all 13 Playwright tests against the local H2 application, including axe and the visual matrix. The Maven `frontend-build` profile remains configured for CI but was not run locally; it is not a gate in the current PROMPT 03 scope. See the task sheet for evidence.
