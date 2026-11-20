package com.simbio.core.messaging.application;

public class ContactSubmissionRejectedException extends RuntimeException {

    public ContactSubmissionRejectedException() {
        super("Contact submission was rejected");
    }
}
