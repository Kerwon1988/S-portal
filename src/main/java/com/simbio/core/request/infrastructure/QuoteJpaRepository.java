package com.simbio.core.request.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.request.domain.QuoteEntity;

interface QuoteJpaRepository extends JpaRepository<QuoteEntity, Long> {
    Optional<QuoteEntity> findByRequestId(Long requestId);
    Optional<QuoteEntity> findByPublicId(UUID publicId);
}
