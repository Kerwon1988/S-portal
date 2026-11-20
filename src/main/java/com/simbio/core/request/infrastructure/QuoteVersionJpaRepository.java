package com.simbio.core.request.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simbio.core.request.api.QuoteStatus;
import com.simbio.core.request.domain.QuoteVersionEntity;

interface QuoteVersionJpaRepository extends JpaRepository<QuoteVersionEntity, Long> {
    List<QuoteVersionEntity> findAllByQuoteIdOrderByVersionNumberDesc(Long quoteId);
    Optional<QuoteVersionEntity> findFirstByQuoteIdOrderByVersionNumberDesc(Long quoteId);
    Optional<QuoteVersionEntity> findByQuoteIdAndVersionNumber(Long quoteId, int versionNumber);
    long countByStatus(QuoteStatus status);

    @Query("""
            select q from QuoteVersionEntity q
            where q.status = com.simbio.core.request.api.QuoteStatus.SENT
              and q.validUntil < :cutoff
            order by q.validUntil asc, q.id asc
            """)
    List<QuoteVersionEntity> findExpired(@Param("cutoff") LocalDate cutoff, Pageable pageable);
}
