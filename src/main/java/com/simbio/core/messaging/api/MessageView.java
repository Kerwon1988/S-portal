package com.simbio.core.messaging.api;

import java.time.Instant;
import java.util.UUID;

public record MessageView(
        UUID publicId,
        MessageAuthorType authorType,
        MessageVisibility visibility,
        String body,
        Instant createdAt) {
}
