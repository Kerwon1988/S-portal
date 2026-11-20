package com.simbio.core.notification.api;

public enum NotificationCategory {
    ESSENTIAL(true),
    REQUEST_UPDATES(false),
    MESSAGE_UPDATES(false);

    private final boolean mandatory;

    NotificationCategory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isMandatory() {
        return mandatory;
    }
}
