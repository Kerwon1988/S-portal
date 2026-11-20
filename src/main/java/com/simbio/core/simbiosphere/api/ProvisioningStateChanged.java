package com.simbio.core.simbiosphere.api;

import java.time.Instant;
import java.util.UUID;

public record ProvisioningStateChanged(
        UUID provisioningPublicId,
        UUID clientAccountPublicId,
        ProvisioningStatus status,
        Instant occurredAt) {
}
