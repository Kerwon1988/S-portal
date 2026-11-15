package com.simbio.core.audit.application;

import java.time.Instant;
import java.util.UUID;

import com.simbio.core.audit.domain.AuditEntryEntity;

public record AuditView(
        UUID actorId,
        String actionCode,
        String entityType,
        UUID entityPublicId,
        Instant occurredAt,
        String correlationId) {

    static AuditView from(AuditEntryEntity entity) {
        return new AuditView(entity.getActorId(), entity.getActionCode(), entity.getEntityType(),
                entity.getEntityPublicId(), entity.getOccurredAt(), entity.getCorrelationId());
    }
}
