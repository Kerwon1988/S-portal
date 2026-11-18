package com.simbio.core.config.web;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.spi.CurrentUser;
import com.simbio.core.shared.spi.LocalePreference;
import com.simbio.core.shared.spi.ThemePreference;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.LocaleResolver;

@Controller
public class PreferenceController {

    private final LocaleResolver localeResolver;
    private final ObjectProvider<CurrentUser> currentUserProvider;
    private final ObjectProvider<ThemePreference> themePreferenceProvider;
    private final ObjectProvider<LocalePreference> localePreferenceProvider;

    public PreferenceController(
            LocaleResolver localeResolver,
            ObjectProvider<CurrentUser> currentUserProvider,
            ObjectProvider<ThemePreference> themePreferenceProvider,
            ObjectProvider<LocalePreference> localePreferenceProvider) {
        this.localeResolver = localeResolver;
        this.currentUserProvider = currentUserProvider;
        this.themePreferenceProvider = themePreferenceProvider;
        this.localePreferenceProvider = localePreferenceProvider;
    }

    @PostMapping("/preferences/locale/{lang}")
    public String updateLocale(
            @PathVariable String lang,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(name = "returnTo", required = false) String returnTo) {
        if (!lang.equals("es") && !lang.equals("en")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported locale");
        }
        localeResolver.setLocale(request, response, Locale.forLanguageTag(lang));
        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Optional<AuthenticatedUser> user = currentUser == null
                ? Optional.empty()
                : currentUser.currentUser();
        if (user.isPresent()) {
            LocalePreference preference = localePreferenceProvider.getIfAvailable();
            if (preference != null) {
                preference.saveLocale(user.get().userId(), lang);
            }
        }
        return "redirect:" + localReturnPath(returnTo, lang);
    }

    @PostMapping("/preferences/theme")
    public org.springframework.http.ResponseEntity<Void> updateTheme(@RequestParam String theme) {
        if (!theme.equals("light") && !theme.equals("dark")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported theme");
        }
        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Optional<AuthenticatedUser> user = currentUser == null
                ? Optional.empty()
                : currentUser.currentUser();
        if (user.isPresent()) {
            ThemePreference preference = themePreferenceProvider.getIfAvailable();
            if (preference != null) {
                preference.saveTheme(user.get().userId(), theme);
            }
        }
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    private String localReturnPath(String returnTo, String lang) {
        if (returnTo == null || !returnTo.startsWith("/") || returnTo.startsWith("//")) {
            return "/" + lang;
        }
        URI target;
        try {
            target = URI.create(returnTo);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid return path", exception);
        }
        if (target.isAbsolute() || target.getRawAuthority() != null || target.getRawFragment() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid return path");
        }
        String path = target.getRawPath();
        if (path == null || path.isBlank()) {
            path = "/";
        }
        if (!path.startsWith("/") || path.startsWith("//") || path.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid return path");
        }
        if (path.equals("/")) {
            path = "/" + lang;
        } else if (path.startsWith("/es/") || path.startsWith("/en/")) {
            path = "/" + lang + path.substring(3);
        } else if (path.equals("/es") || path.equals("/en")) {
            path = "/" + lang;
        }
        String query = target.getRawQuery();
        return path + (query == null ? "" : "?" + query);
    }
}
