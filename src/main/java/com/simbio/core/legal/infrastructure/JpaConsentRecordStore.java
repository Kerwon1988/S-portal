package com.simbio.core.legal.infrastructure;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.legal.api.ConsentPurpose;
import com.simbio.core.legal.application.ConsentRecordStore;
import com.simbio.core.legal.domain.ConsentRecordEntity;

@Repository
class JpaConsentRecordStore implements ConsentRecordStore {

    private final ConsentRecordJpaRepository records;

    JpaConsentRecordStore(ConsentRecordJpaRepository records) {
        this.records = records;
    }

    @Override
    @Transactional
    public ConsentRecordEntity save(ConsentRecordEntity record) {
        return records.save(Objects.requireNonNull(record));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySubjectAndPurpose(UUID subjectUserId, ConsentPurpose purpose) {
        return records.existsBySubjectUserIdAndPurposeCode(subjectUserId, purpose.name());
    }
}
