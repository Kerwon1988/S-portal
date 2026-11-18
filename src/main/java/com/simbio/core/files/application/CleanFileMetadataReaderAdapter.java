package com.simbio.core.files.application;

import java.util.Optional;
import java.util.UUID;

import com.simbio.core.files.api.CleanFileMetadata;
import com.simbio.core.files.api.CleanFileMetadataReader;
import com.simbio.core.files.api.ScanState;
import com.simbio.core.files.domain.FileMetadataEntity;
import org.springframework.stereotype.Component;

@Component
class CleanFileMetadataReaderAdapter implements CleanFileMetadataReader {

    private final FileMetadataStore metadataStore;

    CleanFileMetadataReaderAdapter(FileMetadataStore metadataStore) {
        this.metadataStore = metadataStore;
    }

    @Override
    public Optional<CleanFileMetadata> findClean(UUID publicId) {
        if (publicId == null) {
            return Optional.empty();
        }
        return metadataStore.findByPublicId(publicId)
                .filter(metadata -> metadata.getScanState() == ScanState.CLEAN)
                .map(CleanFileMetadataReaderAdapter::toApi);
    }

    private static CleanFileMetadata toApi(FileMetadataEntity metadata) {
        return new CleanFileMetadata(metadata.getPublicId(), metadata.getDetectedMime(), metadata.getSizeBytes());
    }
}
