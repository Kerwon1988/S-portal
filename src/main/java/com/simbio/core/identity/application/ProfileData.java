package com.simbio.core.identity.application;

import java.util.UUID;

public record ProfileData(
        UUID userId,
        String email,
        String displayName,
        String locale,
        String theme) {

    @Override
    public String toString() {
        return "ProfileData[userId=" + userId + ", email=<redacted>, displayName=<redacted>, locale="
                + locale + ", theme=" + theme + "]";
    }
}
