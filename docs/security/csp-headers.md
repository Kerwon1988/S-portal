# CSP enforce y cabeceras

CSP es obligatoria en todos los perfiles y usa nonce nuevo por request. No contiene `unsafe-inline` ni `unsafe-eval`. La política permite únicamente origen local; los SVG/data se limitan a imágenes y el micrófono requiere origen propio.

Cabeceras verificadas por `SecurityHeadersTest`:

- `Content-Security-Policy` con nonce y sin `report-only`.
- `Strict-Transport-Security` de un año con `includeSubDomains`.
- `X-Content-Type-Options: nosniff`.
- `X-Frame-Options: DENY` y `frame-ancestors 'none'`.
- `Referrer-Policy: strict-origin-when-cross-origin`.
- `Permissions-Policy` deny-by-default para capacidades sensibles.

## Violation review

No se obtuvo tráfico de navegador PRE. Estado: **PREPARADO_NO_EJECUTADO**. Antes de elevar un candidato, revisar reports de navegador sin cookies/query/PII y corregir recursos reales; no ampliar la política sin owner y justificación.
