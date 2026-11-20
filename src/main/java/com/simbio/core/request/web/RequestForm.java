package com.simbio.core.request.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RequestForm(
        @NotBlank @Size(max = 40) String category,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 10000) String description,
        @Size(max = 2000) String objective,
        @Size(max = 4000) String impact,
        @Pattern(regexp = "[A-Za-z0-9_-]{0,80}") String relatedModule,
        @DecimalMin("0") BigDecimal estimatedBudget,
        LocalDate targetDate) {
}
