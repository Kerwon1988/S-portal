package com.simbio.core.staff.api;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StaffSummary(
        UUID userPublicId,
        String jobTitle,
        LocalDate employmentStartDate,
        String status,
        List<String> specialties,
        Instant createdAt) {

    public StaffSummary {
        specialties = List.copyOf(specialties);
    }
}
