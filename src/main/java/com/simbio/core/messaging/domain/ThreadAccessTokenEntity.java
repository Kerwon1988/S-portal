package com.simbio.core.messaging.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "thread_access_token", schema = "messaging")
public class ThreadAccessTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected ThreadAccessTokenEntity() {
    }

    public ThreadAccessTokenEntity(
            UUID publicId,
            ConversationEntity conversation,
            String tokenHash,
            Instant expiresAt) {
        this.publicId = Objects.requireNonNull(publicId);
        this.conversation = Objects.requireNonNull(conversation);
        this.tokenHash = requireHash(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
    }

    public boolean isUsable(Instant now) {
        return consumedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }

    public void consume(Instant now) {
        if (!isUsable(now)) {
            throw new IllegalStateException("Thread access token is no longer usable");
        }
        consumedAt = now;
    }

    public void revoke(Instant now) {
        if (revokedAt == null && consumedAt == null) {
            revokedAt = Objects.requireNonNull(now);
        }
    }

    public UUID getPublicId() {
        return publicId;
    }

    public ConversationEntity getConversation() {
        return conversation;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    private static String requireHash(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Expected a SHA-256 hexadecimal token hash");
        }
        return value;
    }
}
