package com.simbio.core.messaging.api;

import java.util.UUID;

public record MessagePosted(UUID conversationPublicId, UUID messagePublicId) {

    public MessagePosted {
        if (conversationPublicId == null || messagePublicId == null) {
            throw new IllegalArgumentException("Message event identifiers are required");
        }
    }
}
