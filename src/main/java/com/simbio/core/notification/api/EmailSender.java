package com.simbio.core.notification.api;

import java.util.UUID;

public interface EmailSender {

    UUID enqueue(EmailCommand command);

    default UUID enqueueOnce(String idempotencyKey, EmailCommand command) {
        return enqueue(command);
    }
}
