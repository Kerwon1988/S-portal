package com.simbio.core.messaging.application;

public class ConversationClosedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConversationClosedException() {
        super("Conversation is closed");
    }
}
