package com.simbio.core.identity.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.identity.application.MfaFactorStore;
import com.simbio.core.identity.domain.MfaFactor;

@Repository
class JpaMfaFactorStore implements MfaFactorStore {

    private final MfaFactorJpaRepository factors;

    JpaMfaFactorStore(MfaFactorJpaRepository factors) {
        this.factors = factors;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MfaFactor> find(UUID userId) {
        return factors.findByUserPublicId(userId);
    }

    @Override
    @Transactional
    public void save(MfaFactor factor) {
        factors.save(factor);
    }
}
