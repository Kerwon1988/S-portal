package com.simbio.core.identity.api;

import java.time.Instant;
import java.util.UUID;

public record IdentityUserSummary(
        UUID publicId,
        String email,
        String displayName,
        String role,
        String status,
        String locale,
        boolean emailVerified,
        boolean locked,
        Instant createdAt) {
}
