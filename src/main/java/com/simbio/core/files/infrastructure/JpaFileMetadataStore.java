package com.simbio.core.files.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.simbio.core.files.application.FileMetadataStore;
import com.simbio.core.files.domain.FileMetadataEntity;

@Repository
public class JpaFileMetadataStore implements FileMetadataStore {

    private final FileMetadataRepository repository;

    public JpaFileMetadataStore(FileMetadataRepository repository) {
        this.repository = repository;
    }

    @Override
    public FileMetadataEntity save(FileMetadataEntity metadata) {
        return repository.save(metadata);
    }

    @Override
    public Optional<FileMetadataEntity> findByPublicId(UUID publicId) {
        return repository.findByPublicId(publicId);
    }
}
