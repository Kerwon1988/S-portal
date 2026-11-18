package com.simbio.core.files.api;

import java.util.Optional;
import java.util.UUID;

public interface CleanFileMetadataReader {

    Optional<CleanFileMetadata> findClean(UUID publicId);
}
