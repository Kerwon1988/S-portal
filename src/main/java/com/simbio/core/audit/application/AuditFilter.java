package com.simbio.core.audit.application;

import java.time.Instant;
import java.util.UUID;

public record AuditFilter(UUID actorId, String actionCode, Instant fromInclusive, Instant toExclusive) {
}
