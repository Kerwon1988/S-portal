package com.simbio.core.notification.application;

import java.util.UUID;

import com.simbio.core.notification.api.EmailCommand;

public interface EmailDeliveryPort {

    void deliver(UUID messageId, String recipient, EmailCommand command);
}
