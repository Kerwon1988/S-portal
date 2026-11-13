package com.simbio.core.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NoLiteralSecretsTest {

    private static final Pattern SECRET_PROPERTY =
            Pattern.compile("(?im)^[ \\t]*(password|secret|token|api[-_]key)[ \\t]*:[ \\t]*(.*?)[ \\t]*$");

    @TempDir
    Path temporaryDirectory;

    @Test
    void applicationConfigurationContainsNoLiteralSecrets() throws IOException {
        assertThatCode(() -> validate(Path.of("src/main/resources")))
                .doesNotThrowAnyException();
    }

    @Test
    void reportsLiteralPasswordAndFile() throws IOException {
        Path fixture = temporaryDirectory.resolve("application-prod.yml");
        Files.writeString(fixture, "password: abc123");

        assertThatIllegalStateException()
                .isThrownBy(() -> validate(temporaryDirectory))
                .withMessageContaining("application-prod.yml")
                .withMessageContaining("password");
    }

    private static void validate(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path file : paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".yml") || path.toString().endsWith(".yaml"))
                    .toList()) {
                String contents = Files.readString(file);
                Matcher matcher = SECRET_PROPERTY.matcher(contents);
                while (matcher.find()) {
                    String value = matcher.group(2).trim();
                    if (!value.isEmpty() && !value.startsWith("${")) {
                        throw new IllegalStateException(
                                file.getFileName() + " contains a literal value for " + matcher.group(1));
                    }
                }
            }
        }
    }
}
