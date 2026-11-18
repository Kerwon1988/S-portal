package com.simbio.core.content.api;

import java.time.Instant;

public record PublishedLegalDocument(String documentType, String locale, int version, String title,
        SanitizedHtml body, Instant effectiveFrom) {
}
