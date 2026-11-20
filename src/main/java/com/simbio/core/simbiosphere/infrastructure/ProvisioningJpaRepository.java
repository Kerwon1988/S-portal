package com.simbio.core.simbiosphere.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import com.simbio.core.simbiosphere.domain.ProvisioningEntity;

interface ProvisioningJpaRepository extends JpaRepository<ProvisioningEntity, Long> {
    Optional<ProvisioningEntity> findByClientAccountPublicId(UUID clientAccountPublicId);
    Optional<ProvisioningEntity> findByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProvisioningEntity p where p.status = com.simbio.core.simbiosphere.api.ProvisioningStatus.READY "
            + "and (p.nextAttemptAt is null or p.nextAttemptAt <= :now) "
            + "and (p.leaseExpiresAt is null or p.leaseExpiresAt <= :now) order by p.createdAt asc")
    java.util.List<ProvisioningEntity> findClaimable(Instant now, Pageable pageable);
}
