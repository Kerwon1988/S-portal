package com.simbio.core.request.api;

import java.util.List;
import java.util.UUID;

public record RequestAssigned(UUID requestPublicId, UUID responsibleStaffId, List<UUID> collaboratorIds) {
    public RequestAssigned { collaboratorIds = List.copyOf(collaboratorIds); }
}
