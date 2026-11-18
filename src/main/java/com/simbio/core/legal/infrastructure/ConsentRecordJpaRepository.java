package com.simbio.core.legal.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.legal.api.ConsentPurpose;
import com.simbio.core.legal.domain.ConsentRecordEntity;

interface ConsentRecordJpaRepository extends JpaRepository<ConsentRecordEntity, Long> {

    boolean existsBySubjectUserIdAndPurposeCode(UUID subjectUserId, String purposeCode);
}
