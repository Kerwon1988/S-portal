package com.simbio.core.content.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import com.simbio.core.content.api.LegalDocumentsReader;
import com.simbio.core.content.api.PublishedLegalDocument;
import com.simbio.core.content.api.SanitizedHtml;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;

class PublicLegalFooterAdviceTest {

    private final LegalDocumentsReader legalDocuments = mock(LegalDocumentsReader.class);
    private final StaticMessageSource messages = new StaticMessageSource();
    private final PublicLegalFooterAdvice advice = new PublicLegalFooterAdvice(legalDocuments, messages);

    @Test
    void footerLinksContainOnlyPublishedDocumentsInTheRequestedLocale() {
        messages.addMessage("content.legal.link.notice", Locale.ENGLISH, "Legal notice");
        when(legalDocuments.current("NOTICE", "en")).thenReturn(Optional.of(new PublishedLegalDocument(
                "NOTICE", "en", 1, "Notice", new SanitizedHtml("Approved"), Instant.EPOCH)));
        when(legalDocuments.current("PRIVACY", "en")).thenReturn(Optional.empty());
        when(legalDocuments.current("COOKIES", "en")).thenReturn(Optional.empty());
        when(legalDocuments.current("TERMS", "en")).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/portal");
        request.setRequestURI("/portal/en/home");

        assertThat(advice.footerLegalLinks(request))
                .containsExactly(new PublicLegalLink("Legal notice", "/en/legal/legal-notice"));
        verify(legalDocuments).current("NOTICE", "en");
        verify(legalDocuments).current("PRIVACY", "en");
        verify(legalDocuments).current("COOKIES", "en");
        verify(legalDocuments).current("TERMS", "en");
    }

    @Test
    void spanishPublishedLegalDocumentUsesSpanishLabelAndSlug() {
        messages.addMessage("content.legal.link.cookies", Locale.forLanguageTag("es"), "Cookies");
        when(legalDocuments.current("COOKIES", "es")).thenReturn(Optional.of(new PublishedLegalDocument(
                "COOKIES", "es", 1, "Cookies", new SanitizedHtml("Approved"), Instant.EPOCH)));
        when(legalDocuments.current("NOTICE", "es")).thenReturn(Optional.empty());
        when(legalDocuments.current("PRIVACY", "es")).thenReturn(Optional.empty());
        when(legalDocuments.current("TERMS", "es")).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/es/legal/cookies");

        assertThat(advice.footerLegalLinks(request))
                .containsExactly(new PublicLegalLink("Cookies", "/es/legal/cookies"));
    }

    @Test
    void unsupportedRoutesDoNotQueryLegalDocuments() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/gestion/contenido");

        assertThat(advice.footerLegalLinks(request)).isEmpty();

        verifyNoInteractions(legalDocuments);
    }
}
