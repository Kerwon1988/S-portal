package com.simbio.core.legal.api;

public record ConsentDocumentVersions(int termsVersion, int privacyVersion, int marketingVersion) {

    public ConsentDocumentVersions {
        if (termsVersion < 1 || privacyVersion < 1 || marketingVersion < 0) {
            throw new IllegalArgumentException("Invalid consent document versions");
        }
    }

    public boolean hasMarketingDocument() {
        return marketingVersion > 0;
    }
}
