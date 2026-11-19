package com.simbio.core.messaging.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.simbio.core.messaging.api.MessageAuthorType;
import com.simbio.core.messaging.api.MessageVisibility;

@Entity
@Table(name = "message", schema = "messaging")
public class MessageEntity {

    private static final int MAX_BODY_LENGTH = 10_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @Column(name = "author_id")
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false, length = 16)
    private MessageAuthorType authorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 16)
    private MessageVisibility visibility;

    @Column(name = "body", nullable = false, length = MAX_BODY_LENGTH)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected MessageEntity() {
    }

    public MessageEntity(
            UUID publicId,
            ConversationEntity conversation,
            UUID authorId,
            MessageAuthorType authorType,
            MessageVisibility visibility,
            String body,
            Instant createdAt) {
        this.publicId = Objects.requireNonNull(publicId, "publicId must not be null");
        this.conversation = Objects.requireNonNull(conversation, "conversation must not be null");
        this.authorType = Objects.requireNonNull(authorType, "authorType must not be null");
        this.visibility = Objects.requireNonNull(visibility, "visibility must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (authorType == MessageAuthorType.ANONYMOUS || authorType == MessageAuthorType.SYSTEM) {
            if (authorId != null) {
                throw new IllegalArgumentException("Anonymous and system messages must not carry a user id");
            }
        } else if (authorId == null) {
            throw new IllegalArgumentException("Authenticated messages require an author id");
        }
        if (body == null || body.isBlank() || body.length() > MAX_BODY_LENGTH) {
            throw new IllegalArgumentException("Message body must contain 1 to 10000 characters");
        }
        this.authorId = authorId;
        this.body = body;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public ConversationEntity getConversation() {
        return conversation;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public MessageAuthorType getAuthorType() {
        return authorType;
    }

    public MessageVisibility getVisibility() {
        return visibility;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void markRead(Instant timestamp) {
        this.readAt = Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public String toString() {
        return "MessageEntity[publicId=" + publicId
                + ", authorType=" + authorType + ", visibility=" + visibility + "]";
    }
}
