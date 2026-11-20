package com.simbio.core.simbiosphere.api;

import java.util.UUID;

public record LinkedBusinessSummary(
        UUID publicId,
        UUID clientAccountPublicId,
        String externalBusinessId,
        String businessName,
        String businessType,
        String tenantSlug) {
}
