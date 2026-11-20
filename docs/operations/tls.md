# TLS y despliegue tras proxy

La topología preparada publica únicamente 80/443 del reverse proxy; la aplicación expone 8090 solo en red interna. El puerto Actuator 8091 no se publica. TLS termina en nginx con TLS 1.2/1.3; los secretos/certificados se montan desde `deploy/tls`, nunca desde el repositorio.

El host debe mantener redirección HTTP→HTTPS, HSTS, renovación y rotación de certificados, cabeceras forwarded controladas y logs sin query string sensible. `PORTAL_BASE_URL` debe ser HTTPS absoluta en perfil `prod`.

Estado Docker/TLS/health: **PREPARADO_NO_EJECUTADO**. Build de imagen, escaneo de imagen, healthcheck y smoke HTTPS requieren imagen/JAR de release, runtime de contenedores, certificados y plataforma autorizada. No se ejecutaron y no certifican Docker ni PRE.
