package com.simbio.core.request.api;

import java.util.UUID;

public record RequestStatusChanged(UUID requestPublicId, RequestStatus previous, RequestStatus current) {
}
