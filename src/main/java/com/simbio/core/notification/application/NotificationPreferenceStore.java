package com.simbio.core.notification.application;

import java.util.Optional;
import java.util.UUID;

import com.simbio.core.notification.api.NotificationCategory;
import com.simbio.core.notification.domain.NotificationPreference;

public interface NotificationPreferenceStore {

    Optional<NotificationPreference> find(UUID userId, NotificationCategory category);

    void save(NotificationPreference preference);
}
