package com.simbio.core.identity.application;

import java.util.Optional;
import java.util.UUID;

import com.simbio.core.identity.domain.MfaFactor;

public interface MfaFactorStore {

    Optional<MfaFactor> find(UUID userId);

    void save(MfaFactor factor);
}
