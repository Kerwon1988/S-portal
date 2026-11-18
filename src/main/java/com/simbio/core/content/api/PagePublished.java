package com.simbio.core.content.api;

import java.util.Objects;
import java.util.UUID;

public record PagePublished(UUID pagePublicId, UUID translationPublicId) {

    public PagePublished {
        Objects.requireNonNull(pagePublicId, "pagePublicId must not be null");
        Objects.requireNonNull(translationPublicId, "translationPublicId must not be null");
    }
}
