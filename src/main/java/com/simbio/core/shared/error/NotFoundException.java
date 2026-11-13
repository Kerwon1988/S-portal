package com.simbio.core.shared.error;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource) {
        super(resource + " not found");
    }
}
