package com.simbio.core.simbiosphere.api;

import java.util.List;
import java.util.UUID;

public interface LinkedBusinessQuery {
    List<LinkedBusinessSummary> findOwnedBy(UUID userPublicId);
    LinkedBusinessSummary requireOwnedBy(UUID userPublicId, UUID businessPublicId);
}
