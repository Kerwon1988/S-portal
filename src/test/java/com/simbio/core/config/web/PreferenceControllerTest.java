package com.simbio.core.config.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.CurrentUser;
import com.simbio.core.shared.spi.ThemePreference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

class PreferenceControllerTest {

    @Test
    void authenticatedUserCanSaveClientSideThemeWithoutIdentityProvider() {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("currentUser", (CurrentUser) () -> Optional.of(
                new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE, Set.of())));
        PreferenceController controller = new PreferenceController(
                new AcceptHeaderLocaleResolver(),
                beans.getBeanProvider(CurrentUser.class),
                beans.getBeanProvider(ThemePreference.class));

        assertThat(controller.updateTheme("dark").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
