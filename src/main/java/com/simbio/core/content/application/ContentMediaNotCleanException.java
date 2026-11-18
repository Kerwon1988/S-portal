package com.simbio.core.content.application;

public class ContentMediaNotCleanException extends RuntimeException {

    public ContentMediaNotCleanException() {
        super("Media must pass malware scanning before it can be used");
    }
}
