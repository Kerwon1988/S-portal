package com.simbio.core.files.application;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simbio.core.files.api.FileAccessAuthorization;
import com.simbio.core.files.api.FilePurpose;
import com.simbio.core.files.api.FileStorage;
import com.simbio.core.files.api.ScanState;
import com.simbio.core.files.api.StoredFile;
import com.simbio.core.files.domain.FileMetadataEntity;

@Service
public class FileStorageService implements FileStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageService.class);
    private static final long MAX_BYTES = 25L * 1024 * 1024;

    private final FileObjectStore objectStore;
    private final FileMetadataStore metadataStore;
    private final FileValidationService validation;
    private final MalwareScanner scanner;
    private final Clock clock;

    @Autowired
    public FileStorageService(FileObjectStore objectStore, FileMetadataStore metadataStore,
            FileValidationService validation, MalwareScanner scanner) {
        this(objectStore, metadataStore, validation, scanner, Clock.systemUTC());
    }

    FileStorageService(FileObjectStore objectStore, FileMetadataStore metadataStore,
            FileValidationService validation, MalwareScanner scanner, Clock clock) {
        this.objectStore = Objects.requireNonNull(objectStore);
        this.metadataStore = Objects.requireNonNull(metadataStore);
        this.validation = Objects.requireNonNull(validation);
        this.scanner = Objects.requireNonNull(scanner);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional
    public StoredFile store(InputStream content, String submittedName, long declaredSize, FilePurpose purpose) {
        byte[] bytes = readBounded(content, declaredSize);
        FileValidationService.ValidatedFile valid = validation.validate(bytes, submittedName, declaredSize, purpose);
        UUID publicId = UUID.randomUUID();
        String storageKey = publicId.toString().replace("-", "");
        var metadata = metadataStore.save(new FileMetadataEntity(publicId, storageKey, valid.safeName(),
                valid.mime(), bytes.length, ScanState.QUARANTINED, Instant.now(clock)));
        try {
            var path = objectStore.writeQuarantined(new ByteArrayInputStream(bytes), storageKey);
            MalwareScanner.ScanResult scan = scanner.scan(path);
            if (scan == null) {
                LOGGER.warn("Malware scanner returned no result; the file remains quarantined");
                scan = MalwareScanner.ScanResult.UNAVAILABLE;
            }
            Metrics.counter("portal.files.scan", "result", scan.name().toLowerCase(Locale.ROOT)).increment();
            if (scan == MalwareScanner.ScanResult.CLEAN) {
                objectStore.markClean(storageKey);
                metadata.setScanState(ScanState.CLEAN);
            } else if (scan == MalwareScanner.ScanResult.INFECTED) {
                metadata.setScanState(ScanState.REJECTED);
            }
            metadataStore.save(metadata);
            return new StoredFile(publicId, valid.mime(), bytes.length, metadata.getScanState());
        } catch (IOException exception) {
            try {
                objectStore.remove(storageKey);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw new FileStorageException(exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream open(UUID publicId, FileAccessAuthorization authorization) {
        Objects.requireNonNull(publicId);
        Objects.requireNonNull(authorization);
        if (!authorization.mayRead(publicId)) {
            throw new FileNotFoundException();
        }
        FileMetadataEntity metadata = metadataStore.findByPublicId(publicId)
                .filter(file -> file.getScanState() == ScanState.CLEAN)
                .orElseThrow(FileNotFoundException::new);
        try {
            return objectStore.open(metadata.getStorageKey());
        } catch (IOException exception) {
            throw new FileStorageException(exception);
        }
    }

    private static byte[] readBounded(InputStream input, long declaredSize) {
        if (input == null || declaredSize < 1 || declaredSize > MAX_BYTES) {
            throw new InvalidFileException();
        }
        try (input; var output = new ByteArrayOutputStream((int) Math.min(declaredSize, 64 * 1024))) {
            byte[] chunk = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(chunk)) != -1) {
                total += read;
                if (total > declaredSize || total > MAX_BYTES) {
                    throw new InvalidFileException();
                }
                output.write(chunk, 0, read);
            }
            if (total != declaredSize) {
                throw new InvalidFileException();
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new FileStorageException(exception);
        }
    }
}
