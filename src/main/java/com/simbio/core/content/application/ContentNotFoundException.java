package com.simbio.core.content.application;

public class ContentNotFoundException extends RuntimeException {

    public ContentNotFoundException() {
        super("Content was not found");
    }
}
