package com.simbio.core.messaging.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.ConversationStatus;

@Entity
@Table(name = "conversation", schema = "messaging", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(name = "uq_messaging_conversation_context",
                columnNames = {"context_type", "context_public_id"})
})
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 16)
    private ConversationContextType contextType;

    @Column(name = "context_public_id", nullable = false)
    private UUID contextPublicId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false, length = 16)
    private ConversationStatus status;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ConversationEntity() {
    }

    public ConversationEntity(
            UUID publicId,
            ConversationContextType contextType,
            UUID contextPublicId,
            UUID ownerUserId,
            Instant createdAt) {
        this.publicId = Objects.requireNonNull(publicId, "publicId must not be null");
        this.contextType = Objects.requireNonNull(contextType, "contextType must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (contextType == ConversationContextType.REQUEST && (contextPublicId == null || ownerUserId == null)) {
            throw new IllegalArgumentException("Request conversations require a request and owner");
        }
        if (contextType == ConversationContextType.CONTACT && contextPublicId == null) {
            throw new IllegalArgumentException("Contact conversations require a context reference");
        }
        this.contextPublicId = contextPublicId;
        this.ownerUserId = ownerUserId;
        this.status = ConversationStatus.OPEN;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public ConversationContextType getContextType() {
        return contextType;
    }

    public UUID getContextPublicId() {
        return contextPublicId;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }

    public void close() {
        this.status = ConversationStatus.CLOSED;
    }

    @Override
    public String toString() {
        return "ConversationEntity[publicId=" + publicId + ", contextType=" + contextType
                + ", status=" + status + "]";
    }
}
