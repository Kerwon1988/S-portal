package com.simbio.core.config;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class I18nKeyParityTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsMatchingSpanishAndEnglishBundles() throws IOException {
        Path bundleDirectory = Files.createDirectories(temporaryDirectory.resolve("content"));
        Files.writeString(bundleDirectory.resolve("messages_es.properties"), "home.title=Inicio\n");
        Files.writeString(bundleDirectory.resolve("messages_en.properties"), "home.title=Home\n");

        validate(temporaryDirectory);
    }

    @Test
    void rejectsMissingTranslationKeysWithModuleAndKey() throws IOException {
        Path bundleDirectory = Files.createDirectories(temporaryDirectory.resolve("content"));
        Files.writeString(bundleDirectory.resolve("messages_es.properties"), "home.title=Inicio\n");
        Files.writeString(bundleDirectory.resolve("messages_en.properties"), "home.subtitle=Welcome\n");

        assertThatIllegalStateException()
                .isThrownBy(() -> validate(temporaryDirectory))
                .withMessageContaining("content")
                .withMessageContaining("home.title")
                .withMessageContaining("home.subtitle");
    }

    @Test
    void validatesProductionBundlesWhenPresent() throws IOException {
        validate(Path.of("src/main/resources/i18n"));
    }

    static void validate(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }

        Map<Path, Map<String, Path>> bundleFiles = new HashMap<>();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path file : paths.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                if (name.equals("messages_es.properties") || name.equals("messages_en.properties")) {
                    Path module = root.relativize(file).getParent();
                    bundleFiles.computeIfAbsent(module == null ? Path.of(".") : module, ignored -> new HashMap<>())
                            .put(name.substring("messages_".length(), name.length() - ".properties".length()), file);
                }
            }
        }

        for (Map.Entry<Path, Map<String, Path>> entry : bundleFiles.entrySet()) {
            Map<String, Path> locales = entry.getValue();
            if (!locales.containsKey("es") || !locales.containsKey("en")) {
                throw new IllegalStateException("i18n/" + entry.getKey()
                        + " must provide both messages_es.properties and messages_en.properties");
            }

            Set<String> spanishKeys = loadKeys(locales.get("es"));
            Set<String> englishKeys = loadKeys(locales.get("en"));
            if (!spanishKeys.equals(englishKeys)) {
                Set<String> missingInEnglish = new java.util.TreeSet<>(spanishKeys);
                missingInEnglish.removeAll(englishKeys);
                Set<String> missingInSpanish = new java.util.TreeSet<>(englishKeys);
                missingInSpanish.removeAll(spanishKeys);
                throw new IllegalStateException("i18n/" + entry.getKey()
                        + " key mismatch; missing in EN: " + missingInEnglish
                        + "; missing in ES: " + missingInSpanish);
            }
        }
    }

    private static Set<String> loadKeys(Path bundle) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(bundle, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties.stringPropertyNames();
    }
}
