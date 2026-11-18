package com.simbio.core.content.web;

import java.util.List;
import java.util.Locale;

import com.simbio.core.content.api.LegalDocumentsReader;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.simbio.core.content.web")
public class PublicLegalFooterAdvice {

    private static final List<LegalLinkDefinition> LEGAL_LINKS = List.of(
            new LegalLinkDefinition("NOTICE", "content.legal.link.notice", "aviso-legal", "legal-notice"),
            new LegalLinkDefinition("PRIVACY", "content.legal.link.privacy", "privacidad", "privacy"),
            new LegalLinkDefinition("COOKIES", "content.legal.link.cookies", "cookies", "cookies"),
            new LegalLinkDefinition("TERMS", "content.legal.link.terms", "terminos", "terms"));

    private final LegalDocumentsReader legalDocuments;
    private final MessageSource messageSource;

    public PublicLegalFooterAdvice(LegalDocumentsReader legalDocuments, MessageSource messageSource) {
        this.legalDocuments = legalDocuments;
        this.messageSource = messageSource;
    }

    @ModelAttribute("footerLegalLinks")
    public List<PublicLegalLink> footerLegalLinks(HttpServletRequest request) {
        String locale = localeFromPath(request);
        if (locale == null) {
            return List.of();
        }
        Locale messageLocale = Locale.forLanguageTag(locale);
        return LEGAL_LINKS.stream()
                .filter(link -> legalDocuments.current(link.type(), locale).isPresent())
                .map(link -> new PublicLegalLink(
                        messageSource.getMessage(link.messageKey(), null, messageLocale),
                        "/" + locale + "/legal/" + link.slug(locale)))
                .toList();
    }

    private static String localeFromPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String path = request.getRequestURI().substring(contextPath.length());
        String[] segments = path.split("/");
        if (segments.length > 1 && (segments[1].equals("es") || segments[1].equals("en"))) {
            return segments[1];
        }
        return null;
    }

    private record LegalLinkDefinition(String type, String messageKey, String spanishSlug, String englishSlug) {

        private String slug(String locale) {
            return locale.equals("es") ? spanishSlug : englishSlug;
        }
    }
}
