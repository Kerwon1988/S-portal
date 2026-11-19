package com.simbio.core.messaging.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationView(
        UUID publicId,
        ConversationContextType contextType,
        ConversationStatus status,
        Instant createdAt,
        List<MessageView> messages) {

    public ConversationView {
        messages = List.copyOf(messages);
    }
}
