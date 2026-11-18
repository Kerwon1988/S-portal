package com.simbio.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TemplateRulesTest {

    private static final Pattern DIRECT_COLOR = Pattern.compile(
            "(?i)(?:\\b(?:color|background(?:-color)?|border-color)\\s*:\\s*|"
                    + "\\b(?:text|bg|border|ring|fill|stroke)-\\[)\\s*#[0-9a-f]{3,8}\\b");
    private static final Pattern UTEXT = Pattern.compile("\\bth:utext\\s*=");
    private static final Pattern SCRIPT_TAG = Pattern.compile("(?is)<script\\b[^>]*>");
    private static final Pattern SCRIPT_NONCE = Pattern.compile(
            "(?i)(?:th:nonce\\s*=\\s*[\"']\\$\\{cspNonce}[\"']|"
                    + "th:attr\\s*=\\s*[\"']nonce=\\$\\{cspNonce}[\"'])");

    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsTemplatesWhenNoForbiddenPatternsExist() throws IOException {
        Path templates = Files.createDirectories(temporaryDirectory.resolve("templates"));
        Path allowlist = temporaryDirectory.resolve("utext-allowlist.txt");
        Files.writeString(allowlist, "# empty\n");
        Files.writeString(templates.resolve("index.html"), "<p class=\"text-primary\">OK</p>");

        assertThat(validate(templates, allowlist)).containsExactly("index.html");
    }

    @Test
    void rejectsDirectColors() throws IOException {
        Path templates = Files.createDirectories(temporaryDirectory.resolve("templates"));
        Path allowlist = temporaryDirectory.resolve("utext-allowlist.txt");
        Files.writeString(allowlist, "# empty\n");
        Path template = templates.resolve("unsafe.html");
        Files.writeString(template, "<div style=\"color: #ff0000\"></div>");

        assertThatIllegalStateException()
                .isThrownBy(() -> validate(templates, allowlist))
                .withMessageContaining("unsafe.html")
                .withMessageContaining("direct color");
    }

    @Test
    void rejectsUnapprovedUtext() throws IOException {
        Path templates = Files.createDirectories(temporaryDirectory.resolve("templates"));
        Path allowlist = temporaryDirectory.resolve("utext-allowlist.txt");
        Files.writeString(allowlist, "# empty\n");
        Path template = templates.resolve("unsafe.html");
        Files.writeString(template, "<div th:utext=\"${content}\"></div>");

        assertThatIllegalStateException()
                .isThrownBy(() -> validate(templates, allowlist))
                .withMessageContaining("unsafe.html")
                .withMessageContaining("th:utext");
    }

    @Test
    void rejectsScriptsWithoutCspNonce() throws IOException {
        Path templates = Files.createDirectories(temporaryDirectory.resolve("templates"));
        Path allowlist = temporaryDirectory.resolve("utext-allowlist.txt");
        Files.writeString(allowlist, "# empty\n");
        Path template = templates.resolve("script.html");
        Files.writeString(template, "<script src=\"/assets/app.js\"></script>");

        assertThatIllegalStateException()
                .isThrownBy(() -> validate(templates, allowlist))
                .withMessageContaining("script.html")
                .withMessageContaining("CSP nonce");
    }

    @Test
    void sharedFragmentsAndHtmxConfigExposeAccessibleAndSecureContracts() throws IOException {
        String form = Files.readString(Path.of("src/main/resources/templates/fragments/form.html"));
        assertThat(form).contains("th:for=", "th:id=", "aria-describedby=", "aria-invalid=", "role=\"alert\"");

        String table = Files.readString(Path.of("src/main/resources/templates/fragments/table.html"));
        assertThat(table).contains("<caption", "class=\"sr-only\"", "scope=\"col\"", "table.empty");

        String pagination = Files.readString(Path.of("src/main/resources/templates/fragments/pagination.html"));
        assertThat(pagination).contains("aria-label=", "th:if=\"${previousHref != null}\"",
                "th:if=\"${nextHref != null}\"");

        String toast = Files.readString(Path.of("src/main/resources/templates/fragments/toast.html"));
        assertThat(toast).contains("role=\"status\"", "aria-live=\"polite\"", "data-dismiss-toast", "aria-label=");

        String htmxConfig = Files.readString(Path.of("src/main/frontend/js/htmx-config.js"));
        assertThat(htmxConfig).contains("htmx:configRequest", "meta[name=\"_csrf\"]",
                "meta[name=\"_csrf_header\"]", "htmx:beforeSwap", "htmx:afterSwap", "htmx:responseError");
    }

    @Test
    void productionTemplatesUseOnlyExplicitlyApprovedSanitizedHtml() throws IOException {
        Set<String> validated = validate(
                Path.of("src/main/resources/templates"),
                Path.of("src/test/resources/utext-allowlist.txt"));

        assertThat(validated).contains("content/page.html", "content/legal.html");
        String baseLayout = Files.readString(Path.of("src/main/resources/templates/layouts/base.html"));
        assertThat(baseLayout).contains("th:utext=\"${organizationJsonLd}\"");
        assertThat(baseLayout.split("th:utext", -1)).hasSize(2);
    }

    static Set<String> validate(Path templates, Path allowlist) throws IOException {
        Set<String> allowedUtext = Files.readAllLines(allowlist).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> checkedFiles = new java.util.HashSet<>();
        if (!Files.exists(templates)) {
            return checkedFiles;
        }

        try (Stream<Path> paths = Files.walk(templates)) {
            for (Path file : paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".html"))
                    .toList()) {
                String relative = templates.relativize(file).toString().replace('\\', '/');
                String contents = Files.readString(file);
                checkedFiles.add(relative);
                if (DIRECT_COLOR.matcher(contents).find()) {
                    throw violation(relative, "direct color");
                }
                if (UTEXT.matcher(contents).find() && !allowedUtext.contains(relative)) {
                    throw violation(relative, "th:utext is not allowlisted");
                }

                Matcher scriptTags = SCRIPT_TAG.matcher(contents);
                while (scriptTags.find()) {
                    if (!SCRIPT_NONCE.matcher(scriptTags.group()).find()) {
                        throw violation(relative, "script is missing its CSP nonce");
                    }
                }
            }
        }

        return Set.copyOf(checkedFiles);
    }

    private static IllegalStateException violation(String file, String rule) {
        return new IllegalStateException(file + " violates template rule: " + rule);
    }
}
