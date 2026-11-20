package com.simbio.core.notification.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.simbio.core.messaging.api.MessagePosted;
import com.simbio.core.notification.api.RequestNotificationRequested;

@Component
public class DomainNotificationListener {

    private final DomainNotificationService notifications;

    public DomainNotificationListener(DomainNotificationService notifications) {
        this.notifications = notifications;
    }

    @EventListener
    public void on(MessagePosted event) {
        notifications.on(event);
    }

    @EventListener
    public void on(com.simbio.core.messaging.api.MessageReceived event) {
        notifications.on(event);
    }

    @EventListener
    public void on(RequestNotificationRequested event) {
        notifications.on(event);
    }
}
