package com.simbio.core.files.application;

import java.util.Optional;
import java.util.UUID;

import com.simbio.core.files.domain.FileMetadataEntity;

public interface FileMetadataStore {

    FileMetadataEntity save(FileMetadataEntity metadata);

    Optional<FileMetadataEntity> findByPublicId(UUID publicId);
}
