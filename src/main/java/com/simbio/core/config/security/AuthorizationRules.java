package com.simbio.core.config.security;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpMethod;

public final class AuthorizationRules {

    private static final List<RoutePermission> ROUTES = List.of(
            new RoutePermission(HttpMethod.GET, "/gestion", "REQUESTS_VIEW_ALL"),
            new RoutePermission(HttpMethod.GET, "/gestion/inicio", "REQUESTS_VIEW_ALL"),
            new RoutePermission(HttpMethod.GET, "/gestion/carga", "REQUESTS_VIEW_ALL"),
            new RoutePermission(HttpMethod.GET, "/gestion/solicitudes/export.csv", "REQUESTS_VIEW_ALL"),
            new RoutePermission(HttpMethod.GET, "/gestion/solicitudes", "REQUESTS_VIEW_ALL"),
            new RoutePermission(HttpMethod.GET, "/gestion/solicitudes/resultados", "REQUESTS_VIEW_ALL"),
            new RoutePermission(HttpMethod.GET, "/gestion/solicitudes/*", "REQUESTS_VIEW_ALL"),
            new RoutePermission(HttpMethod.GET, "/gestion/solicitudes/*/conversacion", "REQUESTS_VIEW_ALL"),
            new RoutePermission(HttpMethod.GET, "/gestion/solicitudes/*/adjuntos/*", "REQUESTS_VIEW_ALL"),
            new RoutePermission(HttpMethod.GET, "/gestion/solicitudes/*/presupuesto", "REQUESTS_VIEW_ALL"),
            new RoutePermission(HttpMethod.POST, "/gestion/solicitudes/*/asignacion", "REQUESTS_ASSIGN"),
            new RoutePermission(HttpMethod.POST, "/gestion/solicitudes/*/operacion", "REQUESTS_VIEW_ALL"),
            new RoutePermission(HttpMethod.POST, "/gestion/solicitudes/*/mensajes", "REQUESTS_VIEW_ALL"),
            new RoutePermission(HttpMethod.POST, "/gestion/solicitudes/*/presupuesto", "QUOTES_MANAGE"),
            new RoutePermission(HttpMethod.POST, "/gestion/solicitudes/*/presupuesto/*/enviar", "QUOTES_MANAGE"),
            new RoutePermission(HttpMethod.GET, "/gestion/auditoria", "AUDIT_VIEW"),
            new RoutePermission(HttpMethod.GET, "/gestion/auditoria/usuarios/*/panel", "AUDIT_VIEW"),
            new RoutePermission(HttpMethod.GET, "/gestion/auditoria/**", "AUDIT_VIEW"),
            new RoutePermission(HttpMethod.GET, "/gestion/bandeja", "INBOX_MANAGE"),
            new RoutePermission(HttpMethod.POST, "/gestion/bandeja/buscar", "INBOX_MANAGE"),
            RoutePermission.anyOf(HttpMethod.GET, "/gestion/bandeja/*", "INBOX_MANAGE", "REQUESTS_VIEW_ALL"),
            new RoutePermission(HttpMethod.GET, "/gestion/bandeja/*/adjuntos/*", "INBOX_MANAGE"),
            RoutePermission.anyOf(
                    HttpMethod.POST, "/gestion/bandeja/*/responder", "INBOX_MANAGE", "REQUESTS_VIEW_ALL"),
            new RoutePermission(HttpMethod.GET, "/gestion/contenido", "CONTENT_EDIT"),
            new RoutePermission(HttpMethod.GET, "/gestion/contenido/**", "CONTENT_EDIT"),
            new RoutePermission(HttpMethod.POST, "/gestion/contenido/**", "CONTENT_EDIT"),
            new RoutePermission(HttpMethod.GET, "/gestion/errores", "ERROR_INVENTORY_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.POST, "/gestion/errores/**", "ERROR_INVENTORY_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.GET, "/gestion/provisioning", "PROVISIONING_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.POST, "/gestion/provisioning/*/retry", "PROVISIONING_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.GET, "/gestion/clientes", "USERS_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.GET, "/gestion/clientes/nuevo", "USERS_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.POST, "/gestion/clientes/nuevo", "USERS_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.GET, "/gestion/clientes/export.csv", "CLIENTS_EXPORT", "ADMIN"),
            new RoutePermission(HttpMethod.GET, "/gestion/trabajadores", "USERS_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.GET, "/gestion/trabajadores/invitacion", "STAFF_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.POST, "/gestion/trabajadores/invitacion", "STAFF_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.GET, "/gestion/trabajadores/*", "STAFF_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.POST, "/gestion/trabajadores/*", "STAFF_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.GET, "/gestion/usuarios/**", "USERS_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.POST, "/gestion/usuarios/**", "USERS_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.GET, "/gestion/identidad/invitaciones", "USERS_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.POST, "/gestion/identidad/invitaciones", "USERS_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.GET,
                    "/gestion/identidad/invitaciones/pendientes/clientes", "USERS_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.POST,
                    "/gestion/identidad/invitaciones/pendientes/clientes/*/reenviar", "USERS_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.GET,
                    "/gestion/identidad/invitaciones/pendientes/trabajadores", "STAFF_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.POST,
                    "/gestion/identidad/invitaciones/pendientes/trabajadores/*/reenviar", "STAFF_MANAGE", "ADMIN"));

    private AuthorizationRules() {
    }

    public static List<RoutePermission> routes() {
        return ROUTES;
    }

    public record RoutePermission(
            HttpMethod method,
            String path,
            String permission,
            String role,
            String alternativePermission) {
        public RoutePermission(HttpMethod method, String path, String permission) {
            this(method, path, permission, null, null);
        }

        public RoutePermission(HttpMethod method, String path, String permission, String role) {
            this(method, path, permission, role, null);
        }

        public static RoutePermission anyOf(
                HttpMethod method,
                String path,
                String permission,
                String alternativePermission) {
            return new RoutePermission(method, path, permission, null, alternativePermission);
        }

        public Set<String> permissions() {
            return alternativePermission == null
                    ? Set.of(permission)
                    : Set.of(permission, alternativePermission);
        }
    }
}
