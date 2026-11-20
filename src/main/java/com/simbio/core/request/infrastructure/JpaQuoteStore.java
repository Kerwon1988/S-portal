package com.simbio.core.request.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.simbio.core.request.api.QuoteStatus;
import com.simbio.core.request.application.QuoteStore;
import com.simbio.core.request.domain.QuoteEntity;
import com.simbio.core.request.domain.QuoteVersionEntity;

@Repository
class JpaQuoteStore implements QuoteStore {
    private final QuoteJpaRepository quotes;
    private final QuoteVersionJpaRepository versions;

    JpaQuoteStore(QuoteJpaRepository quotes, QuoteVersionJpaRepository versions) {
        this.quotes = quotes;
        this.versions = versions;
    }

    @Override public QuoteEntity save(QuoteEntity quote) { return quotes.save(quote); }
    @Override public Optional<QuoteEntity> findByRequestId(Long requestId) { return quotes.findByRequestId(requestId); }
    @Override public Optional<QuoteEntity> findByPublicId(UUID publicId) { return quotes.findByPublicId(publicId); }
    @Override public Optional<QuoteEntity> findById(Long id) { return quotes.findById(id); }
    @Override public QuoteVersionEntity saveVersion(QuoteVersionEntity version) { return versions.save(version); }
    @Override public List<QuoteVersionEntity> versions(Long quoteId) { return versions.findAllByQuoteIdOrderByVersionNumberDesc(quoteId); }
    @Override public Optional<QuoteVersionEntity> latest(Long quoteId) { return versions.findFirstByQuoteIdOrderByVersionNumberDesc(quoteId); }
    @Override public Optional<QuoteVersionEntity> findVersion(Long quoteId, int versionNumber) {
        return versions.findByQuoteIdAndVersionNumber(quoteId, versionNumber);
    }
    @Override public List<QuoteVersionEntity> findSentExpired(Instant now, Pageable pageable) {
        return versions.findExpired(now.atZone(java.time.ZoneOffset.UTC).toLocalDate(), pageable);
    }
    @Override public long countByStatus(QuoteStatus status) { return versions.countByStatus(status); }
}
