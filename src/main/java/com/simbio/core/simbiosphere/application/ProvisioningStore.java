package com.simbio.core.simbiosphere.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.simbio.core.simbiosphere.domain.AccountLinkEntity;
import com.simbio.core.simbiosphere.domain.LinkedBusinessEntity;
import com.simbio.core.simbiosphere.domain.ProvisioningEntity;

public interface ProvisioningStore {
    ProvisioningEntity save(ProvisioningEntity provisioning);
    Optional<ProvisioningEntity> findProvisioning(UUID clientAccountPublicId);
    Optional<ProvisioningEntity> findByPublicId(UUID publicId);
    Optional<ProvisioningEntity> claimNext(Instant now, String worker, Instant leaseUntil);
    AccountLinkEntity save(AccountLinkEntity accountLink);
    LinkedBusinessEntity save(LinkedBusinessEntity business);
}
