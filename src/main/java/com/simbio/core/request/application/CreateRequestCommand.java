package com.simbio.core.request.application;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.simbio.core.request.api.RequestType;

public record CreateRequestCommand(
        RequestType type,
        String category,
        String title,
        String description,
        String objective,
        String impact,
        String relatedModule,
        BigDecimal estimatedBudget,
        LocalDate targetDate) {
}
