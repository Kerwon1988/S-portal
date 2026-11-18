package com.simbio.core.identity.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "mfa_factor", schema = "identity")
public class MfaFactor {

    @Id
    @Column(name = "user_public_id")
    private UUID userPublicId;
    @Column(name = "secret_encrypted", nullable = false, length = 2048)
    private String secretEncrypted;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "last_accepted_step", nullable = false)
    private long lastAcceptedStep;
    @Column(name = "recovery_hashes", length = 2048)
    private String recoveryHashes;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    protected MfaFactor() {
    }

    public MfaFactor(UUID userPublicId, String secretEncrypted, Instant now) {
        this.userPublicId = userPublicId;
        this.secretEncrypted = secretEncrypted;
        this.updatedAt = now;
    }

    public void replacePendingSecret(String encryptedSecret, Instant now) {
        if (enabled) {
            throw new IllegalStateException("MFA is already enabled");
        }
        secretEncrypted = encryptedSecret;
        updatedAt = now;
    }

    public void enable(String hashes, long acceptedStep, Instant now) {
        enabled = true;
        recoveryHashes = hashes;
        lastAcceptedStep = acceptedStep;
        updatedAt = now;
    }

    public void replaceRecoveryHashes(String hashes, Instant now) {
        if (!enabled) {
            throw new IllegalStateException("MFA is not enabled");
        }
        recoveryHashes = hashes;
        updatedAt = now;
    }

    public void acceptStep(long step, Instant now) {
        if (!enabled || step <= lastAcceptedStep) {
            throw new IllegalStateException("TOTP step has already been used");
        }
        lastAcceptedStep = step;
        updatedAt = now;
    }

    public boolean consumeRecoveryHash(String hash, Instant now) {
        if (!enabled || recoveryHashes == null) {
            return false;
        }
        java.util.List<String> values = new java.util.ArrayList<>(
                java.util.Arrays.asList(recoveryHashes.split(",")));
        int match = -1;
        byte[] expected = hash.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        for (int index = 0; index < values.size(); index++) {
            if (java.security.MessageDigest.isEqual(
                    values.get(index).getBytes(java.nio.charset.StandardCharsets.US_ASCII), expected)) {
                match = index;
            }
        }
        if (match < 0) {
            return false;
        }
        values.remove(match);
        recoveryHashes = String.join(",", values);
        updatedAt = now;
        return true;
    }

    public UUID getUserPublicId() {
        return userPublicId;
    }

    public String getSecretEncrypted() {
        return secretEncrypted;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getLastAcceptedStep() {
        return lastAcceptedStep;
    }
}
