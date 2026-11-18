package com.simbio.core.config.errortracking;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.simbio.core.shared.spi.ErrorReporter;
import com.simbio.core.shared.spi.ErrorReporter.ErrorReport;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ErrorInventoryService implements ErrorReporter {

    private static final int SCHEMA_VERSION = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorInventoryService.class);

    private final Path inventoryPath;
    private final int maxEntries;
    private final long maxFileBytes;
    private final int maxFieldLength;
    private final ObjectMapper objectMapper;
    private final Map<String, ErrorRecord> records = new LinkedHashMap<>();
    private boolean available = true;

    public ErrorInventoryService(
            ErrorTrackingProperties properties,
            ObjectMapper objectMapper) {
        this.inventoryPath = properties.inventoryPath().toAbsolutePath().normalize();
        this.maxEntries = properties.maxEntries();
        this.maxFileBytes = properties.maxFileBytes();
        this.maxFieldLength = properties.maxFieldLength();
        this.objectMapper = objectMapper;
        rejectWebRootPath(this.inventoryPath);
        loadExistingInventory();
    }

    @Override
    public synchronized void report(ErrorReport report) {
        requireAvailable();
        ErrorTrackingSanitizer.SanitizedError sanitized =
                ErrorTrackingSanitizer.sanitize(report, maxFieldLength);
        Map<String, ErrorRecord> updatedRecords = new LinkedHashMap<>(records);
        Instant now = Instant.now();
        ErrorRecord existing = updatedRecords.get(sanitized.fingerprint());
        if (existing != null) {
            updatedRecords.put(existing.fingerprint(), new ErrorRecord(
                    existing.fingerprint(),
                    existing.source(),
                    existing.category(),
                    existing.errorCode(),
                    existing.exceptionType(),
                    existing.sanitizedStack(),
                    existing.path(),
                    existing.release(),
                    existing.locale(),
                    existing.correlationId(),
                    existing.severity(),
                    "OPEN",
                    "",
                    existing.sqlState(),
                    existing.databaseErrorCode(),
                    existing.firstSeen(),
                    now.toString(),
                    existing.occurrences() + 1));
        } else {
            makeRoomIfNeeded(updatedRecords);
            updatedRecords.put(sanitized.fingerprint(), new ErrorRecord(
                    sanitized.fingerprint(),
                    sanitized.source(),
                    sanitized.category(),
                    sanitized.errorCode(),
                    sanitized.exceptionType(),
                    sanitized.sanitizedStack(),
                    sanitized.path(),
                    sanitized.release(),
                    sanitized.locale(),
                    sanitized.correlationId(),
                    sanitized.severity(),
                    "OPEN",
                    "",
                    sanitized.sqlState(),
                    sanitized.databaseErrorCode(),
                    now.toString(),
                    now.toString(),
                    1));
        }
        persistOrDegrade(updatedRecords);
        records.clear();
        records.putAll(updatedRecords);
    }

    public synchronized ErrorInventorySnapshot snapshot() {
        return new ErrorInventorySnapshot(
                available,
                maxEntries,
                available ? records.values().stream()
                        .sorted(Comparator.comparing(record -> Instant.parse(record.firstSeen())))
                        .toList() : List.of());
    }

    public synchronized void updateStatus(String fingerprint, String status, String resolutionNote) {
        requireAvailable();
        if (!List.of("RESOLVED", "IGNORED").contains(status)) {
            throw new IllegalArgumentException("Unsupported error inventory status");
        }
        ErrorRecord existing = records.get(fingerprint);
        if (existing == null) {
            throw new java.util.NoSuchElementException("Error inventory entry was not found");
        }
        String safeNote = ErrorResolutionNoteSanitizer.sanitize(resolutionNote);
        Map<String, ErrorRecord> updatedRecords = new LinkedHashMap<>(records);
        updatedRecords.put(fingerprint, new ErrorRecord(
                existing.fingerprint(),
                existing.source(),
                existing.category(),
                existing.errorCode(),
                existing.exceptionType(),
                existing.sanitizedStack(),
                existing.path(),
                existing.release(),
                existing.locale(),
                existing.correlationId(),
                existing.severity(),
                status,
                safeNote,
                existing.sqlState(),
                existing.databaseErrorCode(),
                existing.firstSeen(),
                existing.lastSeen(),
                existing.occurrences()));
        persistOrDegrade(updatedRecords);
        records.clear();
        records.putAll(updatedRecords);
    }

    private void requireAvailable() {
        if (!available) {
            throw new ErrorInventoryUnavailableException();
        }
    }

    private void persistOrDegrade(Map<String, ErrorRecord> updatedRecords) {
        try {
            persist(updatedRecords);
        } catch (ErrorInventoryPersistenceException exception) {
            available = false;
            LOGGER.error("Error inventory storage is unavailable; ADMIN viewer is degraded");
            throw exception;
        }
    }

    private void loadExistingInventory() {
        try {
            if (!Files.exists(inventoryPath)) {
                return;
            }
            long fileSize = Files.size(inventoryPath);
            if (fileSize > maxFileBytes) {
                throw new IllegalStateException("Error inventory exceeds portal.error-tracking.max-file-bytes");
            }
            ErrorInventory inventory = objectMapper.readValue(inventoryPath.toFile(), ErrorInventory.class);
            if (inventory.schemaVersion() != SCHEMA_VERSION || inventory.errors().size() > maxEntries) {
                throw new IllegalStateException("Error inventory has an unsupported schema or entry count");
            }
            for (ErrorRecord record : inventory.errors()) {
                validateLoadedRecord(record);
                records.put(record.fingerprint(), record);
            }
        } catch (IOException | JacksonException | IllegalStateException | SecurityException exception) {
            records.clear();
            available = false;
            LOGGER.error("Error inventory storage is unavailable; ADMIN viewer is degraded");
        }
    }

    private void validateLoadedRecord(ErrorRecord record) {
        if (record.fingerprint() == null
                || !record.fingerprint().matches("[0-9a-f]{64}")
                || record.source() == null
                || record.firstSeen() == null
                || record.lastSeen() == null
                || record.occurrences() < 1
                || !List.of("OPEN", "RESOLVED", "IGNORED").contains(record.status())) {
            throw new IllegalStateException("Error inventory contains an invalid entry");
        }
    }

    private void makeRoomIfNeeded(Map<String, ErrorRecord> updatedRecords) {
        if (updatedRecords.size() < maxEntries) {
            return;
        }
        String removable = updatedRecords.values().stream()
                .filter(record -> "RESOLVED".equals(record.status()) || "IGNORED".equals(record.status()))
                .min(Comparator.comparing(ErrorRecord::lastSeen))
                .map(ErrorRecord::fingerprint)
                .orElseThrow(() -> new ErrorInventoryPersistenceException(
                        "Error inventory capacity reached; resolve or ignore entries before adding more"));
        updatedRecords.remove(removable);
    }

    private void persist(Map<String, ErrorRecord> updatedRecords) {
        Path parent = inventoryPath.getParent();
        Path temporaryPath = inventoryPath.resolveSibling(inventoryPath.getFileName() + ".tmp");
        try {
            if (parent != null) {
                boolean parentExists = Files.exists(parent);
                Files.createDirectories(parent);
                if (!parentExists) {
                    setPermissions(parent, EnumSet.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE));
                }
            }
            byte[] json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(new ErrorInventory(SCHEMA_VERSION, new ArrayList<>(updatedRecords.values())));
            if (json.length > maxFileBytes) {
                throw new ErrorInventoryPersistenceException(
                        "Error inventory would exceed portal.error-tracking.max-file-bytes", null);
            }
            Files.write(temporaryPath, json);
            setPermissions(temporaryPath, EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
            Files.move(temporaryPath, inventoryPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | JacksonException exception) {
            cleanupTemporaryFile(temporaryPath, exception);
            throw new ErrorInventoryPersistenceException("Could not persist the error inventory", exception);
        } catch (ErrorInventoryPersistenceException exception) {
            cleanupTemporaryFile(temporaryPath, exception);
            throw exception;
        }
    }

    private static void rejectWebRootPath(Path path) {
        Path staticRoot = Path.of("src", "main", "resources", "static").toAbsolutePath().normalize();
        Path publicRoot = Path.of("src", "main", "resources", "public").toAbsolutePath().normalize();
        boolean webRootComponent = false;
        for (Path component : path) {
            String name = component.toString();
            if ("static".equalsIgnoreCase(name) || "public".equalsIgnoreCase(name)) {
                webRootComponent = true;
                break;
            }
        }
        if (webRootComponent || path.startsWith(staticRoot) || path.startsWith(publicRoot)) {
            throw new IllegalArgumentException("Error inventory path must be outside the web root");
        }
    }

    private static void setPermissions(Path path, java.util.Set<PosixFilePermission> permissions) throws IOException {
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException ignored) {
            // Windows ACLs are inherited from the private storage directory.
        }
    }

    private static void cleanupTemporaryFile(Path temporaryPath, Exception failure) {
        try {
            Files.deleteIfExists(temporaryPath);
        } catch (IOException cleanupException) {
            failure.addSuppressed(cleanupException);
        }
    }
}
