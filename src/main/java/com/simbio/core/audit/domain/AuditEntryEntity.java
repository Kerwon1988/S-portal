package com.simbio.core.audit.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_entry", schema = "audit")
public class AuditEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "action_code", nullable = false, length = 64)
    private String actionCode;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_public_id")
    private UUID entityPublicId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    protected AuditEntryEntity() {
    }

    public AuditEntryEntity(UUID actorId, String actionCode, String entityType, UUID entityPublicId,
            Instant occurredAt, String correlationId) {
        this.actorId = actorId;
        this.actionCode = actionCode;
        this.entityType = entityType;
        this.entityPublicId = entityPublicId;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
    }

    @PreUpdate
    private void rejectUpdates() {
        throw new UnsupportedOperationException("Audit entries are append-only");
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityPublicId() {
        return entityPublicId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
