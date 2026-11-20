package com.simbio.core.request.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.simbio.core.request.api.QuoteStatus;

public record QuoteVersionView(
        UUID quotePublicId,
        int versionNumber,
        BigDecimal netAmount,
        String currency,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal grossAmount,
        LocalDate validUntil,
        QuoteStatus status,
        Instant createdAt,
        Instant sentAt,
        UUID decidedBy,
        Instant decidedAt) {
}
