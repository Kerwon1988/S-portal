package com.simbio.core.messaging.api;

import java.util.UUID;

public record MessageReceived(UUID conversationPublicId, UUID messagePublicId) {

    public MessageReceived {
        if (conversationPublicId == null || messagePublicId == null) {
            throw new IllegalArgumentException("Message event identifiers are required");
        }
    }
}
