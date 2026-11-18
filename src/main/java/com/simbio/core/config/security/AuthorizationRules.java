package com.simbio.core.config.security;

import java.util.List;

import org.springframework.http.HttpMethod;

public final class AuthorizationRules {

    private static final List<RoutePermission> ROUTES = List.of(
            new RoutePermission(HttpMethod.GET, "/gestion/auditoria", "AUDIT_VIEW"),
            new RoutePermission(HttpMethod.GET, "/gestion/auditoria/usuarios/*/panel", "AUDIT_VIEW"),
            new RoutePermission(HttpMethod.GET, "/gestion/auditoria/**", "AUDIT_VIEW"),
            new RoutePermission(HttpMethod.GET, "/gestion/contenido", "CONTENT_EDIT"),
            new RoutePermission(HttpMethod.GET, "/gestion/contenido/**", "CONTENT_EDIT"),
            new RoutePermission(HttpMethod.POST, "/gestion/contenido/**", "CONTENT_EDIT"),
            new RoutePermission(HttpMethod.GET, "/gestion/errores", "ERROR_INVENTORY_MANAGE", "ADMIN"),
            new RoutePermission(HttpMethod.POST, "/gestion/errores/**", "ERROR_INVENTORY_MANAGE", "ADMIN"),
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
            new RoutePermission(HttpMethod.POST, "/gestion/identidad/invitaciones", "USERS_MANAGE", "ADMIN"));

    private AuthorizationRules() {
    }

    public static List<RoutePermission> routes() {
        return ROUTES;
    }

    public record RoutePermission(HttpMethod method, String path, String permission, String role) {
        public RoutePermission(HttpMethod method, String path, String permission) {
            this(method, path, permission, null);
        }
    }
}
