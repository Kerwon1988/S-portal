package com.simbio.core.config.errortracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.sql.SQLException;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.simbio.core.shared.spi.ErrorReporter.ErrorReport;
import com.simbio.core.shared.spi.ErrorReporter.Source;

class ErrorInventoryServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void persistsDeduplicatedRecordsAndReloadsThemAfterRestart() {
        Path inventoryPath = temporaryDirectory.resolve("private").resolve("error-inventory.json");
        ErrorTrackingProperties properties = properties(inventoryPath);
        ObjectMapper mapper = JsonMapper.builder().build();
        ErrorInventoryService firstProcess = new ErrorInventoryService(properties, mapper);
        ErrorReport report = new ErrorReport(
                Source.DATABASE,
                "PERSISTENCE_FAILURE",
                "SQLException",
                new SQLException("row value ana@example.com token=secret", "08001", 10054),
                "/customers/42",
                "release-1",
                "en",
                "corr-12345678");

        firstProcess.report(report);
        firstProcess.report(report);

        assertThat(firstProcess.snapshot().errors())
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.occurrences()).isEqualTo(2);
                    assertThat(java.time.Instant.parse(record.firstSeen()))
                            .isBeforeOrEqualTo(java.time.Instant.parse(record.lastSeen()));
                    assertThat(record.sqlState()).isEqualTo("08001");
                });
        String persistedJson = readFile(inventoryPath);
        assertThat(persistedJson)
                .doesNotContain("ana@example.com")
                .doesNotContain("secret")
                .doesNotContain("row value");

        ErrorInventoryService restarted = new ErrorInventoryService(properties, mapper);

        assertThat(restarted.snapshot().errors())
                .singleElement()
                .satisfies(record -> assertThat(record.occurrences()).isEqualTo(2));
    }

    @Test
    void refusesToPlaceInventoryUnderStaticWebResources() {
        Path staticPath = Path.of("src", "main", "resources", "static", "error-inventory.json")
                .toAbsolutePath()
                .normalize();

        assertThatThrownBy(() -> new ErrorInventoryService(properties(staticPath), JsonMapper.builder().build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside the web root");
    }

    @Test
    void persistsResolvedStatusAndSanitizedNote() {
        Path inventoryPath = temporaryDirectory.resolve("private").resolve("error-inventory.json");
        ErrorInventoryService service = new ErrorInventoryService(properties(inventoryPath), JsonMapper.builder().build());
        ErrorReport report = new ErrorReport(
                Source.BACKEND,
                "MVC_EXCEPTION",
                "IllegalStateException",
                new IllegalStateException("synthetic"),
                "/gestion/errores",
                "release-1",
                "es",
                "corr-synthetic");
        service.report(report);
        String fingerprint = service.snapshot().errors().getFirst().fingerprint();

        service.updateStatus(fingerprint, "RESOLVED",
                "Fixed for ana@example.test; token=private-value; caller 12345678");

        ErrorRecord updated = service.snapshot().errors().getFirst();
        assertThat(updated.status()).isEqualTo("RESOLVED");
        assertThat(updated.resolutionNote())
                .isEqualTo("Fixed for [redacted]; token=[redacted]; caller [redacted]");
        assertThat(readFile(inventoryPath))
                .doesNotContain("ana@example.test")
                .doesNotContain("private-value")
                .doesNotContain("12345678");

        ErrorInventoryService restarted = new ErrorInventoryService(properties(inventoryPath), JsonMapper.builder().build());
        assertThat(restarted.snapshot().errors().getFirst().resolutionNote())
                .isEqualTo(updated.resolutionNote());

        restarted.updateStatus(fingerprint, "IGNORED", "Duplicate technical event");
        assertThat(restarted.snapshot().errors().getFirst().status()).isEqualTo("IGNORED");

        restarted.report(report);
        assertThat(restarted.snapshot().errors().getFirst().status()).isEqualTo("OPEN");
        assertThat(restarted.snapshot().errors().getFirst().resolutionNote()).isEmpty();
    }

    @Test
    void reportsDegradedAvailabilityWhenStoredInventoryCannotBeRead() throws Exception {
        Path inventoryPath = temporaryDirectory.resolve("private").resolve("error-inventory.json");
        java.nio.file.Files.createDirectories(inventoryPath.getParent());
        java.nio.file.Files.writeString(inventoryPath, "not-json");

        ErrorInventoryService service = new ErrorInventoryService(properties(inventoryPath), JsonMapper.builder().build());

        assertThat(service.snapshot().available()).isFalse();
        assertThat(service.snapshot().errors()).isEmpty();
        assertThatThrownBy(() -> service.updateStatus("a".repeat(64), "RESOLVED", "synthetic note"))
                .isInstanceOf(ErrorInventoryUnavailableException.class);
    }

    private static ErrorTrackingProperties properties(Path path) {
        return new ErrorTrackingProperties(path, 10, 1024 * 1024, 2048, "test-release");
    }

    private static String readFile(Path path) {
        try {
            return java.nio.file.Files.readString(path);
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
