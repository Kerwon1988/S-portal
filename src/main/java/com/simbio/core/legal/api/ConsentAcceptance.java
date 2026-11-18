package com.simbio.core.legal.api;

import java.util.Objects;
import java.util.UUID;

public record ConsentAcceptance(
        UUID subjectUserId,
        ConsentPurpose purpose,
        String locale,
        int documentVersion,
        ConsentSource source) {

    public ConsentAcceptance {
        Objects.requireNonNull(subjectUserId, "subjectUserId must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(source, "source must not be null");
    }
}
