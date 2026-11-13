package com.simbio.core.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GovernanceArtifactTest {

    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[[^\\]]+\\]\\(([^)]+)\\)");

    @TempDir
    Path temporaryDirectory;

    @Test
    void modularRulesCoverP01ThroughP12AndDefineExpiry() throws IOException {
        validateRules(Path.of("docs/arquitectura/NORMAS-DESARROLLO-MODULAR.md"));
    }

    @Test
    void missingRuleIsReported() throws IOException {
        Path incomplete = temporaryDirectory.resolve("rules.md");
        Files.writeString(incomplete, "# Rules\n| P01 | owner |\n| P12 | expiry |\n");
        assertThatIllegalStateException()
                .isThrownBy(() -> validateRules(incomplete))
                .withMessageContaining("P02");
    }

    @Test
    void copilotInstructionsAndSkillHaveValidTriggersAndSourceLinks() throws IOException {
        String instructions = Files.readString(Path.of(".github/copilot-instructions.md"));
        assertThat(instructions)
                .contains("NORMAS-DESARROLLO-MODULAR.md", "acceptance criteria", "CSRF", "INVENTARIO.md");
        assertThat(Path.of(".github/../docs/arquitectura/NORMAS-DESARROLLO-MODULAR.md")).exists();
        validateLinks(Path.of(".github/copilot-instructions.md"), instructions);

        String skill = Files.readString(Path.of(".github/skills/sportal-modular/SKILL.md"));
        assertThat(skill).contains("---", "name: sportal-modular", "description:", "Required before editing",
                "Implementation rules", "Verification and completion");
        assertThat(skill.toLowerCase()).contains("feature", "bug fix", "refactor", "template");
    }

    @Test
    void readmeInventoryAndDiaryDocumentTheCurrentWorkspaceState() throws IOException {
        String readme = Files.readString(Path.of("README.md"));
        String inventory = Files.readString(Path.of("INVENTARIO.md"));
        String diary = Files.readString(Path.of("DIARIO.md"));
        assertThat(readme).contains("Frontend build and UI foundation", "E2E and accessibility",
                "Governance and architecture documentation");
        assertThat(inventory).contains("Actualizado:", "UI kit y gobernanza", "Estado observable");
        assertThat(diary).contains("PROMPT 03", "No se declara éxito");
        assertThat(Path.of("docs/arquitectura/rutas-ui.md")).exists();
        assertThat(Path.of("docs/arquitectura/modulos.mmd")).exists();
        validateLinks(Path.of("README.md"), readme);
        validateLinks(Path.of("INVENTARIO.md"), inventory);
    }

    private static void validateLinks(Path document, String content) {
        var matcher = MARKDOWN_LINK.matcher(content);
        while (matcher.find()) {
            String target = matcher.group(1);
            if (target.startsWith("http://") || target.startsWith("https://") || target.startsWith("mailto:")) {
                continue;
            }
            String linkedPath = target.split("#", 2)[0];
            Path resolved = linkedPath.isBlank()
                    ? document
                    : document.getParent() == null
                            ? Path.of(linkedPath)
                            : document.getParent().resolve(linkedPath).normalize();
            if (!Files.exists(resolved)) {
                throw new IllegalStateException(document + " contains broken link " + target);
            }
        }
    }

    private static void validateRules(Path file) throws IOException {
        String content = Files.readString(file);
        for (int rule = 1; rule <= 12; rule++) {
            String id = "P%02d".formatted(rule);
            if (!Pattern.compile("(?m)^\\|\\s*" + id + "\\s*\\|").matcher(content).find()) {
                throw new IllegalStateException("Modular rules are missing " + id);
            }
        }
        if (!content.contains("owner") || !content.toLowerCase().contains("caducidad")) {
            throw new IllegalStateException("P12 exceptions must include an owner and expiry");
        }
    }
}
