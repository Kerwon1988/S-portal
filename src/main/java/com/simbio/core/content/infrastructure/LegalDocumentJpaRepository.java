package com.simbio.core.content.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simbio.core.content.domain.LegalDocumentVersionEntity;

interface LegalDocumentJpaRepository extends JpaRepository<LegalDocumentVersionEntity, Long> {

    List<LegalDocumentVersionEntity> findAllByOrderByDocumentTypeAscLocaleAscVersionDesc();

    Optional<LegalDocumentVersionEntity> findFirstByDocumentTypeAndLocaleOrderByVersionDesc(
            String documentType, String locale);

    @Query("""
            select document from LegalDocumentVersionEntity document
            where document.documentType = :documentType
              and document.locale = :locale
              and document.approvalState = :approvalState
              and (document.effectiveFrom is null or document.effectiveFrom <= :effectiveAt)
            order by document.version desc
            """)
    List<LegalDocumentVersionEntity> findCurrent(
            @Param("documentType") String documentType,
            @Param("locale") String locale,
            @Param("approvalState") String approvalState,
            @Param("effectiveAt") Instant effectiveAt,
            Pageable pageable);
}
