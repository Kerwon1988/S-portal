package com.simbio.core.identity.api;

import java.time.Instant;
import java.util.UUID;

public record IdentityOwnerProfile(
        UUID publicId,
        String email,
        String displayName,
        String locale,
        Instant emailVerifiedAt) {

    public boolean isEmailVerified() { return emailVerifiedAt != null; }

    @Override public String toString() {
        return "IdentityOwnerProfile[email=<redacted>, displayName=<redacted>]";
    }
}
