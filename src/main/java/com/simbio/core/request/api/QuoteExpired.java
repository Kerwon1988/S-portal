package com.simbio.core.request.api;

import java.util.UUID;

public record QuoteExpired(UUID requestPublicId, UUID quotePublicId, int versionNumber) {
}
