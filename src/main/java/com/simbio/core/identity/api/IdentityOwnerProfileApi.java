package com.simbio.core.identity.api;

import java.util.Optional;
import java.util.UUID;

public interface IdentityOwnerProfileApi {
    Optional<IdentityOwnerProfile> findOwnerProfile(UUID userPublicId);
}
