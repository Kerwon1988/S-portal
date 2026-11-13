package com.simbio.core.config.errortracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.simbio.core.shared.spi.ErrorReporter.ErrorReport;
import com.simbio.core.shared.spi.ErrorReporter.Source;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

class ErrorTrackingH2IT {

    private static final Path STORAGE_DIRECTORY = Path.of(
                    System.getProperty("java.io.tmpdir"),
                    "sportal-error-tracking-" + UUID.randomUUID())
            .toAbsolutePath()
            .normalize();
    private static final Path INVENTORY_PATH = STORAGE_DIRECTORY.resolve("error-inventory.json");

    @Test
    void recordsH2ConstraintFailureWithoutPersistingItsRowValue() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(
                "jdbc:h2:mem:error_tracking_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        ErrorInventoryService errorInventoryService = new ErrorInventoryService(
                new ErrorTrackingProperties(INVENTORY_PATH, 10, 1024 * 1024, 2048, "test-release"),
                JsonMapper.builder().build());
        jdbcTemplate.execute("CREATE TABLE ERROR_TRACKING_TEST (id INT PRIMARY KEY, email VARCHAR(100) UNIQUE)");
        jdbcTemplate.update(
                "INSERT INTO ERROR_TRACKING_TEST (id, email) VALUES (?, ?)", 1, "private@example.com");
        Throwable databaseFailure = catchThrowable(() -> jdbcTemplate.update(
                "INSERT INTO ERROR_TRACKING_TEST (id, email) VALUES (?, ?)", 2, "private@example.com"));

        assertThat(databaseFailure).isNotNull();
        errorInventoryService.report(new ErrorReport(
                Source.DATABASE,
                "PERSISTENCE_FAILURE",
                "H2_CONSTRAINT",
                databaseFailure,
                "/{unmatched}",
                "",
                "en",
                ""));

        assertThat(errorInventoryService.snapshot().errors())
                .singleElement()
                .satisfies(record -> assertThat(record.sqlState()).isNotBlank());
        try {
            assertThat(Files.readString(INVENTORY_PATH)).doesNotContain("private@example.com");
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    @AfterAll
    static void removeTestInventory() throws IOException {
        Files.deleteIfExists(INVENTORY_PATH);
        Files.deleteIfExists(STORAGE_DIRECTORY);
    }
}
