package com.simbio.core.customer.api;

import java.time.Instant;
import java.util.UUID;

public record CustomerAccountSummary(
        UUID publicId,
        String accountType,
        String displayName,
        String status,
        Instant createdAt) {
}
