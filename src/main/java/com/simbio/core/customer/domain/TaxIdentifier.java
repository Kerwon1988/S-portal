package com.simbio.core.customer.domain;

import java.util.Locale;
import java.util.Objects;

public record TaxIdentifier(String countryCode, String normalizedValue) {

    public TaxIdentifier {
        Objects.requireNonNull(countryCode, "countryCode must not be null");
        Objects.requireNonNull(normalizedValue, "normalizedValue must not be null");
    }

    public static String normalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Tax identifier is invalid");
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
        if (normalized.isEmpty() || normalized.length() > 32) {
            throw new IllegalArgumentException("Tax identifier is invalid");
        }
        return normalized;
    }

    public String masked() {
        int visibleCharacters = Math.min(4, normalizedValue.length());
        return "*".repeat(normalizedValue.length() - visibleCharacters)
                + normalizedValue.substring(normalizedValue.length() - visibleCharacters);
    }
}
