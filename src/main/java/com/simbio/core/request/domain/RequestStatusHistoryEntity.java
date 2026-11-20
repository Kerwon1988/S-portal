package com.simbio.core.request.domain;

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

import com.simbio.core.request.api.RequestStatus;

@Entity
@Table(name = "request_status_history", schema = "request")
public class RequestStatusHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "request_id", nullable = false)
    private Long requestId;
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    private RequestStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 32)
    private RequestStatus toStatus;
    @Column(name = "actor_id", nullable = false)
    private UUID actorId;
    @Column(name = "reason", length = 1000)
    private String reason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RequestStatusHistoryEntity() {
    }

    public RequestStatusHistoryEntity(Long requestId, RequestStatus fromStatus, RequestStatus toStatus,
            UUID actorId, String reason, Instant createdAt) {
        this.requestId = Objects.requireNonNull(requestId);
        this.fromStatus = fromStatus;
        this.toStatus = Objects.requireNonNull(toStatus);
        this.actorId = Objects.requireNonNull(actorId);
        this.reason = reason == null || reason.isBlank() ? null : reason.trim();
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public RequestStatus getFromStatus() { return fromStatus; }
    public RequestStatus getToStatus() { return toStatus; }
    public UUID getActorId() { return actorId; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
