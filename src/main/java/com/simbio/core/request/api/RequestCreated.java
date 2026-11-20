package com.simbio.core.request.api;

import java.util.UUID;

public record RequestCreated(UUID requestPublicId, UUID ownerAccountId) {
}
