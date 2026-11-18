package com.simbio.core.identity.api;

import java.util.UUID;

public record InvitationAccepted(UUID userId, String roleCode, UUID invitationReference) {

    public InvitationAccepted {
        if (userId == null || roleCode == null
                || !roleCode.equals("CLIENT") && !roleCode.equals("EMPLOYEE")) {
            throw new IllegalArgumentException("Invalid invitation acceptance event");
        }
    }

    public InvitationAccepted(UUID userId) {
        this(userId, "CLIENT", null);
    }

    public InvitationAccepted(UUID userId, String roleCode) {
        this(userId, roleCode, null);
    }
}
