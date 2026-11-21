# Licencias y activos

## Dependencias

`pom.xml`/`package-lock.json` son la fuente observable para dependencias. La generación SBOM debe producir CycloneDX en la pipeline autorizada y registrar versión, licencia, origen y checksum del artefacto. OWASP Dependency-Check permanece como gate de vulnerabilidades.

## Assets

Inventariar cada imagen, vídeo, icono y documento incluido con autor/origen/licencia/uso/atribución. No incorporar material sin derechos de redistribución. La tipografía web queda fuera: se usan fuentes del sistema.

## SQL Server

Edición, términos, TDE, backup, HA y precio/coste están **PENDIENTES DE DECISIÓN/APROBACIÓN**. No afirmar Developer/Standard/Enterprise para producción sin evidencia contractual. ADR-006 registra la decisión pendiente.

El script `npm run sbom` genera un manifest CycloneDX local desde `pom.xml` y `package-lock.json`; esa evidencia de manifiestos no sustituye una revisión de licencias transitivas resuelta desde repositorios/artefactos ni una certificación contractual. Estado de validación oficial/licencias/edición SQL: **PREPARADO_NO_EJECUTADO**.
