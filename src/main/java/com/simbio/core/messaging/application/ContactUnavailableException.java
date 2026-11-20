package com.simbio.core.messaging.application;

public class ContactUnavailableException extends RuntimeException {

    public ContactUnavailableException() {
        super("Contact submission is not configured");
    }
}
