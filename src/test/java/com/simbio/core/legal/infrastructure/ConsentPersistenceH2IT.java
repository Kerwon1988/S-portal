package com.simbio.core.legal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.legal.api.ConsentAcceptance;
import com.simbio.core.legal.api.ConsentPurpose;
import com.simbio.core.legal.api.ConsentSource;
import com.simbio.core.legal.application.ConsentRecordStore;
import com.simbio.core.legal.domain.ConsentRecordEntity;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConsentRecordStore.class)
class ConsentPersistenceH2IT {

    @Autowired
    private ConsentRecordStore store;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    @DisplayName("TC-LGL-001: H2 stores versioned consent evidence without storing request-supplied time")
    void persistsVersionPurposeSourceAndSubjectInH2() {
        UUID subjectId = UUID.randomUUID();
        ConsentAcceptance acceptance = new ConsentAcceptance(subjectId,
                ConsentPurpose.PRIVACY_ACKNOWLEDGEMENT, "en", 2, ConsentSource.REGISTRATION);

        store.save(new ConsentRecordEntity(UUID.randomUUID(), acceptance,
                Instant.parse("2026-11-18T08:00:00Z")));
        entityManager.flush();
        entityManager.clear();

        assertThat(store.existsBySubjectAndPurpose(subjectId, ConsentPurpose.PRIVACY_ACKNOWLEDGEMENT)).isTrue();
        assertThat(store.existsBySubjectAndPurpose(subjectId, ConsentPurpose.MARKETING_COMMUNICATIONS)).isFalse();
    }
}
