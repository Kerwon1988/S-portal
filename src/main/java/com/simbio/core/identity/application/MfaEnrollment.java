package com.simbio.core.identity.application;

public record MfaEnrollment(String secret, String provisioningUri) {

    @Override
    public String toString() {
        return "MfaEnrollment[secret=<redacted>, provisioningUri=<redacted>]";
    }
}
