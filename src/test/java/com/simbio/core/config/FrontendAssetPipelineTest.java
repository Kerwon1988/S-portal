package com.simbio.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class FrontendAssetPipelineTest {

    private static final Pattern FINGERPRINTED_ASSET =
            Pattern.compile(".+\\.[0-9a-f]{16}\\.(?:css|js|png)");
    private static final Pattern EXTERNAL_ASSET =
            Pattern.compile("(?i)(?:src|href)\\s*=\\s*[\"']https?://");
    private static final Path TEMPLATE_ROOT = Path.of("src/main/resources/templates");
    private static final Path ASSET_ROOT = Path.of("target/classes/static/assets");

    @Test
    void packageManifestPinsLocalLibrariesAndHasALockfile() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> packageData = mapper.readValue(
                Path.of("package.json").toFile(), new TypeReference<Map<String, Object>>() {});
        Map<?, ?> dependencies = (Map<?, ?>) packageData.get("dependencies");
        Map<?, ?> developmentDependencies = (Map<?, ?>) packageData.get("devDependencies");
        Map<String, Object> lockData = mapper.readValue(
                Path.of("package-lock.json").toFile(), new TypeReference<Map<String, Object>>() {});
        Map<?, ?> lockedPackages = (Map<?, ?>) lockData.get("packages");
        Map<?, ?> lockedRoot = (Map<?, ?>) lockedPackages.get("");
        Map<?, ?> lockedDependencies = (Map<?, ?>) lockedRoot.get("dependencies");
        Map<?, ?> lockedDevelopmentDependencies = (Map<?, ?>) lockedRoot.get("devDependencies");

        assertThat(dependencies.get("@alpinejs/csp")).isEqualTo("3.17.5");
        assertThat(dependencies.get("htmx.org")).isEqualTo("2.0.11");
        assertThat(developmentDependencies.get("@axe-core/playwright")).isEqualTo("4.10.2");
        assertThat(developmentDependencies.get("@playwright/test")).isEqualTo("1.62.1");
        assertThat(developmentDependencies.get("postcss-import")).isEqualTo("15.1.0");
        assertThat(developmentDependencies.get("tailwindcss")).isEqualTo("3.4.19");
        assertThat(developmentDependencies.containsKey("@tailwindcss/cli")).isFalse();
        assertThat(lockedDependencies.get("@alpinejs/csp")).isEqualTo("3.17.5");
        assertThat(lockedDependencies.get("htmx.org")).isEqualTo("2.0.11");
        assertThat(lockedDevelopmentDependencies.get("@axe-core/playwright")).isEqualTo("4.10.2");
        assertThat(lockedDevelopmentDependencies.get("@playwright/test")).isEqualTo("1.62.1");
        assertThat(lockedDevelopmentDependencies.get("postcss-import")).isEqualTo("15.1.0");
        assertThat(lockedDevelopmentDependencies.get("tailwindcss")).isEqualTo("3.4.19");
        assertThat(Files.exists(Path.of("package-lock.json"))).isTrue();
    }

    @Test
    void typographyUsesSystemFontsWithoutBundledWebFonts() throws IOException {
        String tailwind = Files.readString(Path.of("tailwind.config.cjs"));
        String tokens = Files.readString(Path.of("src/main/frontend/css/tokens.css"));

        assertThat(tailwind)
                .contains(
                        "brand: [\"system-ui\", \"sans-serif\"]",
                        "heading: [\"system-ui\", \"sans-serif\"]",
                        "body: [\"system-ui\", \"sans-serif\"]")
                .doesNotContain("Nasalization", "League Gothic", "Open Sans", "@font-face");
        assertThat(tokens).contains("--font-body: system-ui, sans-serif;");
    }

    @Test
    void productionAssetsAreFingerprintNamedAndMatchTheManifest() throws IOException {
        Path manifest = ASSET_ROOT.resolve("asset-manifest.json");
        assertThat(manifest).exists();
        Map<String, String> assets = new ObjectMapper().readValue(
                manifest.toFile(), new TypeReference<Map<String, String>>() {});
        assertThat(assets.keySet()).contains(
                "app.css", "htmx.js", "alpine-csp.js", "theme.js", "menu.js", "htmx-config.js",
                "error-reporter.js", "simbio-logo.png");

        for (String asset : assets.values()) {
            String fileName = asset.substring(asset.lastIndexOf('/') + 1);
            assertThat(fileName).matches(FINGERPRINTED_ASSET);
            assertThat(ASSET_ROOT.resolve(fileName)).exists();
        }
    }

    @Test
    void homeUsesFingerprintLogoAsHighPriorityImageWithReservedDimensions() throws IOException {
        String template = Files.readString(TEMPLATE_ROOT.resolve("content/page.html"));

        assertThat(template).contains("assets['simbio-logo.png']", "width=\"2869\"", "height=\"929\"",
                "content.home.logo.alt", "fetchpriority=\"high\"");
    }

    @Test
    void templatesDoNotLoadStylesOrScriptsFromExternalOrigins() throws IOException {
        try (Stream<Path> templates = Files.walk(TEMPLATE_ROOT)) {
            for (Path template : templates.filter(Files::isRegularFile).toList()) {
                String contents = Files.readString(template);
                if (EXTERNAL_ASSET.matcher(contents).find()) {
                    throw new IllegalStateException("External asset reference in " + TEMPLATE_ROOT.relativize(template));
                }
            }
        }
    }

    @Test
    void externalAssetFixtureIsRejected() {
        assertThatIllegalStateException()
                .isThrownBy(() -> requireLocalAssets("<script src=\"https://cdn.example/app.js\"></script>"))
                .withMessageContaining("External asset");
    }

    private static void requireLocalAssets(String template) {
        if (EXTERNAL_ASSET.matcher(template).find()) {
            throw new IllegalStateException("External asset reference");
        }
    }
}
