package com.simbio.core.identity.api;

import java.util.UUID;

public interface IdentityUserLookup {

    boolean existsById(UUID userId);
}
