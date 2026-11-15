package com.simbio.core.files.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.simbio.core.files.application.FileObjectStore;

@Component
public final class LocalFileStorage implements FileObjectStore {

    private static final Pattern STORAGE_KEY = Pattern.compile("[a-f0-9]{32}");

    private final Path quarantineDirectory;
    private final Path cleanDirectory;

    public LocalFileStorage(
            @Value("${portal.files.storage-root:${java.io.tmpdir}/s-portal-files}") Path storageRoot) {
        Path configuredRoot = Objects.requireNonNull(storageRoot).toAbsolutePath().normalize();
        rejectWebRoot(configuredRoot);
        try {
            Files.createDirectories(configuredRoot);
            Path root = configuredRoot.toRealPath();
            rejectWebRoot(root);
            this.quarantineDirectory = root.resolve("quarantine").normalize();
            this.cleanDirectory = root.resolve("clean").normalize();
            if (!quarantineDirectory.startsWith(root) || !cleanDirectory.startsWith(root)) {
                throw new IllegalArgumentException("File storage directory is invalid");
            }
            rejectSymbolicLink(quarantineDirectory);
            rejectSymbolicLink(cleanDirectory);
            Files.createDirectories(quarantineDirectory);
            Files.createDirectories(cleanDirectory);
            rejectSymbolicLink(root);
            rejectSymbolicLink(quarantineDirectory);
            rejectSymbolicLink(cleanDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("File storage directory is unavailable", exception);
        }
    }

    @Override
    public Path writeQuarantined(InputStream content, String storageKey) throws IOException {
        Objects.requireNonNull(content, "content must not be null");
        Path target = resolve(storageKey);
        Path temporary = Files.createTempFile(quarantineDirectory, ".upload-", ".tmp");
        try {
            Files.copy(content, temporary, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            }
            return target;
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Override
    public void markClean(String storageKey) throws IOException {
        Path source = resolve(quarantineDirectory, storageKey);
        Path target = resolve(cleanDirectory, storageKey);
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        Path file = resolve(cleanDirectory, storageKey);
        if (Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Stored file is unavailable");
        }
        return Files.newInputStream(file);
    }

    @Override
    public void remove(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(quarantineDirectory, storageKey));
        Files.deleteIfExists(resolve(cleanDirectory, storageKey));
    }

    private static Path resolve(Path directory, String storageKey) {
        if (storageKey == null || !STORAGE_KEY.matcher(storageKey).matches()) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        Path file = directory.resolve(storageKey).normalize();
        if (!file.startsWith(directory)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return file;
    }

    private static void rejectWebRoot(Path storageRoot) {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path sourceWebRoot = projectRoot.resolve("src/main/resources/static").normalize();
        Path buildWebRoot = projectRoot.resolve("target/classes/static").normalize();
        if (storageRoot.startsWith(sourceWebRoot) || storageRoot.startsWith(buildWebRoot)) {
            throw new IllegalArgumentException("File storage directory must be outside the web root");
        }
    }

    private static void rejectSymbolicLink(Path path) throws IOException {
        if (Files.isSymbolicLink(path)) {
            throw new IllegalArgumentException("File storage directory must not be a symbolic link");
        }
    }
}
