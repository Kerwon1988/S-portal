package com.simbio.core.content.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class ContentSanitizerTest {

    private final ContentSanitizer sanitizer = new ContentSanitizer();

    @Test
    @DisplayName("TC-CMS-003: valid Markdown retains allowlisted formatting")
    void rendersMarkdownAndRetainsOnlyAllowlistedFormatting() {
        var result = sanitizer.sanitize("## Welcome\n\n**Safe text** and [a link](https://example.test/page).");

        assertThat(result.value()).contains("<h2>", "<strong>Safe text</strong>", "href=\"https://example.test/page\"");
    }

    @Test
    @DisplayName("TC-CMS-003: active HTML and dangerous links are removed")
    void removesScriptsHandlersSvgAndDangerousUrls() {
        var result = sanitizer.sanitize(
                "<script>alert(1)</script><img src=x onerror=alert(1)>"
                        + "<a href=\"javascript:alert(1)\" onclick=\"alert(1)\">unsafe</a>"
                        + "<svg onload=\"alert(1)\"><script>alert(1)</script></svg>");

        assertThat(result.value()).doesNotContain("<script", "<img", "<svg", "onerror", "onclick",
                "onload", "javascript:");
    }
}
