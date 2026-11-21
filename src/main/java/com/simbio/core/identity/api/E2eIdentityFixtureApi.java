package com.simbio.core.identity.api;

import java.util.UUID;

public interface E2eIdentityFixtureApi {
    UUID ensureVerifiedUser(String email, String password, String displayName, Role role);
}
