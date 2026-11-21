package com.simbio.core.simbiosphere.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProvisioningIntent(
        String businessName,
        String businessSlug,
        String businessType,
        String locationName,
        String locationSlug,
        List<String> modules,
        String termsVersion,
        Instant termsAcceptedAt) {

    public ProvisioningIntent {
        businessName = required(businessName, "businessName", 200);
        businessSlug = pattern(businessSlug, "^[a-z0-9-]{3,50}$", "businessSlug");
        businessType = businessType == null ? "" : businessType.trim();
        if (!businessType.matches("RESTAURANT|RETAIL|SERVICES|REPAIR_SHOP")) {
            throw new IllegalArgumentException("Unsupported business type");
        }
        locationName = required(locationName, "locationName", 200);
        locationSlug = pattern(locationSlug, "^[a-z0-9-]{2,50}$", "locationSlug");
        modules = modules == null ? null : List.copyOf(modules);
        if (modules != null && modules.stream().anyMatch(value -> value == null || !value.matches("^[A-Z0-9_-]{1,64}$"))) {
            throw new IllegalArgumentException("Invalid optional module");
        }
        termsVersion = required(termsVersion, "termsVersion", 64);
        termsAcceptedAt = Objects.requireNonNull(termsAcceptedAt, "termsAcceptedAt");
    }

    private static String required(String value, String field, int max) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > max) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return normalized;
    }

    private static String pattern(String value, String regex, String field) {
        String normalized = value == null ? "" : value.trim();
        if (!normalized.matches(regex)) throw new IllegalArgumentException(field + " is invalid");
        return normalized;
    }
}
