package com.simbio.core.files.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.files.domain.FileMetadataEntity;

interface FileMetadataRepository extends JpaRepository<FileMetadataEntity, Long> {

    Optional<FileMetadataEntity> findByPublicId(UUID publicId);
}
