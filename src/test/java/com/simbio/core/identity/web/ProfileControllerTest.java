package com.simbio.core.identity.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.LocaleResolver;

import com.simbio.core.identity.application.PasswordRecoveryService;
import com.simbio.core.identity.application.ProfileData;
import com.simbio.core.identity.application.ProfileService;
import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.identity.security.PortalUserPrincipal;

class ProfileControllerTest {

    @Test
    void updatesOnlyTheAuthenticatedUsersProfile() {
        UUID userId = UUID.randomUUID();
        PortalUserPrincipal principal = new PortalUserPrincipal(new UserAccountEntity(
                userId,
                "synthetic@example.test",
                "synthetic-hash",
                "en",
                Instant.now(),
                new RoleEntity("CLIENT", "Client")));
        ProfileService profiles = mock(ProfileService.class);
        ProfileData updated = new ProfileData(userId, "synthetic@example.test", "Synthetic User", "es", "dark");
        when(profiles.get(userId)).thenReturn(updated);
        ProfileController controller = new ProfileController(
                profiles, mock(PasswordRecoveryService.class), mock(LocaleResolver.class));
        ProfileController.ProfileForm form = new ProfileController.ProfileForm(" Synthetic User ", "es", "dark");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        controller.updateProfile(
                form,
                bindingResult,
                principal,
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new ExtendedModelMap());

        verify(profiles).update(userId, " Synthetic User ", "es", "dark");
    }
}
