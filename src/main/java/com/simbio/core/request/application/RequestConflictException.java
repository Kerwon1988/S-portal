package com.simbio.core.request.application;

public class RequestConflictException extends IllegalStateException {
    public RequestConflictException(String message) { super(message); }
}
