package com.simbio.core.request.api;

import java.util.UUID;

public record QuoteSent(UUID requestPublicId, UUID quotePublicId, int versionNumber) {
}
