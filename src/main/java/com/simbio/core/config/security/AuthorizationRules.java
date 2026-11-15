package com.simbio.core.config.security;

import java.util.List;

import org.springframework.http.HttpMethod;

public final class AuthorizationRules {

    private static final List<RoutePermission> ROUTES = List.of(
            new RoutePermission(HttpMethod.GET, "/gestion/auditoria", "AUDIT_VIEW"),
            new RoutePermission(HttpMethod.GET, "/gestion/auditoria/**", "AUDIT_VIEW"));

    private AuthorizationRules() {
    }

    public static List<RoutePermission> routes() {
        return ROUTES;
    }

    public record RoutePermission(HttpMethod method, String path, String permission) {
    }
}
