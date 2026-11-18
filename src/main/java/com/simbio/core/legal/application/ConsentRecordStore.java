package com.simbio.core.legal.application;

import java.util.UUID;

import com.simbio.core.legal.api.ConsentPurpose;
import com.simbio.core.legal.domain.ConsentRecordEntity;

public interface ConsentRecordStore {

    ConsentRecordEntity save(ConsentRecordEntity record);

    boolean existsBySubjectAndPurpose(UUID subjectUserId, ConsentPurpose purpose);
}
