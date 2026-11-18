package com.simbio.core.content.web;

import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.simbio.core.content.application.ContentNotFoundException;
import com.simbio.core.content.application.ContentService;
import com.simbio.core.content.application.PageResolution;
import com.simbio.core.content.application.PublishedPageView;
import com.simbio.core.content.application.PublicMedia;
import com.simbio.core.content.api.TeamMembersReader;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ContentPublicController {

    private static final CacheControl PUBLIC_PAGE_CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final ContentService contentService;
    private final TeamMembersReader teamMembersReader;
    private final String baseUrl;
    private final String brandName;
    private final ObjectMapper objectMapper;

    public ContentPublicController(ContentService contentService, TeamMembersReader teamMembersReader,
            @Value("${portal.base-url}") String baseUrl,
            @Value("${portal.brand-name:Simbio}") String brandName, ObjectMapper objectMapper) {
        this.contentService = contentService;
        this.teamMembersReader = teamMembersReader;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.brandName = brandName;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{lang:es|en}")
    public String home(@PathVariable String lang, Model model, HttpServletResponse response) {
        PublishedPageView page = contentService.home(lang);
        addPage(model, page);
        response.setHeader("Content-Language", page.locale());
        response.setHeader(HttpHeaders.CACHE_CONTROL, PUBLIC_PAGE_CACHE.getHeaderValue());
        return "content/page";
    }

    @GetMapping("/{lang:es|en}/{*slug}")
    public Object page(@PathVariable String lang, @PathVariable String slug, Model model,
            HttpServletResponse response) {
        String normalizedSlug = slug == null ? "" : slug.replaceFirst("^/", "");
        if (normalizedSlug.isBlank()) {
            throw new ContentNotFoundException();
        }
        PageResolution resolution = contentService.resolvePublic(lang, normalizedSlug);
        if (resolution.redirectPath() != null) {
            return ResponseEntity.status(301).location(URI.create(resolution.redirectPath())).build();
        }
        response.setHeader("Content-Language", resolution.page().locale());
        response.setHeader(HttpHeaders.CACHE_CONTROL, PUBLIC_PAGE_CACHE.getHeaderValue());
        addPage(model, resolution.page());
        return new ModelAndView("content/page");
    }

    @GetMapping("/public/media/{publicId}")
    @ResponseBody
    public ResponseEntity<InputStreamResource> media(@PathVariable UUID publicId) {
        PublicMedia media = contentService.openPublicMedia(publicId);
        return mediaResponse(media);
    }

    @GetMapping("/public/media/{publicId}/variants/{variantCode}")
    @ResponseBody
    public ResponseEntity<InputStreamResource> mediaVariant(@PathVariable UUID publicId,
            @PathVariable String variantCode) {
        return mediaResponse(contentService.openPublicImageVariant(publicId, variantCode));
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        Set<String> seenPaths = new HashSet<>();
        String urls = contentService.sitemapPages().stream()
                .filter(page -> seenPaths.add(page.canonicalPath()))
                .map(page -> {
                    String alternates = page.alternatePaths().entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(alternate -> "<xhtml:link rel=\"alternate\" hreflang=\""
                                    + xmlEscape(alternate.getKey()) + "\" href=\""
                                    + xmlEscape(absoluteUrl(alternate.getValue())) + "\"/>")
                            .collect(java.util.stream.Collectors.joining());
                    return "<url><loc>" + xmlEscape(absoluteUrl(page.canonicalPath()))
                            + "</loc>" + alternates + "</url>";
                })
                .collect(java.util.stream.Collectors.joining());
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset "
                + "xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" "
                + "xmlns:xhtml=\"http://www.w3.org/1999/xhtml\">" + urls + "</urlset>";
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String robots() {
        return "User-agent: *\nAllow: /\nDisallow: /gestion/\nDisallow: /cuenta/\n"
                + "Disallow: /acceso\nDisallow: /registro\nDisallow: /preview/\n"
                + "Disallow: /api/\nDisallow: /actuator/\nDisallow: /public/media/\n"
                + "Sitemap: " + baseUrl + "/sitemap.xml\n";
    }

    private void addPage(Model model, PublishedPageView page) {
        model.addAttribute("page", page);
        model.addAttribute("seoTitle", page.seoTitle());
        model.addAttribute("metaDescription", page.metaDescription());
        model.addAttribute("canonicalPath", page.canonicalPath());
        model.addAttribute("alternatePaths", page.alternatePaths());
        Map<String, String> absoluteAlternates = new LinkedHashMap<>();
        page.alternatePaths().forEach((locale, path) -> absoluteAlternates.put(locale, absoluteUrl(path)));
        model.addAttribute("canonicalUrl", absoluteUrl(page.canonicalPath()));
        model.addAttribute("alternateUrls", Map.copyOf(absoluteAlternates));
        model.addAttribute("siteName", brandName);
        model.addAttribute("openGraphLocale", page.locale().equals("es") ? "es_ES" : "en_US");
        model.addAttribute("organizationJsonLd", organizationJsonLd());
        model.addAttribute("contentLocale", page.locale());
        model.addAttribute("isPreview", false);
        if (page.pageKey().equals("about-us")) {
            model.addAttribute("teamMembers", teamMembersReader.publicMembers(page.locale()));
        }
    }

    private String organizationJsonLd() {
        if (brandName.isBlank()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "@context", "https://schema.org",
                    "@type", "Organization",
                    "name", brandName));
            return json.replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize organization metadata", exception);
        }
    }

    private String absoluteUrl(String path) {
        return baseUrl + path;
    }

    private static ResponseEntity<InputStreamResource> mediaResponse(PublicMedia media) {
        MediaType mediaType = MediaType.parseMediaType(media.detectedMime());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(media.content()));
    }

    private static String xmlEscape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
