package com.simbio.core.notification.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import com.simbio.core.notification.application.NotificationPreferenceService;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.CurrentUser;

class NotificationPreferenceControllerTest {

    @Test
    void savesPreferencesOnlyForAuthenticatedOwner() {
        UUID owner = UUID.randomUUID();
        CurrentUser currentUser = mock(CurrentUser.class);
        when(currentUser.currentUser()).thenReturn(Optional.of(
                new AuthenticatedUser(owner, Role.CLIENT, java.util.Set.of())));
        NotificationPreferenceService service = mock(NotificationPreferenceService.class);
        NotificationPreferenceController controller = new NotificationPreferenceController(service, currentUser);

        controller.save(false, true, new ExtendedModelMap());

        verify(service).update(owner, false, true);
    }

    @Test
    void rejectsPreferenceAccessWithoutAuthenticatedOwner() {
        CurrentUser currentUser = mock(CurrentUser.class);
        when(currentUser.currentUser()).thenReturn(Optional.empty());
        NotificationPreferenceService service = mock(NotificationPreferenceService.class);
        NotificationPreferenceController controller = new NotificationPreferenceController(service, currentUser);

        assertThatThrownBy(() -> controller.form(new ExtendedModelMap()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(403));
        verifyNoInteractions(service);
    }
}
