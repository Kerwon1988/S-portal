package com.simbio.core.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationConventionTest {

    private static final Pattern MIGRATION_NAME =
            Pattern.compile("V\\d{12}__([a-z][a-z0-9_]*)__([a-z0-9][a-z0-9_-]*)\\.sql");
    private static final Pattern LEGACY_DBO_SCHEMA = Pattern.compile("(?i)\\bdbo\\s*\\.");

    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsVersionedMigrationsInsideTheirOwnerSchema() throws IOException {
        assertThatCode(() -> validate(Path.of("src/main/resources/db/migration")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsLegacyMigrationNamesAndDboReferences() throws IOException {
        Path legacyName = temporaryDirectory.resolve("identity/V1__x.sql");
        Files.createDirectories(legacyName.getParent());
        Files.writeString(legacyName, "SELECT 1;");

        assertThatIllegalStateException()
                .isThrownBy(() -> validate(temporaryDirectory))
                .withMessageContaining("V1__x.sql")
                .withMessageContaining("filename pattern");

        Files.delete(legacyName);
        Path dboReference = legacyName.getParent().resolve("V202611110001__identity__users.sql");
        Files.writeString(dboReference, "SELECT * FROM dbo.users;");

        assertThatIllegalStateException()
                .isThrownBy(() -> validate(temporaryDirectory))
                .withMessageContaining("V202611110001__identity__users.sql")
                .withMessageContaining("dbo schema");
    }

    @Test
    void rejectsMigrationFolderThatDoesNotMatchItsModule() throws IOException {
        Path misplaced = temporaryDirectory.resolve("identity/V202611110001__users__create.sql");
        Files.createDirectories(misplaced.getParent());
        Files.writeString(misplaced, "SELECT 1;");

        assertThatIllegalStateException()
                .isThrownBy(() -> validate(temporaryDirectory))
                .withMessageContaining("V202611110001__users__create.sql")
                .withMessageContaining("folder must match module");
    }

    private static void validate(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path file : paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .toList()) {
                Path relative = root.relativize(file);
                String filename = file.getFileName().toString();
                if (relative.getNameCount() != 2) {
                    throw violation(filename, "migration must be directly inside its module folder");
                }

                String folder = relative.getName(0).toString();
                var matcher = MIGRATION_NAME.matcher(filename);
                if (!matcher.matches()) {
                    throw violation(filename, "filename pattern must be V<yyyyMMddHHmm>__<module>__<desc>.sql");
                }
                if (!folder.equals(matcher.group(1))) {
                    throw violation(filename, "folder must match module");
                }
                if (LEGACY_DBO_SCHEMA.matcher(Files.readString(file)).find()) {
                    throw violation(filename, "dbo schema is not allowed");
                }
            }
        }
    }

    private static IllegalStateException violation(String filename, String rule) {
        return new IllegalStateException(filename + " violates migration convention: " + rule);
    }
}
