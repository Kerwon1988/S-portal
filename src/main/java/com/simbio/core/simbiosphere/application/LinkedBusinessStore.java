package com.simbio.core.simbiosphere.application;

import java.util.List;
import java.util.UUID;

import com.simbio.core.simbiosphere.domain.LinkedBusinessEntity;

public interface LinkedBusinessStore {
    List<LinkedBusinessEntity> findByClientAccount(UUID clientAccountPublicId);
}
