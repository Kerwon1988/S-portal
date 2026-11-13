package com.simbio.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import com.simbio.core.config.navigation.NavigationComposer;
import com.simbio.core.config.theme.ThemeResolver;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.CurrentUser;
import com.simbio.core.shared.spi.ThemePreference;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ThemePreferenceTest {

    private final ThemeResolver resolver = new ThemeResolver();

    @Test
    void acceptsOnlyExplicitLightAndDarkThemeCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThat(resolver.resolve(request)).isEqualTo("system");

        request.setCookies(new Cookie("sp_theme", "dark"));
        assertThat(resolver.resolve(request)).isEqualTo("dark");

        request.setCookies(new Cookie("sp_theme", "javascript:alert(1)"));
        assertThat(resolver.resolve(request)).isEqualTo("system");
    }

    @Test
    void authenticatedUserFallsBackToCookieWhenIdentityProviderIsUnavailable() {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("currentUser", (CurrentUser) () -> Optional.of(
                new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE, Set.of())));
        WebViewAdvice advice = new WebViewAdvice(
                mock(AssetManifest.class),
                resolver,
                mock(NavigationComposer.class),
                beans.getBeanProvider(CurrentUser.class),
                beans.getBeanProvider(ThemePreference.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("sp_theme", "dark"));

        assertThat(advice.theme(request)).isEqualTo("dark");
    }

    @Test
    void authenticatedProfilePreferenceOverridesCookieWhenProviderExists() {
        UUID userId = UUID.randomUUID();
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("currentUser", (CurrentUser) () -> Optional.of(
                new AuthenticatedUser(userId, Role.EMPLOYEE, Set.of())));
        beans.addBean("themePreference", new ThemePreference() {
            @Override
            public Optional<String> findTheme(UUID requestedUserId) {
                assertThat(requestedUserId).isEqualTo(userId);
                return Optional.of("light");
            }

            @Override
            public void saveTheme(UUID requestedUserId, String theme) {
                throw new UnsupportedOperationException();
            }
        });
        WebViewAdvice advice = new WebViewAdvice(
                mock(AssetManifest.class),
                resolver,
                mock(NavigationComposer.class),
                beans.getBeanProvider(CurrentUser.class),
                beans.getBeanProvider(ThemePreference.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("sp_theme", "dark"));

        assertThat(advice.theme(request)).isEqualTo("light");
    }
}
