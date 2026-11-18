package com.simbio.core.config.errortracking;

public final class ErrorInventoryUnavailableException extends ErrorInventoryPersistenceException {

    public ErrorInventoryUnavailableException() {
        super("Error inventory storage is unavailable");
    }
}
