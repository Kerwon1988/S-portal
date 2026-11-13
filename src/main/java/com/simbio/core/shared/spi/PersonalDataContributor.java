package com.simbio.core.shared.spi;

import java.util.UUID;

public interface PersonalDataContributor {

    void contribute(UUID subjectId, PersonalDataExport export);
}
