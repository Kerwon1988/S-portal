package com.simbio.core.content.api;

import java.util.Objects;

public record RequestConditions(String locale, boolean approved, int version, String title, SanitizedHtml body) {

    public RequestConditions {
        Objects.requireNonNull(locale, "locale must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(body, "body must not be null");
    }
}
