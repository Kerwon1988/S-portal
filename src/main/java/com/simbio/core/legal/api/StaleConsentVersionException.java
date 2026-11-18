package com.simbio.core.legal.api;

public class StaleConsentVersionException extends RuntimeException {

    public StaleConsentVersionException() {
        super("The consent document version has changed");
    }
}
