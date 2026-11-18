package com.simbio.core.legal.application;

public class ConsentDocumentUnavailableException
        extends com.simbio.core.legal.api.ConsentDocumentsUnavailableException {

    public ConsentDocumentUnavailableException(String documentType) {
        super(documentType);
    }
}
