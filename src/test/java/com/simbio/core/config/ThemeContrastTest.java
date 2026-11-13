package com.simbio.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ThemeContrastTest {

    private static final Pattern TOKEN = Pattern.compile("--sp-([a-z-]+):\\s*(#[0-9a-fA-F]{6})");
    private static final String LIGHT = ":root {";
    private static final String DARK = ":root[data-theme=\"dark\"] {";

    @Test
    void themeTokensMeetTheDocumentedContrastThresholds() throws IOException {
        String css = Files.readString(Path.of("src/main/frontend/css/tokens.css"));
        Map<String, String> light = tokens(section(css, LIGHT));
        Map<String, String> dark = tokens(section(css, DARK));

        assertRatio(light, "text", "bg", 14.48, 0.03);
        assertRatio(light, "text-muted", "bg", 7.51, 0.03);
        assertRatio(light, "accent-text", "bg", 9.63, 0.03);
        assertRatio(light, "border", "bg", 4.71, 0.03);
        assertRatio(light, "on-primary", "primary", 8.71, 0.03);
        assertRatio(dark, "text", "bg", 14.48, 0.03);
        assertRatio(dark, "text-muted", "surface-alt", 7.27, 0.03);
        assertRatio(dark, "accent-text", "surface-alt", 8.75, 0.03);
        assertRatio(dark, "border", "surface-alt", 3.03, 0.03);
    }

    @Test
    void rejectsLimeAsTextOnLightBackgroundAndAcceptsOliveAccent() {
        assertThatIllegalStateException()
                .isThrownBy(() -> requireContrast("#bcc907", "#ffffff", 4.5))
                .withMessageContaining("4.5:1");
        requireContrast("#3e4202", "#f5f5e8", 4.5);
        requireContrast("#6f6f4c", "#f5f5e8", 3.0);
    }

    private static void assertRatio(
            Map<String, String> tokens, String foreground, String background, double expected, double tolerance) {
        double actual = contrast(tokens.get(foreground), tokens.get(background));
        assertThat(actual).isCloseTo(expected, org.assertj.core.data.Offset.offset(tolerance));
        requireContrast(tokens.get(foreground), tokens.get(background), foreground.equals("border") ? 3.0 : 4.5);
    }

    private static String section(String css, String marker) {
        int start = css.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Missing theme token section " + marker);
        }
        int end = css.indexOf('}', start);
        return css.substring(start, end);
    }

    private static Map<String, String> tokens(String section) {
        Map<String, String> tokens = new HashMap<>();
        Matcher matcher = TOKEN.matcher(section);
        while (matcher.find()) {
            tokens.put(matcher.group(1), matcher.group(2));
        }
        return tokens;
    }

    private static void requireContrast(String foreground, String background, double minimum) {
        double actual = contrast(foreground, background);
        if (actual < minimum) {
            throw new IllegalStateException("Contrast " + actual + ":1 is below " + minimum + ":1");
        }
    }

    private static double contrast(String foreground, String background) {
        double first = luminance(foreground);
        double second = luminance(background);
        return (Math.max(first, second) + 0.05) / (Math.min(first, second) + 0.05);
    }

    private static double luminance(String color) {
        int red = Integer.parseInt(color.substring(1, 3), 16);
        int green = Integer.parseInt(color.substring(3, 5), 16);
        int blue = Integer.parseInt(color.substring(5, 7), 16);
        return 0.2126 * linearize(red / 255.0)
                + 0.7152 * linearize(green / 255.0)
                + 0.0722 * linearize(blue / 255.0);
    }

    private static double linearize(double channel) {
        return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }
}
