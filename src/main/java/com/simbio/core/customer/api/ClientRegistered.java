package com.simbio.core.customer.api;

import java.util.UUID;

public record ClientRegistered(UUID clientAccountPublicId, UUID ownerUserId) {

    public ClientRegistered {
        if (clientAccountPublicId == null || ownerUserId == null) {
            throw new IllegalArgumentException("Client registration identifiers are required");
        }
    }
}
