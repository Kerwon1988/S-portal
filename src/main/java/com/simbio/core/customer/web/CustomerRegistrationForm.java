package com.simbio.core.customer.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.simbio.core.customer.application.CustomerRegistrationCommand;
import com.simbio.core.legal.api.ConsentDocumentVersions;

public record CustomerRegistrationForm(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 12, max = 64) String password,
        @NotBlank @Size(max = 120) String personName,
        @Size(max = 160) String accountName,
        @NotBlank @Pattern(regexp = "COMPANY|SOLE_TRADER|INDIVIDUAL") String accountType,
        @Pattern(regexp = "^$|\\+[1-9][0-9]{0,14}") @Size(max = 16) String phoneNumber,
        @Pattern(regexp = "^$|[A-Za-z]{2}") @Size(max = 2) String countryCode,
        @Size(max = 32) String taxIdentifier,
        boolean termsAccepted,
        boolean privacyAccepted,
        boolean marketingAccepted,
        int termsVersion,
        int privacyVersion,
        int marketingVersion,
        @Size(max = 4096) String altcha,
        @Size(max = 4096) String altchaFallback,
        @Size(max = 256) String websiteAddress,
        boolean humanConfirmation) {

    public static CustomerRegistrationForm empty(ConsentDocumentVersions versions) {
        int terms = versions == null ? 0 : versions.termsVersion();
        int privacy = versions == null ? 0 : versions.privacyVersion();
        int marketing = versions == null ? 0 : versions.marketingVersion();
        return new CustomerRegistrationForm("", "", "", "", "INDIVIDUAL", "", "ES", "",
                false, false, false, terms, privacy, marketing, "", "", "", false);
    }

    public CustomerRegistrationCommand toCommand(String locale) {
        String antiBotPayload = altcha == null || altcha.isBlank() ? altchaFallback : altcha;
        return new CustomerRegistrationCommand(email, password, personName, accountName, accountType,
                locale, phoneNumber, countryCode, taxIdentifier, termsAccepted, privacyAccepted,
                marketingAccepted, termsVersion, privacyVersion, marketingVersion, antiBotPayload,
                websiteAddress, humanConfirmation);
    }

    @Override
    public String toString() {
        return "CustomerRegistrationForm[<redacted>]";
    }
}
