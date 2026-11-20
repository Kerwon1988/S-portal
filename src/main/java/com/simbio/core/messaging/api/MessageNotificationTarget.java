package com.simbio.core.messaging.api;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import com.simbio.core.shared.value.EmailAddress;

public record MessageNotificationTarget(
        UUID recipientUserId,
        EmailAddress recipientAddress,
        Locale locale,
        String internalPath) {

    public MessageNotificationTarget {
        if ((recipientUserId == null) == (recipientAddress == null)) {
            throw new IllegalArgumentException("Specify exactly one message recipient");
        }
        Objects.requireNonNull(locale, "locale must not be null");
        if (internalPath != null && (!internalPath.startsWith("/") || internalPath.startsWith("//")
                || internalPath.contains("?") || internalPath.contains("#"))) {
            throw new IllegalArgumentException("Internal path must remain token-free and on the portal origin");
        }
    }

    public static MessageNotificationTarget toUser(UUID userId, String path) {
        return new MessageNotificationTarget(Objects.requireNonNull(userId), null, Locale.forLanguageTag("es"), path);
    }

    public static MessageNotificationTarget toAddress(EmailAddress address, Locale locale) {
        return new MessageNotificationTarget(null, Objects.requireNonNull(address), locale, null);
    }
}
