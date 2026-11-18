package com.simbio.core.identity.application;

import java.util.UUID;

public record ConsumedToken(
        UUID userPublicId,
        String invitationEmail,
        String roleCode,
        UUID invitationReference) {

    public ConsumedToken(UUID userPublicId, String invitationEmail, String roleCode) {
        this(userPublicId, invitationEmail, roleCode, null);
    }

    @Override
    public String toString() {
        return "ConsumedToken[userPublicId=" + userPublicId
                + ", invitationEmail=<redacted>, roleCode=" + roleCode
                + ", invitationReference=" + invitationReference + "]";
    }
}
