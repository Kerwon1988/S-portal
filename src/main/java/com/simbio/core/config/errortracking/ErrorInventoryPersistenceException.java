package com.simbio.core.config.errortracking;

public final class ErrorInventoryPersistenceException extends RuntimeException {

    public ErrorInventoryPersistenceException(String message) {
        super(message);
    }

    public ErrorInventoryPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
