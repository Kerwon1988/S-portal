package com.simbio.core.staff.api;

import java.util.Optional;
import java.util.UUID;

import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.paging.PageResult;

public interface StaffDirectory {

    PageResult<StaffSummary> search(String search, String status, PageQuery page);

    Optional<StaffSummary> findByUserId(UUID userPublicId);
}
