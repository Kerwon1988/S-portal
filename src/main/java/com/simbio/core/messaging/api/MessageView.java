package com.simbio.core.messaging.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageView(
        UUID publicId,
        MessageAuthorType authorType,
        MessageVisibility visibility,
        String body,
        Instant createdAt,
        List<MessageAttachmentView> attachments) {

    public MessageView {
        attachments = List.copyOf(attachments);
    }

    public MessageView(
            UUID publicId,
            MessageAuthorType authorType,
            MessageVisibility visibility,
            String body,
            Instant createdAt) {
        this(publicId, authorType, visibility, body, createdAt, List.of());
    }
}
