package com.simbio.core.request.api;

import java.util.UUID;

public record QuoteDecisionMade(UUID requestPublicId, UUID quotePublicId, int versionNumber,
        QuoteDecision decision) {
}
