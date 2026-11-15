package com.simbio.core.files.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.simbio.core.files.api.FileAccessAuthorization;
import com.simbio.core.files.api.FileStorage;
import com.simbio.core.files.api.StoredFile;
import com.simbio.core.files.api.FilePurpose;
import com.simbio.core.files.api.ScanState;
import com.simbio.core.files.domain.FileMetadataEntity;
import com.simbio.core.files.domain.FileVariantEntity;

class ImageVariantServiceTest {

    @Test
    void rejectsCorruptImagesBeforePublishingVariants() {
        FileStorage fileStorage = new ByteFileStorage(new byte[] {1, 2, 3});
        ImageVariantService service = new ImageVariantService(fileStorage, null, null, null);

        assertThatThrownBy(() -> service.createVariants(UUID.randomUUID(), ignored -> true))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsUnauthorizedSourceImages() {
        ImageVariantService service = new ImageVariantService(
                new ByteFileStorage(png()), null, null, null);

        assertThatThrownBy(() -> service.createVariants(UUID.randomUUID(), ignored -> false))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void createsBoundedWebpVariantsWithoutExifMetadata() {
        UUID publicId = UUID.randomUUID();
        byte[] source = png(2400, 1200);
        InMemoryMetadataStore metadata = new InMemoryMetadataStore(publicId, source.length);
        CapturingObjectStore objects = new CapturingObjectStore();
        ImageVariantService service = new ImageVariantService(
                new ByteFileStorage(source), metadata, objects, variant -> variant);

        List<ImageVariantService.Variant> variants =
                service.createVariants(publicId, requestedId -> publicId.equals(requestedId));

        assertThat(variants).extracting(ImageVariantService.Variant::width)
                .containsExactly(400, 900, 1600);
        assertThat(variants).extracting(ImageVariantService.Variant::height)
                .containsExactly(200, 450, 800);
        assertThat(objects.derived.values()).hasSize(3).allSatisfy(bytes -> {
            assertThat(new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("RIFF");
            assertThat(new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("WEBP");
            assertThat(new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("Exif");
        });
    }

    private static byte[] png() {
        return png(1, 1);
    }

    private static byte[] png(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        try (var output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record ByteFileStorage(byte[] content) implements FileStorage {
        @Override
        public StoredFile store(java.io.InputStream input, String submittedName, long declaredSize, FilePurpose purpose) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.io.InputStream open(UUID publicId, FileAccessAuthorization authorization) {
            if (!authorization.mayRead(publicId)) {
                throw new FileNotFoundException();
            }
            return new java.io.ByteArrayInputStream(content);
        }
    }

    private static final class InMemoryMetadataStore implements FileMetadataStore {
        private final FileMetadataEntity metadata;

        private InMemoryMetadataStore(UUID publicId, long size) {
            metadata = new FileMetadataEntity(publicId, "a".repeat(32), "upload.png", "image/png",
                    size, ScanState.CLEAN, Instant.now());
        }

        @Override
        public FileMetadataEntity save(FileMetadataEntity value) {
            return value;
        }

        @Override
        public Optional<FileMetadataEntity> findByPublicId(UUID publicId) {
            return metadata.getPublicId().equals(publicId) ? Optional.of(metadata) : Optional.empty();
        }
    }

    private static final class CapturingObjectStore implements FileObjectStore {
        private final Map<String, byte[]> derived = new HashMap<>();

        @Override
        public Path writeQuarantined(InputStream content, String storageKey) throws IOException {
            derived.put(storageKey, content.readAllBytes());
            return Path.of(storageKey);
        }

        @Override
        public void markClean(String storageKey) {
        }

        @Override
        public InputStream open(String storageKey) {
            return new java.io.ByteArrayInputStream(derived.get(storageKey));
        }

        @Override
        public void remove(String storageKey) {
            derived.remove(storageKey);
        }
    }
}
