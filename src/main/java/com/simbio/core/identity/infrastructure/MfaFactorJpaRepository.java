package com.simbio.core.identity.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.identity.domain.MfaFactor;

interface MfaFactorJpaRepository extends JpaRepository<MfaFactor, UUID> {

    Optional<MfaFactor> findByUserPublicId(UUID userPublicId);
}
