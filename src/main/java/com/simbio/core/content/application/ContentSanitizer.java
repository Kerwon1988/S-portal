package com.simbio.core.content.application;

import java.util.Objects;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

import com.simbio.core.content.api.SanitizedHtml;

@Component
public class ContentSanitizer {

    private static final PolicyFactory ALLOWLIST = new HtmlPolicyBuilder()
            .allowElements("p", "h2", "h3", "ul", "ol", "li", "strong", "em", "blockquote", "code", "pre", "a", "br")
            .allowAttributes("href", "title").onElements("a")
            .allowUrlProtocols("https", "mailto")
            .requireRelNofollowOnLinks()
            .toFactory();

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    public SanitizedHtml sanitize(String markdown) {
        Objects.requireNonNull(markdown, "markdown must not be null");
        if (markdown.length() > 8000) {
            throw new IllegalArgumentException("Content exceeds the maximum length");
        }
        return new SanitizedHtml(ALLOWLIST.sanitize(renderer.render(parser.parse(markdown))));
    }
}
