package com.simbio.core.notification.api;

public enum EmailTemplate {
    EMAIL_VERIFICATION,
    PASSWORD_RECOVERY,
    PASSWORD_CHANGED,
    ACCOUNT_SECURITY_CHANGED,
    INVITATION,
    REQUEST_CREATED,
    REQUEST_STATUS_CHANGED,
    QUOTE_SENT,
    MESSAGE_POSTED,
    CONTACT_ACKNOWLEDGEMENT,
    PROVISIONING_ACTION_REQUIRED,
    PROVISIONING_FAILED;

    public boolean isRequestOperational() {
        return this == REQUEST_CREATED || this == REQUEST_STATUS_CHANGED || this == QUOTE_SENT;
    }
}
