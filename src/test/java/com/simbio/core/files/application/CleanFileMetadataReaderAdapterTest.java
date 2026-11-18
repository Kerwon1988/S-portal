package com.simbio.core.files.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.simbio.core.files.api.CleanFileMetadata;
import com.simbio.core.files.api.ScanState;
import com.simbio.core.files.domain.FileMetadataEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CleanFileMetadataReaderAdapterTest {

    private final FileMetadataStore metadataStore = mock(FileMetadataStore.class);
    private final CleanFileMetadataReaderAdapter reader = new CleanFileMetadataReaderAdapter(metadataStore);

    @BeforeEach
    void setUp() {
        when(metadataStore.findByPublicId(null)).thenReturn(Optional.empty());
    }

    @Test
    void returnsOnlyMinimalMetadataForCleanFiles() {
        UUID publicId = UUID.randomUUID();
        when(metadataStore.findByPublicId(publicId)).thenReturn(Optional.of(new FileMetadataEntity(publicId,
                "storage-key", "original.png", "image/png", 2048, ScanState.CLEAN, Instant.EPOCH)));

        assertThat(reader.findClean(publicId))
                .contains(new CleanFileMetadata(publicId, "image/png", 2048));
    }

    @Test
    void hidesMetadataForFilesThatAreNotClean() {
        UUID publicId = UUID.randomUUID();
        when(metadataStore.findByPublicId(publicId)).thenReturn(Optional.of(new FileMetadataEntity(publicId,
                "storage-key", "original.png", "image/png", 2048, ScanState.QUARANTINED, Instant.EPOCH)));

        assertThat(reader.findClean(publicId)).isEmpty();
    }
}
