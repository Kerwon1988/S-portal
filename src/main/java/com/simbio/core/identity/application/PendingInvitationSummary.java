package com.simbio.core.identity.application;

import java.time.Instant;
import java.util.UUID;

public record PendingInvitationSummary(
        UUID id,
        String maskedEmail,
        String roleCode,
        Instant createdAt,
        Instant expiresAt,
        boolean expired) {
}
