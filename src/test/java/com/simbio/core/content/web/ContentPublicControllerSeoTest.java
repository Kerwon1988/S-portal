package com.simbio.core.content.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import tools.jackson.databind.ObjectMapper;
import com.simbio.core.content.application.ContentBlockView;
import com.simbio.core.content.application.ContentService;
import com.simbio.core.content.application.PublishedPageView;
import com.simbio.core.content.api.TeamMembersReader;
import com.simbio.core.content.domain.ContentPageType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;

class ContentPublicControllerSeoTest {

    private static final String BASE_URL = "https://portal.example";

    private final ContentService contentService = mock(ContentService.class);
    private final ContentPublicController controller = new ContentPublicController(
            contentService, mock(TeamMembersReader.class), BASE_URL, "Simbio", new ObjectMapper());

    @Test
    void homeProvidesAbsoluteLocaleMetadataAndPublicCacheHeaders() throws Exception {
        PublishedPageView page = page("es", "/es", Map.of("es", "/es", "en", "/en"));
        when(contentService.home("es")).thenReturn(page);
        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(controller.home("es", model, response)).isEqualTo("content/page");
        assertThat(model.get("canonicalUrl")).isEqualTo(BASE_URL + "/es");
        assertThat(model.get("alternateUrls"))
                .isEqualTo(Map.of("es", BASE_URL + "/es", "en", BASE_URL + "/en"));
        assertThat(response.getHeader("Cache-Control")).contains("public", "max-age=300");
        assertThat(response.getHeader("Content-Language")).isEqualTo("es");

        var organization = new ObjectMapper().readTree((String) model.get("organizationJsonLd"));
        assertThat(organization.get("@type").asText()).isEqualTo("Organization");
        assertThat(organization.get("name").asText()).isEqualTo("Simbio");
    }

    @Test
    void sitemapContainsAbsoluteCanonicalAndLanguageAlternatesOnlyOnce() {
        PublishedPageView spanish = page("es", "/es", Map.of("es", "/es", "en", "/en"));
        PublishedPageView english = page("en", "/en", Map.of("es", "/es", "en", "/en"));
        when(contentService.sitemapPages()).thenReturn(List.of(spanish, english, english));

        String sitemap = controller.sitemap();

        assertThat(sitemap).contains("xmlns:xhtml=\"http://www.w3.org/1999/xhtml\"");
        assertThat(sitemap).contains("<loc>https://portal.example/es</loc>");
        assertThat(sitemap).contains("<loc>https://portal.example/en</loc>");
        assertThat(sitemap).contains("hreflang=\"es\" href=\"https://portal.example/es\"");
        assertThat(sitemap).contains("hreflang=\"en\" href=\"https://portal.example/en\"");
        assertThat(sitemap.split("<url>").length - 1).isEqualTo(2);
    }

    @Test
    void robotsExcludesPrivateRoutesAndPublishesConfiguredSitemapLocation() {
        String robots = controller.robots();

        assertThat(robots).contains("Disallow: /gestion/", "Disallow: /cuenta/",
                "Disallow: /preview/", "Disallow: /api/", "Disallow: /actuator/",
                "Sitemap: " + BASE_URL + "/sitemap.xml");
    }

    private static PublishedPageView page(String locale, String canonicalPath, Map<String, String> alternates) {
        return page(locale, canonicalPath, alternates, List.of());
    }

    private static PublishedPageView page(String locale, String canonicalPath, Map<String, String> alternates,
            List<ContentBlockView> blocks) {
        return new PublishedPageView(UUID.randomUUID(), "home", ContentPageType.PAGE, locale, locale,
                locale.equals("es") ? "inicio" : "home", "Simbio", "Simbio", "Descripción pública",
                canonicalPath, alternates, 1, blocks);
    }
}
