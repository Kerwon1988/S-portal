package com.simbio.core.messaging.application;

public class ConversationNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConversationNotFoundException() {
        super("Conversation is unavailable");
    }
}
