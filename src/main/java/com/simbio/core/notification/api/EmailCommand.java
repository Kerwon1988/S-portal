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
        if (templateRequiresAuthorizedUser(template) && recipientUserId == null) {
            throw new IllegalArgumentException("Operational email requires an authorized user recipient");
        }
        if (templateRequiresInternalLink(template)) {
            String url = variables.getOrDefault("url", variables.getOrDefault("actionUrl", ""));
            boolean relativeInternal = url.startsWith("/") && !url.startsWith("//");
            boolean configuredAbsolute = template == EmailTemplate.QUOTE_SENT
                    && (url.startsWith("http://") || url.startsWith("https://"));
            if (url.isBlank() || (!relativeInternal && !configuredAbsolute)
                    || url.contains("?") || url.contains("#")) {
                throw new IllegalArgumentException("Operational email requires a token-free internal path");
            }
        }
    }

    private static boolean templateRequiresAuthorizedUser(EmailTemplate template) {
        return templateRequiresInternalLink(template);
    }

    private static boolean templateRequiresInternalLink(EmailTemplate template) {
        return template == EmailTemplate.REQUEST_CREATED
                || template == EmailTemplate.REQUEST_STATUS_CHANGED
                || template == EmailTemplate.QUOTE_SENT
                || template == EmailTemplate.MESSAGE_POSTED
                || template == EmailTemplate.PROVISIONING_ACTION_REQUIRED
                || template == EmailTemplate.PROVISIONING_FAILED;
    }

    public static EmailCommand toUser(
            UUID userId, EmailTemplate template, Locale locale, Map<String, String> variables) {
        return new EmailCommand(null, Objects.requireNonNull(userId), template, locale, variables);
    }

    public static EmailCommand toAddress(
            EmailAddress address, EmailTemplate template, Locale locale, Map<String, String> variables) {
        return new EmailCommand(Objects.requireNonNull(address), null, template, locale, variables);
    }

    public NotificationCategory category() {
        return switch (template) {
            case REQUEST_CREATED, REQUEST_STATUS_CHANGED, QUOTE_SENT -> NotificationCategory.REQUEST_UPDATES;
            case MESSAGE_POSTED -> NotificationCategory.MESSAGE_UPDATES;
            default -> NotificationCategory.ESSENTIAL;
        };
    }

    public boolean isOperational() {
        return templateRequiresInternalLink(template);
    }

    @Override
    public String toString() {
        return "EmailCommand[recipient=<redacted>, template=" + template + ", locale=" + locale.getLanguage()
                + ", variables=<redacted>]";
    }
}
