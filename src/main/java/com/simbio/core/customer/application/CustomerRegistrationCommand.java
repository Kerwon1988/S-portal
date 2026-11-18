package com.simbio.core.customer.application;

public record CustomerRegistrationCommand(
        String email,
        String password,
        String personName,
        String accountName,
        String accountType,
        String locale,
        String phoneNumber,
        String countryCode,
        String taxIdentifier,
        boolean termsAccepted,
        boolean privacyAccepted,
        boolean marketingAccepted,
        int termsVersion,
        int privacyVersion,
        int marketingVersion,
        String altchaPayload,
        String honeypot,
        boolean accessibleConfirmation) {

    @Override
    public String toString() {
        return "CustomerRegistrationCommand[<redacted>]";
    }
}
