package com.simbio.core.identity.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "one_time_token", schema = "identity")
public class OneTimeToken {

    @Id
    private UUID id;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "purpose", nullable = false, length = 32)
    private String purpose;
    @Column(name = "user_public_id")
    private UUID userPublicId;
    @Column(name = "email_encrypted", length = 2048)
    private String emailEncrypted;
    @Column(name = "email_key", length = 64)
    private String emailKey;
    @Column(name = "role_code", length = 32)
    private String roleCode;
    @Column(name = "invitation_reference")
    private UUID invitationReference;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "consumed_at")
    private Instant consumedAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OneTimeToken() {
    }

    public OneTimeToken(
            UUID id,
            String tokenHash,
            String purpose,
            UUID userPublicId,
            String emailEncrypted,
            String emailKey,
            String roleCode,
            UUID invitationReference,
            Instant expiresAt,
            Instant createdAt) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.purpose = purpose;
        this.userPublicId = userPublicId;
        this.emailEncrypted = emailEncrypted;
        this.emailKey = emailKey;
        this.roleCode = roleCode;
        this.invitationReference = invitationReference;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public OneTimeToken(
            UUID id,
            String tokenHash,
            String purpose,
            UUID userPublicId,
            String emailEncrypted,
            String emailKey,
            String roleCode,
            Instant expiresAt,
            Instant createdAt) {
        this(id, tokenHash, purpose, userPublicId, emailEncrypted, emailKey, roleCode, null, expiresAt, createdAt);
    }

    public OneTimeToken(
            UUID id,
            String tokenHash,
            String purpose,
            UUID userPublicId,
            String emailEncrypted,
            String roleCode,
            Instant expiresAt,
            Instant createdAt) {
        this(id, tokenHash, purpose, userPublicId, emailEncrypted, null, roleCode, expiresAt, createdAt);
    }

    public String getEmailKey() {
        return emailKey;
    }

    public UUID getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getPurpose() {
        return purpose;
    }

    public UUID getUserPublicId() {
        return userPublicId;
    }

    public String getEmailEncrypted() {
        return emailEncrypted;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public UUID getInvitationReference() {
        return invitationReference;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void revoke(Instant now) {
        if (!"INVITATION".equals(purpose) || now == null || consumedAt != null || revokedAt != null) {
            throw new IllegalStateException("Invitation token is not pending");
        }
        revokedAt = now;
    }
}
