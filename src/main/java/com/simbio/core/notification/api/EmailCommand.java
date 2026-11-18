package com.simbio.core.notification.api;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.simbio.core.shared.value.EmailAddress;

public record EmailCommand(
        EmailAddress recipient,
        UUID recipientUserId,
        EmailTemplate template,
        Locale locale,
        Map<String, String> variables) {

    public EmailCommand {
        if ((recipient == null) == (recipientUserId == null)) {
            throw new IllegalArgumentException("Specify either an address or a user ID as recipient");
        }
        Objects.requireNonNull(template, "template must not be null");
        Objects.requireNonNull(locale, "locale must not be null");
        if (!locale.getLanguage().equals("es") && !locale.getLanguage().equals("en")) {
            throw new IllegalArgumentException("Only Spanish and English email locales are supported");
        }
        variables = Map.copyOf(Objects.requireNonNull(variables, "variables must not be null"));
        if (variables.size() > 8 || variables.values().stream().anyMatch(value -> value == null
                || value.length() > 4096)) {
            throw new IllegalArgumentException("Email variables exceed the allowed bounds");
        }
    }

    public static EmailCommand toUser(
            UUID userId, EmailTemplate template, Locale locale, Map<String, String> variables) {
        return new EmailCommand(null, Objects.requireNonNull(userId), template, locale, variables);
    }

    public static EmailCommand toAddress(
            EmailAddress address, EmailTemplate template, Locale locale, Map<String, String> variables) {
        return new EmailCommand(Objects.requireNonNull(address), null, template, locale, variables);
    }

    @Override
    public String toString() {
        return "EmailCommand[recipient=<redacted>, template=" + template + ", locale=" + locale.getLanguage()
                + ", variables=<redacted>]";
    }
}
