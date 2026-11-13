package com.simbio.core.config.i18n;

import java.util.Locale;
import java.util.Set;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.LocaleResolver;

public class PortalLocaleResolver implements LocaleResolver {

    private static final Set<String> SUPPORTED = Set.of("es", "en");
    private static final String LANGUAGE_COOKIE = "sp_lang";

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String[] pathSegments = request.getRequestURI().split("/", 3);
        String pathLanguage = pathSegments.length > 1 ? pathSegments[1] : "";
        if (SUPPORTED.contains(pathLanguage)) {
            return Locale.forLanguageTag(pathLanguage);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (LANGUAGE_COOKIE.equals(cookie.getName()) && SUPPORTED.contains(cookie.getValue())) {
                    return Locale.forLanguageTag(cookie.getValue());
                }
            }
        }

        String acceptedLanguage = request.getLocale().getLanguage();
        return Locale.forLanguageTag(SUPPORTED.contains(acceptedLanguage) ? acceptedLanguage : "es");
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        if (locale == null || !SUPPORTED.contains(locale.getLanguage())) {
            throw new IllegalArgumentException("Only ES and EN locales are supported");
        }
        response.addHeader("Set-Cookie", LANGUAGE_COOKIE + "=" + locale.getLanguage()
                + "; Path=/; Max-Age=31536000; SameSite=Lax; HttpOnly"
                + (request.isSecure() ? "; Secure" : ""));
    }
}
