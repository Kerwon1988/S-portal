package com.simbio.core.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AdrDocumentRulesTest {

    private static final Pattern TASK_REFERENCE = Pattern.compile(
            "\\b(?!(?:ADR|AES|SHA)-)(?:[A-Z]{2,5}-\\d{3}|R\\d-[A-Z]{3,})\\b");
    private static final Pattern LINK = Pattern.compile("\\[[^\\]]+\\]\\(([^)]+)\\)");
    private static final Path ROADMAP = Path.of("docs/plan/10-roadmap-backlog.md");

    @TempDir
    Path temporaryDirectory;

    @Test
    void allCurrentAdrsHaveRequiredMetadataSectionsLinksAndKnownTasks() throws IOException {
        validate(Path.of("docs/adr"), ROADMAP);
        assertThat(Path.of("docs/adr/TEMPLATE.md")).exists();
    }

    @Test
    void adrTemplateContainsRequiredFieldsAndDecisionSections() throws IOException {
        String template = Files.readString(Path.of("docs/adr/TEMPLATE.md"));
        assertThat(template).contains("- Estado:", "- Fecha:", "- Tarea(s):", "## Contexto",
                "## Decisión", "## Alternativas", "## Consecuencias");
    }

    @Test
    void rejectsAdrsWithoutDecisionMetadata() throws IOException {
        Path adr = temporaryDirectory.resolve("ADR-999-invalid.md");
        Files.writeString(adr, "# ADR-999 — Invalid\n## Contexto\n");
        Files.writeString(temporaryDirectory.resolve("10-roadmap-backlog.md"), "| FND-001 |");

        assertThatIllegalStateException()
                .isThrownBy(() -> validate(temporaryDirectory, temporaryDirectory.resolve("10-roadmap-backlog.md")))
                .withMessageContaining("Estado");
    }

    @Test
    void rejectsUnknownTaskReferencesAndBrokenRelativeLinks() throws IOException {
        Path adrDirectory = Files.createDirectories(temporaryDirectory.resolve("adr"));
        Path adr = adrDirectory.resolve("ADR-999-invalid.md");
        Files.writeString(adr, """
                # ADR-999 — Invalid
                - Estado: Propuesto
                - Fecha: 2026-11-11
                - Tarea(s): `UIK-999`
                ## Contexto
                ## Decisión
                ## Alternativas
                ## Consecuencias
                [Missing](missing.md)
                """);
        Path roadmap = temporaryDirectory.resolve("10-roadmap-backlog.md");
        Files.writeString(roadmap, "| UIK-002 |");

        assertThatIllegalStateException()
                .isThrownBy(() -> validate(adrDirectory, roadmap))
                .withMessageContaining("UIK-999");
    }

    private static void validate(Path directory, Path roadmap) throws IOException {
        Set<String> taskIds = new HashSet<>();
        Matcher roadmapMatcher = TASK_REFERENCE.matcher(Files.readString(roadmap));
        while (roadmapMatcher.find()) {
            taskIds.add(roadmapMatcher.group());
        }

        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().matches("ADR-\\d{3}-.*\\.md"))
                    .toList()) {
                String content = Files.readString(file);
                require(content.contains("- Estado:"), file, "Estado");
                require(content.contains("- Fecha:"), file, "Fecha");
                require(content.contains("## Contexto"), file, "Contexto");
                require(content.matches("(?s).*## Decisi[oó]n(?:\\s|\\n).*"), file, "Decisión");
                require(content.contains("## Alternativas"), file, "Alternativas");
                require(content.contains("## Consecuencias"), file, "Consecuencias");

                Matcher taskMatcher = TASK_REFERENCE.matcher(content);
                boolean referencesTask = false;
                while (taskMatcher.find()) {
                    referencesTask = true;
                    if (!taskIds.contains(taskMatcher.group())) {
                        throw new IllegalStateException(file.getFileName() + " references unknown task "
                                + taskMatcher.group());
                    }
                }
                require(referencesTask, file, "referencia a tarea");
                validateLinks(file, content);
            }
        }
    }

    private static void validateLinks(Path file, String content) {
        Matcher matcher = LINK.matcher(content);
        while (matcher.find()) {
            String target = matcher.group(1);
            if (target.startsWith("http://") || target.startsWith("https://") || target.startsWith("mailto:")) {
                continue;
            }
            String path = target.split("#", 2)[0];
            Path resolved = path.isBlank() ? file : file.getParent().resolve(path).normalize();
            if (!Files.exists(resolved)) {
                throw new IllegalStateException(file.getFileName() + " has broken link " + target);
            }
        }
    }

    private static void require(boolean condition, Path file, String field) {
        if (!condition) {
            throw new IllegalStateException(file.getFileName() + " is missing " + field);
        }
    }
}
