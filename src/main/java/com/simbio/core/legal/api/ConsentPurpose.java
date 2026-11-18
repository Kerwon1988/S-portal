package com.simbio.core.legal.api;

public enum ConsentPurpose {
    TERMS_ACCEPTANCE("TERMS"),
    PRIVACY_ACKNOWLEDGEMENT("PRIVACY"),
    MARKETING_COMMUNICATIONS("MARKETING");

    private final String documentType;

    ConsentPurpose(String documentType) {
        this.documentType = documentType;
    }

    public String documentType() {
        return documentType;
    }
}
