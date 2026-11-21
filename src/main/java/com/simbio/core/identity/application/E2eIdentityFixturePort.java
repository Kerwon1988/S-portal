package com.simbio.core.identity.application;

import java.util.UUID;
import com.simbio.core.identity.api.Role;

public interface E2eIdentityFixturePort {
    UUID ensureVerifiedUser(String email, String password, String displayName, Role role);
}
