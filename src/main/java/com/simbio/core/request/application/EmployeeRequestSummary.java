package com.simbio.core.request.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.simbio.core.request.api.RequestStatus;
import com.simbio.core.request.api.RequestType;

public record EmployeeRequestSummary(
        UUID publicId,
        String reference,
        RequestType type,
        String category,
        String title,
        RequestStatus status,
        String priority,
        BigDecimal estimateHours,
        UUID ownerAccountId,
        UUID ownerUserId,
        Instant createdAt,
        UUID responsibleStaffId,
        boolean overdue) {
}
