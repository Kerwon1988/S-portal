package com.simbio.core.messaging.domain;

import java.util.Objects;
import java.util.UUID;

public final class AiReplyDraft {

    private static final int MAX_BODY_LENGTH = 10_000;

    private final UUID publicId;
    private final UUID conversationPublicId;
    private final UUID sourceMessagePublicId;
    private final String body;

    private AiReplyDraft(UUID publicId, UUID conversationPublicId, UUID sourceMessagePublicId, String body) {
        this.publicId = Objects.requireNonNull(publicId, "publicId must not be null");
        this.conversationPublicId = Objects.requireNonNull(conversationPublicId, "conversationPublicId must not be null");
        this.sourceMessagePublicId = Objects.requireNonNull(sourceMessagePublicId, "sourceMessagePublicId must not be null");
        if (body == null || body.isBlank() || body.length() > MAX_BODY_LENGTH) {
            throw new IllegalArgumentException("Draft body must contain 1 to 10000 characters");
        }
        this.body = body;
    }

    public static AiReplyDraft pendingApproval(
            UUID publicId,
            UUID conversationPublicId,
            UUID sourceMessagePublicId,
            String body) {
        return new AiReplyDraft(publicId, conversationPublicId, sourceMessagePublicId, body);
    }

    public UUID publicId() {
        return publicId;
    }

    public UUID conversationPublicId() {
        return conversationPublicId;
    }

    public UUID sourceMessagePublicId() {
        return sourceMessagePublicId;
    }

    public String body() {
        return body;
    }

    public AiDraftStatus status() {
        return AiDraftStatus.PENDING_APPROVAL;
    }

    @Override
    public String toString() {
        return "AiReplyDraft[publicId=" + publicId + ", conversationPublicId=" + conversationPublicId
                + ", sourceMessagePublicId=" + sourceMessagePublicId + ", status=" + status() + "]";
    }
}
