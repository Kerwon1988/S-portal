package com.simbio.core.identity.api;

import java.util.UUID;

public record IdentityAccountChanged(UUID userId, String roleCode, String statusCode) {

    public IdentityAccountChanged {
        if (userId == null || roleCode == null
                || !roleCode.equals("ADMIN") && !roleCode.equals("EMPLOYEE") && !roleCode.equals("CLIENT")
                || statusCode == null || !statusCode.equals("ACTIVE") && !statusCode.equals("INACTIVE")) {
            throw new IllegalArgumentException("Invalid identity account change event");
        }
    }
}
