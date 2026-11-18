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
import com.simbio.core.files.api.ImageVariant;
import com.simbio.core.files.api.FileStorage;
import com.simbio.core.files.api.StoredFile;
import com.simbio.core.files.api.FilePurpose;
import com.simbio.core.files.api.ScanState;
import com.simbio.core.files.domain.FileMetadataEntity;
import com.simbio.core.files.domain.FileVariantEntity;

class ImageVariantServiceTest {

    @Test
    void rejectsCorruptImagesBeforePublishingVariants() {
        UUID publicId = UUID.randomUUID();
        byte[] source = new byte[] {1, 2, 3};
        ImageVariantService service = new ImageVariantService(
                new ByteFileStorage(source), new InMemoryMetadataStore(publicId, source.length),
                new CapturingObjectStore(), new InMemoryVariantStore());

        assertThatThrownBy(() -> service.createVariants(publicId, ignored -> true))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsUnauthorizedSourceImages() {
        UUID publicId = UUID.randomUUID();
        byte[] source = png();
        ImageVariantService service = new ImageVariantService(
                new ByteFileStorage(source), new InMemoryMetadataStore(publicId, source.length),
                new CapturingObjectStore(), new InMemoryVariantStore());

        assertThatThrownBy(() -> service.createVariants(publicId, ignored -> false))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void rejectsImagesExceedingDimensionLimitsBeforeGeneratingVariants() {
        byte[] source = png(10_001, 1);
        UUID publicId = UUID.randomUUID();
        ImageVariantService service = new ImageVariantService(
                new ByteFileStorage(source), new InMemoryMetadataStore(publicId, source.length),
                new CapturingObjectStore(), new InMemoryVariantStore());

        assertThatThrownBy(() -> service.createVariants(publicId, ignored -> true))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void createsBoundedWebpVariantsWithoutExifMetadata() {
        UUID publicId = UUID.randomUUID();
        byte[] source = png(2400, 1200);
        InMemoryMetadataStore metadata = new InMemoryMetadataStore(publicId, source.length);
        CapturingObjectStore objects = new CapturingObjectStore();
        ImageVariantService service = new ImageVariantService(
                new ByteFileStorage(source), metadata, objects, new InMemoryVariantStore());

        List<ImageVariant> variants =
                service.createVariants(publicId, requestedId -> publicId.equals(requestedId));

        assertThat(variants).extracting(ImageVariant::width)
                .containsExactly(400, 900, 1600);
        assertThat(variants).extracting(ImageVariant::height)
                .containsExactly(200, 450, 800);
        assertThat(objects.derived.values()).hasSize(3).allSatisfy(bytes -> {
            assertThat(new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("RIFF");
            assertThat(new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("WEBP");
            assertThat(new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("Exif");
        });
    }

    @Test
    void variantCreationIsIdempotentAndOpeningRequiresAuthorization() throws Exception {
        UUID publicId = UUID.randomUUID();
        byte[] source = png(2400, 1200);
        InMemoryMetadataStore metadata = new InMemoryMetadataStore(publicId, source.length);
        CapturingObjectStore objects = new CapturingObjectStore();
        InMemoryVariantStore variants = new InMemoryVariantStore();
        ImageVariantService service = new ImageVariantService(
                new ByteFileStorage(source), metadata, objects, variants);
        FileAccessAuthorization allowed = requestedId -> publicId.equals(requestedId);

        List<ImageVariant> created = service.createVariants(publicId, allowed);
        List<ImageVariant> repeated = service.createVariants(publicId, allowed);

        assertThat(repeated).containsExactlyElementsOf(created);
        assertThat(objects.derived).hasSize(3);
        try (InputStream content = service.openVariant(publicId, created.get(0).code(), allowed)) {
            assertThat(content.readAllBytes()).isNotEmpty();
        }
        assertThatThrownBy(() -> service.openVariant(publicId, created.get(0).code(), ignored -> false))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void stripsExifAndGpsMetadataWhenReencodingJpeg() {
        byte[] source = jpegWithExifGps();
        UUID publicId = UUID.randomUUID();
        InMemoryMetadataStore metadata = new InMemoryMetadataStore(publicId, source.length, "image/jpeg");
        CapturingObjectStore objects = new CapturingObjectStore();
        ImageVariantService service = new ImageVariantService(
                new ByteFileStorage(source), metadata, objects, new InMemoryVariantStore());

        assertThat(new String(source, java.nio.charset.StandardCharsets.ISO_8859_1)).contains("Exif");
        service.createVariants(publicId, ignored -> true);

        assertThat(objects.derived.values()).isNotEmpty().allSatisfy(bytes -> {
            assertThat(new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1))
                    .doesNotContain("Exif")
                    .doesNotContain("GPSInfo");
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

    private static byte[] jpegWithExifGps() {
        BufferedImage image = new BufferedImage(32, 16, BufferedImage.TYPE_INT_RGB);
        try (var jpegOutput = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", jpegOutput);
            byte[] jpeg = jpegOutput.toByteArray();
            byte[] exifTiff = {
                    'I', 'I', 42, 0, 8, 0, 0, 0,
                    1, 0, 0x25, (byte) 0x88, 4, 0, 1, 0, 0, 0, 26, 0, 0, 0,
                    0, 0, 0, 0,
                    2, 0,
                    1, 0, 2, 0, 2, 0, 0, 0, 'N', 0, 0, 0,
                    2, 0, 5, 0, 3, 0, 0, 0, 56, 0, 0, 0,
                    0, 0, 0, 0,
                    1, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0
            };
            byte[] exifHeader = "Exif\u0000\u0000".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            byte[] metadata = new byte[exifHeader.length + exifTiff.length];
            System.arraycopy(exifHeader, 0, metadata, 0, exifHeader.length);
            System.arraycopy(exifTiff, 0, metadata, exifHeader.length, exifTiff.length);
            int segmentLength = metadata.length + 2;
            try (var output = new ByteArrayOutputStream()) {
                output.write(jpeg, 0, 2);
                output.write(0xFF);
                output.write(0xE1);
                output.write((segmentLength >>> 8) & 0xFF);
                output.write(segmentLength & 0xFF);
                output.write(metadata);
                output.write(jpeg, 2, jpeg.length - 2);
                return output.toByteArray();
            }
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
            this(publicId, size, "image/png");
        }

        private InMemoryMetadataStore(UUID publicId, long size, String mime) {
            String extension = mime.substring(mime.indexOf('/') + 1);
            metadata = new FileMetadataEntity(publicId, "a".repeat(32), "upload." + extension,
                    mime, size, ScanState.CLEAN, Instant.now());
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

    private static final class InMemoryVariantStore implements FileVariantStore {
        private final Map<String, FileVariantEntity> variants = new HashMap<>();

        @Override
        public FileVariantEntity save(FileVariantEntity variant) {
            variants.put(variant.getVariantCode(), variant);
            return variant;
        }

        @Override
        public List<FileVariantEntity> findByFileId(Long fileId) {
            return variants.values().stream()
                    .filter(variant -> java.util.Objects.equals(variant.getFileId(), fileId))
                    .sorted(java.util.Comparator.comparingInt(FileVariantEntity::getWidth)).toList();
        }
    }
}
