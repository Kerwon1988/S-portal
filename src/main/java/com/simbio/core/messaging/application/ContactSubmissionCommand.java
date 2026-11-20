package com.simbio.core.messaging.application;

import java.util.List;
import java.util.UUID;

public record ContactSubmissionCommand(
        String personName,
        String email,
        String phone,
        String reason,
        String message,
        String locale,
        boolean consentAccepted,
        int consentVersion,
        String altchaPayload,
        String honeypot,
        boolean accessibleConfirmation,
        UUID authenticatedClientId,
        List<ContactAttachmentInput> attachments) {

    public ContactSubmissionCommand {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}
