package com.simbio.core.content.api;

import java.util.Objects;

public record SanitizedHtml(String value) {

    public SanitizedHtml {
        Objects.requireNonNull(value, "value must not be null");
    }
}
