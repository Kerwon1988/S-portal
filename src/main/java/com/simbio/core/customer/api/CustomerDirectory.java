package com.simbio.core.customer.api;

import java.util.Optional;
import java.util.UUID;

import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.paging.PageResult;

public interface CustomerDirectory {

    PageResult<CustomerAccountSummary> search(String search, String status, PageQuery page);

    Optional<CustomerAccountSummary> findOwnedBy(UUID userPublicId);
}
