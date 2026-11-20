package com.simbio.core.notification.application;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.NotificationCategory;
import com.simbio.core.notification.domain.NotificationPreference;

@Service
public class NotificationPreferenceService {

    private static final Set<NotificationCategory> OPTIONAL_CATEGORIES = EnumSet.of(
            NotificationCategory.REQUEST_UPDATES, NotificationCategory.MESSAGE_UPDATES);

    private final NotificationPreferenceStore store;
    private final Clock clock;

    @Autowired
    public NotificationPreferenceService(NotificationPreferenceStore store) {
        this(store, Clock.systemUTC());
    }

    NotificationPreferenceService(NotificationPreferenceStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceView viewFor(UUID userId) {
        requireUser(userId);
        return new NotificationPreferenceView(
                enabled(userId, NotificationCategory.REQUEST_UPDATES),
                enabled(userId, NotificationCategory.MESSAGE_UPDATES));
    }

    @Transactional
    public void update(UUID userId, boolean requestUpdates, boolean messageUpdates) {
        requireUser(userId);
        save(userId, NotificationCategory.REQUEST_UPDATES, requestUpdates);
        save(userId, NotificationCategory.MESSAGE_UPDATES, messageUpdates);
    }

    @Transactional(readOnly = true)
    public boolean allows(EmailCommand command) {
        if (command.category().isMandatory() || command.recipientUserId() == null) {
            return true;
        }
        return enabled(command.recipientUserId(), command.category());
    }

    private boolean enabled(UUID userId, NotificationCategory category) {
        if (!OPTIONAL_CATEGORIES.contains(category)) {
            return true;
        }
        return store.find(userId, category).map(NotificationPreference::isEnabled).orElse(true);
    }

    private void save(UUID userId, NotificationCategory category, boolean enabled) {
        Instant now = Instant.now(clock);
        store.find(userId, category).ifPresentOrElse(
                preference -> {
                    preference.update(enabled, now);
                    store.save(preference);
                },
                () -> store.save(new NotificationPreference(userId, category, enabled, now)));
    }

    private static void requireUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Notification preferences require a user");
        }
    }

    public record NotificationPreferenceView(boolean requestUpdates, boolean messageUpdates) {
    }
}
