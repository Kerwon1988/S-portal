package com.simbio.core.messaging.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.messaging.domain.ThreadAccessTokenEntity;

interface ThreadAccessTokenJpaRepository extends JpaRepository<ThreadAccessTokenEntity, Long> {

    Optional<ThreadAccessTokenEntity> findByTokenHash(String tokenHash);
}
