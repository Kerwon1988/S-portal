package com.simbio.core.config;

import jakarta.servlet.http.HttpServletRequest;

import com.simbio.core.config.navigation.NavigationComposer;
import com.simbio.core.config.theme.ThemeResolver;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.spi.CurrentUser;
import com.simbio.core.shared.spi.ThemePreference;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class WebViewAdvice {

    private final AssetManifest assetManifest;
    private final ThemeResolver themeResolver;
    private final NavigationComposer navigationComposer;
    private final ObjectProvider<CurrentUser> currentUserProvider;
    private final ObjectProvider<ThemePreference> themePreferenceProvider;

    public WebViewAdvice(
            AssetManifest assetManifest,
            ThemeResolver themeResolver,
            NavigationComposer navigationComposer,
            ObjectProvider<CurrentUser> currentUserProvider,
            ObjectProvider<ThemePreference> themePreferenceProvider) {
        this.assetManifest = assetManifest;
        this.themeResolver = themeResolver;
        this.navigationComposer = navigationComposer;
        this.currentUserProvider = currentUserProvider;
        this.themePreferenceProvider = themePreferenceProvider;
    }

    @ModelAttribute("assets")
    public java.util.Map<String, String> assets() {
        return assetManifest.assets();
    }

    @ModelAttribute("cspNonce")
    public Object cspNonce(HttpServletRequest request) {
        return request.getAttribute("cspNonce");
    }

    @ModelAttribute("portalCorrelationId")
    public String portalCorrelationId(HttpServletRequest request) {
        Object correlationId = request.getAttribute("correlationId");
        return correlationId instanceof String value ? value : "";
    }

    @ModelAttribute("theme")
    public String theme(HttpServletRequest request) {
        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        if (currentUser != null) {
            java.util.Optional<AuthenticatedUser> authenticatedUser = currentUser.currentUser();
            if (authenticatedUser.isPresent()) {
                ThemePreference preference = themePreferenceProvider.getIfAvailable();
                if (preference != null) {
                    java.util.Optional<String> savedTheme = preference.findTheme(authenticatedUser.get().userId())
                            .filter(value -> value.equals("light") || value.equals("dark"));
                    if (savedTheme.isPresent()) {
                        return savedTheme.get();
                    }
                }
            }
        }
        return themeResolver.resolve(request);
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("navigationItems")
    public java.util.List<com.simbio.core.shared.spi.NavigationItem> navigationItems() {
        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        return navigationComposer.itemsFor(
                currentUser == null ? java.util.Optional.empty() : currentUser.currentUser());
    }
}
