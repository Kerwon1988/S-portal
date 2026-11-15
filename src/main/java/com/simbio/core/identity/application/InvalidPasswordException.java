package com.simbio.core.identity.application;

public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException() {
        super("Password does not satisfy the account policy");
    }
}
