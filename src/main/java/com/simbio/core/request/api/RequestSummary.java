package com.simbio.core.request.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RequestSummary(
        UUID publicId,
        String reference,
        RequestType type,
        String category,
        String title,
        RequestStatus status,
        BigDecimal estimatedBudget,
        LocalDate targetDate,
        Instant createdAt) {
}
