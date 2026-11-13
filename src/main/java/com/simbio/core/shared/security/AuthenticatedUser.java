package com.simbio.core.shared.security;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(UUID userId, Role role, Set<Permission> permissions) {

    public AuthenticatedUser {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(role, "role must not be null");
        permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions must not be null"));
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(Objects.requireNonNull(permission, "permission must not be null"));
    }
}
