package com.simbio.core.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.notification.api.NotificationCategory;
import com.simbio.core.notification.domain.NotificationPreference;

class NotificationPreferenceServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000123");

    @Test
    void suppressesDisabledOptionalNotificationButNeverEssentialNotification() {
        Map<NotificationCategory, NotificationPreference> preferences = new HashMap<>();
        NotificationPreferenceStore store = mock(NotificationPreferenceStore.class);
        when(store.find(org.mockito.ArgumentMatchers.eq(USER_ID), any(NotificationCategory.class)))
                .thenAnswer(invocation -> Optional.ofNullable(preferences.get(invocation.getArgument(1))));
        NotificationPreferenceService service = new NotificationPreferenceService(store,
                Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC));
        service.update(USER_ID, false, true);
        preferences.put(NotificationCategory.REQUEST_UPDATES,
                new NotificationPreference(USER_ID, NotificationCategory.REQUEST_UPDATES, false, Instant.now()));

        EmailCommand optional = EmailCommand.toUser(USER_ID, EmailTemplate.REQUEST_STATUS_CHANGED,
                Locale.ENGLISH, Map.of("url", "/cuenta/solicitudes/abc"));
        EmailCommand essential = EmailCommand.toUser(USER_ID, EmailTemplate.PASSWORD_CHANGED,
                Locale.ENGLISH, Map.of());

        assertThat(service.allows(optional)).isFalse();
        assertThat(service.allows(essential)).isTrue();
    }

    @Test
    void defaultsOptionalNotificationsToEnabled() {
        NotificationPreferenceStore store = mock(NotificationPreferenceStore.class);
        when(store.find(any(), any())).thenReturn(Optional.empty());
        NotificationPreferenceService service = new NotificationPreferenceService(store);

        assertThat(service.viewFor(USER_ID)).isEqualTo(
                new NotificationPreferenceService.NotificationPreferenceView(true, true));
    }
}
