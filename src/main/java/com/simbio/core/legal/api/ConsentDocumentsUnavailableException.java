package com.simbio.core.legal.api;

public class ConsentDocumentsUnavailableException extends RuntimeException {

    public ConsentDocumentsUnavailableException(String documentType) {
        super("A current " + documentType + " document is unavailable");
    }
}
