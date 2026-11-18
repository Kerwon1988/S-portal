package com.simbio.core.identity.api;

import java.util.UUID;

import com.simbio.core.shared.security.AuthenticatedUser;

public interface StaffAccountAccessManager {

    void setActive(AuthenticatedUser actor, UUID employeeUserId, boolean active);
}
