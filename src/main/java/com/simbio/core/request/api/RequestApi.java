package com.simbio.core.request.api;

import java.util.Optional;
import java.util.UUID;

public interface RequestApi {
    Optional<RequestSummary> findByPublicId(UUID publicId);
}
