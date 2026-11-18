package com.simbio.core.config.crypto;

public class InvalidProtectedDataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidProtectedDataException() {
        super("Protected data is invalid");
    }

    public InvalidProtectedDataException(Throwable cause) {
        super("Protected data is invalid", cause);
    }
}
