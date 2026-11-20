package com.simbio.core.messaging.application;

import java.time.Instant;
import java.util.UUID;

import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.ConversationStatus;

public record InboxConversationSummary(
        UUID publicId,
        ConversationContextType contextType,
        ConversationStatus status,
        UUID assignedTo,
        Instant createdAt) {
}
