package com.simbio.core.identity.application;

import java.util.UUID;

public interface SessionInvalidator {

    void invalidate(UUID userId);
}
