package com.simbio.core.messaging.infrastructure;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.simbio.core.messaging.application.ThreadAccessTokenStore;
import com.simbio.core.messaging.domain.ThreadAccessTokenEntity;

@Repository
class JpaThreadAccessTokenStore implements ThreadAccessTokenStore {

    private final ThreadAccessTokenJpaRepository repository;

    JpaThreadAccessTokenStore(ThreadAccessTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ThreadAccessTokenEntity save(ThreadAccessTokenEntity token) {
        return repository.save(token);
    }

    @Override
    public Optional<ThreadAccessTokenEntity> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash);
    }
}
