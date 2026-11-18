package com.simbio.core.notification.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_message", schema = "notification")
public class OutboxMessage {

    @Id
    private UUID id;
    @Column(name = "payload_encrypted", length = 65535)
    private String payloadEncrypted;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "available_at", nullable = false)
    private Instant availableAt;
    @Column(name = "claimed_until")
    private Instant claimedUntil;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "sent_at")
    private Instant sentAt;
    @Column(name = "failure_code", length = 64)
    private String failureCode;

    protected OutboxMessage() {
    }

    public OutboxMessage(UUID id, String payloadEncrypted, Instant now) {
        this.id = id;
        this.payloadEncrypted = payloadEncrypted;
        this.status = "PENDING";
        this.availableAt = now;
        this.createdAt = now;
    }

    public void claimUntil(Instant until) {
        status = "PROCESSING";
        claimedUntil = until;
        attemptCount++;
    }

    public void markSent(Instant now) {
        status = "SENT";
        sentAt = now;
        claimedUntil = null;
        failureCode = null;
    }

    public void scheduleRetry(Instant availableAt, String code) {
        status = "PENDING";
        this.availableAt = availableAt;
        claimedUntil = null;
        failureCode = code;
    }

    public void markFailed(String code) {
        status = "FAILED";
        claimedUntil = null;
        failureCode = code;
    }

    public void purgePayload() {
        payloadEncrypted = null;
    }

    public UUID getId() {
        return id;
    }

    public String getPayloadEncrypted() {
        return payloadEncrypted;
    }

    public String getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public Instant getClaimedUntil() {
        return claimedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public String getFailureCode() {
        return failureCode;
    }
}
