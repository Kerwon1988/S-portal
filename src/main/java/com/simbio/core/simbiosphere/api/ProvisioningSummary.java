package com.simbio.core.simbiosphere.api;

import java.time.Instant;
import java.util.UUID;

public record ProvisioningSummary(
        UUID publicId,
        UUID clientAccountPublicId,
        ProvisioningStatus status,
        String errorCode,
        int attemptCount,
        Instant createdAt,
        Instant updatedAt) {
}
