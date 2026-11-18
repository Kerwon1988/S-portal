package com.simbio.core.content.api;

import java.util.Optional;

public interface LegalDocumentsReader {

    Optional<PublishedLegalDocument> current(String type, String locale);
}
