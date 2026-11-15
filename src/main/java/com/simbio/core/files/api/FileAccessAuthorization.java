package com.simbio.core.files.api;

import java.util.UUID;

@FunctionalInterface
public interface FileAccessAuthorization {

    boolean mayRead(UUID filePublicId);
}
