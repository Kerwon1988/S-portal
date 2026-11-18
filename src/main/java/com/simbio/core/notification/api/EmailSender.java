package com.simbio.core.notification.api;

import java.util.UUID;

public interface EmailSender {

    UUID enqueue(EmailCommand command);
}
