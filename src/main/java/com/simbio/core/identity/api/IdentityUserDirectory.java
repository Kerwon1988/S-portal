package com.simbio.core.identity.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.paging.PageResult;
import com.simbio.core.shared.security.AuthenticatedUser;

public interface IdentityUserDirectory {

    Optional<IdentityUserSummary> find(AuthenticatedUser actor, UUID userPublicId);

    PageResult<IdentityUserSummary> search(
            AuthenticatedUser actor,
            String search,
            String role,
            String status,
            String locale,
            Instant createdFrom,
            Instant createdBefore,
            UserDirectoryOrder order,
            PageQuery page);
}
