package com.simbio.core.files.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.simbio.core.files.api.FileAccessAuthorization;
import com.simbio.core.files.api.FilePurpose;
import com.simbio.core.files.api.ScanState;
import com.simbio.core.files.api.StoredFile;
import com.simbio.core.files.domain.FileMetadataEntity;
import com.simbio.core.files.infrastructure.LocalFileStorage;

class FileStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesMetadataAndStreamsCleanFileAfterAuthorization() throws IOException {
        byte[] image = png();
        InMemoryMetadataStore metadata = new InMemoryMetadataStore();
        LocalFileStorage objectStore = new LocalFileStorage(temporaryDirectory.resolve("private-files"));
        FileStorageService service = service(objectStore, metadata, path -> MalwareScanner.ScanResult.CLEAN);

        StoredFile stored = service.store(new ByteArrayInputStream(image), "logo.png", image.length, FilePurpose.IMAGE);

        assertThat(stored.scanState()).isEqualTo(ScanState.CLEAN);
        assertThat(metadata.findByPublicId(stored.publicId())).isPresent();
        try (InputStream downloaded = service.open(stored.publicId(), ignored -> true)) {
            assertThat(downloaded.readAllBytes()).containsExactly(image);
        }
    }

    @Test
    void deniesUnauthorizedDownloadWithoutRevealingStorageLocation() {
        byte[] image = png();
        InMemoryMetadataStore metadata = new InMemoryMetadataStore();
        LocalFileStorage objectStore = new LocalFileStorage(temporaryDirectory.resolve("private-files"));
        FileStorageService service = service(objectStore, metadata, path -> MalwareScanner.ScanResult.CLEAN);
        StoredFile stored = service.store(new ByteArrayInputStream(image), "logo.png", image.length, FilePurpose.IMAGE);

        assertThatThrownBy(() -> service.open(stored.publicId(), ignored -> false))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessage("File not found");
    }

    @Test
    void keepsFileQuarantinedWhenScannerIsUnavailable() {
        byte[] image = png();
        InMemoryMetadataStore metadata = new InMemoryMetadataStore();
        LocalFileStorage objectStore = new LocalFileStorage(temporaryDirectory.resolve("private-files"));
        FileStorageService service = service(objectStore, metadata, path -> MalwareScanner.ScanResult.UNAVAILABLE);

        StoredFile stored = service.store(new ByteArrayInputStream(image), "logo.png", image.length, FilePurpose.IMAGE);

        assertThat(stored.scanState()).isEqualTo(ScanState.QUARANTINED);
        assertThatThrownBy(() -> service.open(stored.publicId(), ignored -> true))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void keepsFileQuarantinedWhenScannerReturnsNoResult() {
        byte[] image = png();
        InMemoryMetadataStore metadata = new InMemoryMetadataStore();
        LocalFileStorage objectStore = new LocalFileStorage(temporaryDirectory.resolve("private-files"));
        FileStorageService service = service(objectStore, metadata, path -> null);

        StoredFile stored = service.store(new ByteArrayInputStream(image), "logo.png", image.length, FilePurpose.IMAGE);

        assertThat(stored.scanState()).isEqualTo(ScanState.QUARANTINED);
        assertThatThrownBy(() -> service.open(stored.publicId(), ignored -> true))
                .isInstanceOf(FileNotFoundException.class);
    }

    private static FileStorageService service(
            FileObjectStore objectStore, FileMetadataStore metadata, MalwareScanner scanner) {
        return new FileStorageService(objectStore, metadata, new FileValidationService(), scanner);
    }

    private static byte[] png() {
        return java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/lI0AAAAASUVORK5CYII=");
    }

    private static final class InMemoryMetadataStore implements FileMetadataStore {

        private final Map<UUID, FileMetadataEntity> files = new HashMap<>();

        @Override
        public FileMetadataEntity save(FileMetadataEntity metadata) {
            files.put(metadata.getPublicId(), metadata);
            return metadata;
        }

        @Override
        public Optional<FileMetadataEntity> findByPublicId(UUID publicId) {
            return Optional.ofNullable(files.get(publicId));
        }
    }
}
