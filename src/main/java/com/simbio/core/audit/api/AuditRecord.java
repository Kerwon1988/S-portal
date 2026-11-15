package com.simbio.core.audit.api;

import java.util.UUID;

public record AuditRecord(
        UUID actorId,
        String actionCode,
        String entityType,
        UUID entityPublicId,
        String correlationId) {
}
