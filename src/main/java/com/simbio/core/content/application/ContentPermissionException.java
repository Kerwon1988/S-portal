package com.simbio.core.content.application;

public class ContentPermissionException extends RuntimeException {

    public ContentPermissionException() {
        super("CONTENT_EDIT permission is required");
    }
}
