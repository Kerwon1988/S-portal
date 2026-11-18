package com.simbio.core.shared.spi;

import java.util.UUID;

public record AuditEvent(
        UUID actorId,
        String actionCode,
        String entityType,
        UUID entityPublicId,
        String correlationId) {
}
