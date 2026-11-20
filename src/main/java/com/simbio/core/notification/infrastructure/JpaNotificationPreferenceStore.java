package com.simbio.core.notification.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.simbio.core.notification.api.NotificationCategory;
import com.simbio.core.notification.application.NotificationPreferenceStore;
import com.simbio.core.notification.domain.NotificationPreference;

@Repository
class JpaNotificationPreferenceStore implements NotificationPreferenceStore {

    private final NotificationPreferenceJpaRepository repository;

    JpaNotificationPreferenceStore(NotificationPreferenceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<NotificationPreference> find(UUID userId, NotificationCategory category) {
        return repository.findByUserIdAndCategory(userId, category);
    }

    @Override
    public void save(NotificationPreference preference) {
        repository.save(preference);
    }
}
