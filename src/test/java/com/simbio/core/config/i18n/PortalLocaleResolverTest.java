package com.simbio.core.config.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PortalLocaleResolverTest {

    private final PortalLocaleResolver resolver = new PortalLocaleResolver();

    @Test
    void usesPublicUrlLanguageBeforePrivateCookieAndHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/es/services");
        request.setCookies(new Cookie("sp_lang", "en"));
        request.addPreferredLocale(java.util.Locale.ENGLISH);

        assertThat(resolver.resolveLocale(request).getLanguage()).isEqualTo("es");
    }

    @Test
    void privateAreaUsesValidatedCookieThenAcceptLanguageAndSpanishFallback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/cuenta/profile");
        request.setCookies(new Cookie("sp_lang", "invalid"));
        request.addPreferredLocale(java.util.Locale.ENGLISH);
        assertThat(resolver.resolveLocale(request).getLanguage()).isEqualTo("en");

        request.setCookies(new Cookie("sp_lang", "es"));
        assertThat(resolver.resolveLocale(request).getLanguage()).isEqualTo("es");

        request.setCookies(new Cookie("sp_lang", "fr"));
        request.setPreferredLocales(java.util.List.of(java.util.Locale.FRENCH));
        assertThat(resolver.resolveLocale(request).getLanguage()).isEqualTo("es");
    }
}
