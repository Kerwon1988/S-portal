package com.simbio.core.simbiosphere.application;

import java.time.Instant;
import java.util.UUID;

import com.simbio.core.simbiosphere.api.ProvisioningStatus;

public record AdminProvisioningView(
        UUID publicId,
        UUID clientAccountPublicId,
        ProvisioningStatus status,
        String errorCode,
        int attemptCount,
        Instant nextAttemptAt,
        Instant updatedAt,
        boolean retryable) {
}
