# S-portal

S-portal is a server-rendered modular monolith for Simbio. Prompt 05 audit, file-storage, and identity work and Prompt 06 CMS are complete for their approved development scopes. Prompt 07 ERR-004, Prompt 08 SEO/contract work, Prompt 09 notification/extended-identity work, Prompt 10 legal/customer work, and Prompt 11 staff/user-management work remain partial. In the latest Java 21 offline recheck, Maven stopped while resolving the parent `org.apache:apache:pom:37` for Artemis, before application compilation or test execution. Spring Boot 4.0.0, 4.1.1, and 4.1.0 were tried as alternatives, but their parent POMs are not available in the provided offline repository. The project remains on Spring Boot 4.1.2; no online Maven resolution was authorized. In the preceding run, test sources compiled; Surefire no longer requested `plexus-utils:1.1` but could not resolve `surefire-junit-platform:3.5.6`. The debugger briefly reported the application ready, but its session ended, port 8090 had no listener, and an HTTP request to `/acceso` could not connect; startup remains unconfirmed. `npm run build` passed for the current assets; it does not validate Java. Business workflows and production UI are not complete.

The previously recorded full offline test run completed with 166 Surefire tests, 14 Failsafe integration tests, and JaCoCo checks passing. That evidence predates Prompts 07, 09, 10, and 11 and does not validate their changes. It validates only the earlier local Java/H2 build; SQL Server compatibility, production operations, and external approvals remain unverified. The current application launch attempt did not leave an active debugger session or a listener on port 8090; runtime startup remains unverified and is separate from Prompt 05's agreed compile/test acceptance gate.

The `content` owner module delivers bilingual pages and blocks, drafts, publishing, snapshots/restoration, slug redirects, sanitized Markdown, media references, legal-document versioning, team content, and request-condition copy. CMS unit/MVC/H2 tests passed in the full verification. Legal text and team material remain pending approval; editors cannot self-approve them. The request-module consumer of the published conditions API is deferred to F3. Prompt 08 SEO metadata and sitemap/robots support, plus a Portal↔DEMO OpenAPI proposal and local contract fixtures, pass the tests in the full verification but remain partial pending performance thresholds and external approvals. The owner-approved local fingerprinted Simbio logo is present. The public footer links only to published legal documents. The current cookie setup uses technical preferences/session cookies only; no optional-cookie integration or third-party analytics is configured. Error reports are a separate same-origin, explicit opt-in feature. `npm run test:privacy` passes three focused Node tests. Fingerprinted CSS, JS and PNG assets are configured for one-year immutable caching; the manifest, pages and previews are excluded. Java tests for the new footer/cache behavior have not run: the latest focused Maven `test` stopped before test execution, so the earlier full verification does not validate those tests. DEMO-01 and both owners' contract approval remain pending; this is not a live integration.

## Stack and local build

- Java 21; Maven Wrapper pinned to Maven 3.6.3.
- Spring Boot 4.1.2 and Spring Modulith 2.1.2; package root `com.simbio.core`.
- H2 2.2.224 is pinned explicitly for automated tests and the `local-h2` Maven profile; it is the only database used by automated tests. The profile adds H2 at runtime and loads `application-local-h2.yml`; it deactivates the default `frontend-build` Maven profile so local startup does not resolve or run the frontend plugin. CI explicitly activates both `ci` and `frontend-build`. Production builds do not activate `local-h2`. SQL Server 2022 remains the production target, but its compatibility has not been validated.

Run these commands separately from PowerShell at the project root. They use the provided Maven 3.6.3 installation, Java 21, the local dependency repository, and the `local-h2` profile. Set up the terminal once:

```powershell
$env:JAVA_HOME='C:\Users\x455035\2Programs\temurin-21.0.11'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$mvn='C:\Users\x455035\2Programs\maven-3.6.3\bin\mvn.cmd'
$repo='C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias'
```

Compile the application:

```powershell
& $mvn -o -B "-Dmaven.repo.local=$repo" -Plocal-h2 compile
```

Run unit tests, integration tests, and the JaCoCo verification gate:

```powershell
& $mvn -o -B "-Dmaven.repo.local=$repo" -Plocal-h2 verify
```

Start the application locally after the build and tests:

```powershell
& $mvn -o -B "-Dmaven.repo.local=$repo" -Plocal-h2 "-Dspring-boot.run.profiles=local-h2" spring-boot:run
```

`-o` keeps Maven offline, and `-Dmaven.repo.local` selects the provided local repository. The application listens on port `8090`; the management server uses port `8091`. These commands do not download dependencies. If an artifact is absent from the local repository, Maven stops with an offline resolution error. Keep the terminal open while `spring-boot:run` is active.

## Profiles and local services

The application listens on port `8090`; the management server uses port `8091`. Configuration is split across `dev`, `test`, `local-h2`, and `prod` profiles. `local-h2` uses an in-memory H2 database and generated schema; production requires an HTTPS `PORTAL_BASE_URL`, and secrets are supplied through environment variables and must not be committed.

`docker-compose.dev.yml` describes optional SQL Server 2022, Mailpit, and ClamAV services. Docker is not required for the build or tests. Copy `.env.example` to a local `.env`, supply local-only values, and review the Compose configuration before starting services. Do not commit `.env`.

H2 tests and the `local-h2` profile use generated schemas. The Flyway tree under `src/main/resources/db/migration/<module>/` is intended for SQL Server and is checked statically; migrations are not run against SQL Server in development.

## Security and observability

The foundation configures default-deny Spring Security, CSRF, session/CSRF cookies, security headers, and a nonce-based CSP. Production logging is JSON; correlation IDs are validated, and message PII such as email addresses and `token=` values is masked. Production log exception traces are replaced with `***`, so they do not expose stack details.

Readiness and other configured Actuator endpoints use the management port. Environment details and application-port Actuator routes are not exposed. The security and Actuator behavior is covered by automated tests.

Backend and database failures are captured by `config.errortracking.ErrorInventoryService` through the `shared.spi.ErrorReporter` port. The JSON inventory defaults to `logs-error/error-inventory.json`, outside the web root and SQL Server, and can be relocated with `PORTAL_ERROR_TRACKING_PATH` to a private persistent volume. The initial storage model is single-writer; do not share it between application replicas. Stored records omit exception messages, SQL text/values, request query strings, and request bodies; stack data contains class and method names only. The inventory is bounded by configurable entry, file-size, and field-length limits. When full, it evicts the oldest resolved/ignored record; if all records remain open, new distinct records fail explicitly and the original application error is preserved. MVC error capture now runs after standard exception handlers and excludes expected 4xx responses. Automated tests use `target/test-error-inventory.json` instead of writing to the operational inventory. Retention and production limits still require operational approval.

The local fingerprinted `error-reporter.js` submits allowlisted JavaScript, resource, network, and HTMX failures to `POST /api/telemetry/errors` only after explicit opt-in. A non-modal ES/EN banner is denied by default, offers accept/reject, and can be reopened from the footer to change or withdraw the choice. The versioned preference stays in browser `localStorage` only; no identity or server-side consent record is created, and a storage failure keeps reporting disabled. Consent copy is provisional and needs legal review before production. The endpoint keeps CSRF enabled, requires a matching `Origin`, accepts at most 4 KiB of uncompressed JSON, and applies an in-memory token bucket (burst 30, refill 30/minute); the browser additionally caps reports at five per minute. The server fixes the source to `FRONTEND`, validates event/error names, and stores only an allowlisted asset-frame summary, locale, correlation ID, and redacted path. Free-form error messages, DOM/form data, cookies, headers, and query strings are not sent or persisted. The rate limiter and JSON inventory are process-local/single-writer and need an approved shared design before scaling.

Prompt 07 adds the server-rendered `/gestion/errores` viewer under `config`, protected by both role `ADMIN` and the dedicated `ERROR_INVENTORY_MANAGE` permission. It filters and paginates summaries, shows current storage count/capacity, hides inventory details when storage is unavailable, and persists RESOLVED/IGNORED state and a bounded, sanitized operator note in the private JSON file; a new occurrence reopens the entry. Successful changes are recorded through `shared.spi.AuditEventRecorder`, implemented by the `audit` owner. Mutations use CSRF and no bulk-delete action is exposed. Focused controller/service tests cover authorization, filters, CSRF, resolution, redaction and storage degradation; the storage-unavailable integration test covers both viewing and attempting an update. ERR-004 remains partial: the focused Maven verification was attempted with the provided offline repository but Maven could not resolve the Spring Boot 4.1.2 parent POM, so Java compilation and tests did not start. `npm run build` and `npm run test:privacy` passed (3/3); these frontend checks do not validate the Java viewer.

## Audit and identity (Prompt 05 complete)

The `audit` module records minimal, append-only events and provides paged, permission-protected views. The `identity` module contains role/permission seeding, password policy and Argon2id encoding, login/session handling, a default-deny authorization matrix, rate limiting, and externally configured one-use administrator bootstrap. The required unit, web, security, and H2 tests passed in the full Java 21 Maven verification. The bootstrap must remain disabled by default and cannot run in `prod`; the rate limiter is process-local and needs an approved shared design before multi-replica deployment. ADR-002 remains a draft for separate SimbioSphere/M2M decisions.

## Notifications and extended identity (Prompt 09 — partial)

The `notification` module adds an encrypted outbox, an SMTP adapter, bilingual Thymeleaf HTML/plain-text email templates, bounded retry/backoff, leases, and queue health/metrics. Payload encryption uses AES-256-GCM with a key ID; production SMTP credentials, key ring, and retention are external configuration, not committed secrets. SMTP delivery is not exactly-once: a process failure after remote acceptance can still cause a duplicate. Mailpit delivery has not been exercised.

The `identity` module adds hashed, expiring, one-use tokens for email verification, password recovery, and invitations; account profile preferences; TOTP MFA with recovery codes; and identity adapters for `CurrentUser`, `RecipientDirectory`, theme, and locale SPIs. New invitation email lookups use a purpose-separated HMAC-SHA-256 blind index; revocation considers fingerprints from every configured key and temporarily queries legacy SHA-256 indexes for invitations created before this change. Keep previous keys configured until invitations indexed with them expire or are revoked. Legacy hashes remain in stored token rows until a token-retention/purge mechanism removes those rows; that policy and purge are still pending. Mutating forms retain CSRF protection, the profile derives its owner from the authenticated principal, and identity events contain IDs only. MFA is mandatory for administrators; any employee may opt in, after which login challenges the enrolled factor. The self-hosted proof-of-work/honeypot is consumed by `/registro`, but it is not the official ALTCHA protocol, lacks multi-replica replay coordination, and is not wired to a contact form; IAM-006 remains partial.

Prompt 09 remains open pending Java test execution. Focused tests exist for AES-GCM/HMAC fingerprints and key-ring revocation, anti-bot verification, delivery policy, TOTP, and H2 token consumption. Offline Java 21 `test-compile` succeeded, compiling 342 production sources and 119 test sources. `verify` stopped before running tests because the supplied offline repository lacks `org.apache.maven.surefire:surefire-junit-platform:3.5.6`. The frontend asset build passed. Production SMTP provider/region, approved retention, key operations/rotation, multi-replica anti-bot replay protection, Mailpit delivery, and SQL Server migrations remain unverified or externally pending.

## File storage (Prompt 05 complete)

The `files.api.FileStorage` port uses a private local directory configured by `PORTAL_FILE_STORAGE_ROOT` (default: `${java.io.tmpdir}/s-portal-files`), outside the static web root. Stored objects use random UUID-derived keys in quarantine; downloads require the caller-provided authorization check and are exposed as an `InputStream` only when metadata is `CLEAN`. `files.api.ImageVariants` exposes authorized variant creation, listing, and streaming; variants are generated only from clean images and their metadata remains owned by `files`. Upload validation, ClamAV INSTREAM scanning, and WebP image-variant code are present; the available unit/H2 tests passed in the full Maven verification. No HTTP upload/download route is implemented. Configure a durable private volume and include it in backups before deployment; volume/backup operations and the draft status of ADR-005 remain open.

If ClamAV is unavailable or returns an invalid response, the file remains quarantined and non-downloadable.

## CMS (Prompt 06 complete)

The `content` module owns page translations, blocks, snapshots, slug redirects, team content, legal-document versions, and media references/alt text. Its `content.api` exposes published-page events with identifiers only, request-condition fallback, published legal documents, and team-member reads. The module uses only named `audit.api`, `files.api`, and `shared` contracts; file MIME, size, scan state, and bytes remain owned by `files`. The editor selects only CLEAN registered media and shows private previews; HERO and IMAGE blocks may reference media, which publication verifies as clean and alt-text-complete in both locales. Legal drafts can carry an effective date while remaining pending approval.

The public routes resolve only published snapshots, use canonical/alternate locale metadata, and redirect renamed slugs. The backoffice checks `CONTENT_EDIT`, uses CSRF-protected mutations, sanitizes Markdown before `th:utext`, and previews drafts with `no-store`/`noindex`. A missing or unapproved request-conditions version returns neutral copy and does not activate payment. Legal drafts and team materials cannot be approved from the current CMS UI.

Prompt 06 is complete for the approved development scope. The full Java 21/local-H2 verification passed with 166 Surefire tests, 14 Failsafe integration tests (including CMS seed and persistence), and JaCoCo coverage checks. CMS-010 content remains pending legal approval, team materials remain unpublished pending consent/licensing, and the request-module consumer of the conditions API is deferred to F3. The SQL Server migration was not executed, so SQL Server compatibility is not claimed.

## Legal and customer (Prompt 10 partial)

The `legal` owner records consent against a currently published CMS document/version, with purpose, locale, source, and server UTC time; the subject must match the authenticated actor. Marketing consent is separate and defaults to false. The marketing copy remains a draft pending legal review. `customer` owns client accounts and the single active OWNER relationship, validates the user through the identity API, and encrypts optional contact phone and Spanish NIF/NIE/CIF values. Tax identifiers are returned masked only to the owning CLIENT; only Spain has a syntax validator, with no external authority check.

The customer registration flow is present at `/registro`: it creates a CLIENT account with an OWNER, sends email verification, records required consent versions, treats marketing as optional, and returns a generic submission result for duplicate email. It accepts locale from the request and stores the optional phone in E.164 form. The local proof-of-work/honeypot challenge is connected, but it is not the official ALTCHA library/protocol integration and its replay guard is process-local; CUS-002/IAM-006 remain partial. ADMIN invitation is present at `/gestion/clientes/nuevo`, uses ADMIN plus `USERS_MANAGE`, and creates the customer account only after the identity invitation is accepted. Invitation names are encrypted in the customer-owned table; identity retains invitation email/token data.

Focused Java tests and a controller test are written. Surefire is pinned to `plexus-utils:3.5.1`; Maven debug output confirms that `plexus-utils:1.1` is no longer requested. The latest run stopped earlier, during parent-POM resolution; the preceding run could not resolve `surefire-junit-platform:3.5.6`. No Java tests ran. CUS-001/002/003/004, IAM-006/007/010, and LGL-001 remain unvalidated; SQL Server compatibility is not claimed. LGL-002 has no complete export/request flow or all-owner contributor coverage, and irreversible anonymization remains blocked pending legal approval. LGL-003 has no active policy or purging job; only dry-run is authorized, with no approved categories or retention periods. Pending customer invitation metadata has no expiry/cleanup lifecycle. Spain is the only supported tax-identifier country; no external tax-authority check is performed.

## Staff and user management (Prompt 11 — partial)

The `staff` owner contains worker profiles, invitation handling via `identity` APIs/events, specialties, status management, and ADMIN bootstrap synchronization. The customer-owned management view composes identity/customer/staff directory APIs for filtered, paged client and worker lists, with allowlisted ordering by creation date, name, or email, and a client CSV export capped at 1000 rows. Ordering is stable across pages (selected field plus ID tie-breaker). User detail loads the audit history through the audit-owned HTMX fragment, independently protected by `AUDIT_VIEW`; request, messaging, and SimbioSphere panels remain unavailable because their owner routes/contracts are not present. CSV export requires ADMIN plus `USERS_MANAGE` and `CLIENTS_EXPORT`; staff invitations and profile mutations require ADMIN plus `STAFF_MANAGE` and use CSRF. Account deactivation prevents self-deactivation and deactivation of the last active ADMIN.

Prompt 11 is not complete: user actions cover activation/deactivation, unlock, password-reset request, and EMPLOYEE↔ADMIN role changes, but role transitions involving CLIENT, invitation resend from the detail page, owner-specific detail panels, personal-data export, anonymization, and E2E-001 are not implemented. `/cuenta/datos` is informational only. Per the current decisions, no partial personal-data export will be offered before every owner contributes, and no deletion/anonymous data operation will run until an approved retention policy exists. A clean Maven `test-compile` passed for 343 production and 119 test sources. Focused JUnit execution remains blocked offline because `junit-platform-commons:1.12.2` is missing from the configured Maven repository; no tests have run. SQL Server migrations and compatibility have not been tested.

## CI and verification limits

The GitHub Actions workflow is configured to run the Maven `ci` profile, tests, JaCoCo coverage checks, and OWASP Dependency-Check. Configure `NVD_API_KEY` as a GitHub Actions secret for the scan. Running the remote workflow, obtaining a scan result, and reviewing licenses are not prerequisites for closing the F0 foundation tasks.

The F0/PROMPT 04 verification with JDK 21, Maven offline, and `local-h2` completed with 76 Surefire tests and 3 Failsafe integration tests passing; the configured JaCoCo checks passed. No Dependency-Check result is claimed. SQL Server remains an unverified production target, and Docker/SQL Server execution is outside the development gates.

See [ADR-001](docs/adr/ADR-001-spring-boot-4-1-y-gobernanza-modulith.md) for the stack decision and [the F0 task sheet](docs/plan/tareas/F0-fundaciones.md) for scope and acceptance criteria.

## Frontend build and UI foundation

The Maven lifecycle installs the pinned Node/npm toolchain, runs `npm ci`, compiles Tailwind CSS 3.4.19, and writes fingerprinted CSS/JavaScript plus an asset manifest into `target/classes/static/assets`. Playwright 1.62.1 is used for E2E. HTMX 2.0.11 and Alpine CSP 3.17.5 are served locally; HTMX indicator styles are defined in the local CSS rather than injected inline, honoring CSP. Templates do not load scripts, styles, or fonts from a CDN. Theme tokens are in `src/main/frontend/css/tokens.css`; typography uses the platform `system-ui` stack and bundles no font files.

The initial shell is server-rendered at `/es` and `/en`. Public/account/backoffice layouts share accessible navigation, skip link, language/theme controls, security metadata, and branded error views. Theme selection persists in a technical cookie and local storage for anonymous users and through the identity-backed `ThemePreference` SPI for authenticated users. The `LocalePreference` SPI similarly applies the authenticated profile locale. Account and backoffice business routes remain default-denied except for the identity flows described above.

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

The current Playwright suite discovers 16 tests covering the public shell across ES/EN, light/dark, mobile/desktop, theme persistence, language switching, navigation overflow/focus, HTMX CSRF/errors, consent-gated frontend error reporting, draft visibility, cookies, local assets, screenshots under `visual/`, and axe checks. `npm run e2e -- --list` passed on 2026-11-17. The previous 15-test runtime pass against the updated local H2 application was on 2026-11-13; the added Prompt 08 test still needs execution against a running application. Tests fail with an explicit precondition message when `PORTAL_E2E_BASE_URL` is not set; a missing server is not reported as a successful test.

PROMPT 04 is complete for ERR-001..003. `npm run build` passed; `mvn -o -s settings-s-portal.xml -Plocal-h2 verify` passed with 76 Surefire tests, 3 Failsafe integration tests, and JaCoCo checks; all 15 Playwright tests passed against the updated local H2 application, including opt-in, rejection/revocation, and browser-storage failure. The focused documentation, governance, i18n, and template gates also passed (19 tests). The full suite exposed and led to fixing a Thymeleaf 3.1 incompatibility: the layout no longer accesses the disabled `#request` expression object and receives the correlation ID via `WebViewAdvice`. The startup log showed the application needed about 36 seconds to start; earlier probes ran too soon. SQL Server compatibility, multi-replica storage/rate limiting, retention, and legal review of the provisional consent copy remain production follow-ups, not gates for closing PROMPT 04.

## Governance and architecture documentation

- [P01–P12 modular development rules](docs/arquitectura/NORMAS-DESARROLLO-MODULAR.md)
- [Current route/UI contract](docs/arquitectura/rutas-ui.md)
- [Module diagram](docs/arquitectura/modulos.mmd)
- [Technical inventory](INVENTARIO.md) and [development diary](DIARIO.md)
- [ADR register/template](docs/adr/TEMPLATE.md)
- [Copilot repository instructions](.github/copilot-instructions.md) and the [S-portal delivery skill](.github/skills/sportal-modular/SKILL.md)

PROMPT 03 is complete under the revised scope (UIK-002..UIK-009 and GOV-001..GOV-006). The latest `mvn clean verify -Plocal-h2` passes (73 tests, no failures or errors; JaCoCo passes), as do the focused UI/GOV tests, direct frontend build, and all 13 Playwright tests against the local H2 application, including axe and the visual matrix. The Maven `frontend-build` profile remains configured for CI but was not run locally; it is not a gate in the current PROMPT 03 scope. See the task sheet for evidence.
