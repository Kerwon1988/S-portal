package com.simbio.core.request.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.simbio.core.request.api.QuoteStatus;
import com.simbio.core.request.domain.QuoteEntity;
import com.simbio.core.request.domain.QuoteVersionEntity;

public interface QuoteStore {
    QuoteEntity save(QuoteEntity quote);
    Optional<QuoteEntity> findByRequestId(Long requestId);
    Optional<QuoteEntity> findByPublicId(UUID publicId);
    Optional<QuoteEntity> findById(Long id);
    QuoteVersionEntity saveVersion(QuoteVersionEntity version);
    List<QuoteVersionEntity> versions(Long quoteId);
    Optional<QuoteVersionEntity> latest(Long quoteId);
    Optional<QuoteVersionEntity> findVersion(Long quoteId, int versionNumber);
    List<QuoteVersionEntity> findSentExpired(Instant now, Pageable pageable);
    long countByStatus(QuoteStatus status);
}
