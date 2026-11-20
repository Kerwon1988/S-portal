package com.simbio.core.messaging.application;

import java.util.Optional;

import com.simbio.core.messaging.domain.ThreadAccessTokenEntity;

public interface ThreadAccessTokenStore {

    ThreadAccessTokenEntity save(ThreadAccessTokenEntity token);

    Optional<ThreadAccessTokenEntity> findByTokenHash(String tokenHash);
}
