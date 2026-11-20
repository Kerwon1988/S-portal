package com.simbio.core.messaging.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactForm(
        @NotBlank @Size(max = 120) String personName,
        @NotBlank @Email @Size(max = 320) String email,
        @Size(max = 24) String phone,
        @NotBlank @Size(max = 160) String reason,
        @NotBlank @Size(max = 10000) String message,
        boolean consentAccepted,
        int consentVersion,
        @Size(max = 4096) String altcha,
        @Size(max = 4096) String altchaFallback,
        @Size(max = 256) String websiteAddress,
        boolean humanConfirmation,
        Integer audioDurationSeconds) {

    public static ContactForm empty(int consentVersion) {
        return new ContactForm("", "", "", "", "", false, consentVersion,
                "", "", "", false, null);
    }

    public String antiBotPayload() {
        return altcha == null || altcha.isBlank() ? altchaFallback : altcha;
    }

    @Override
    public String toString() {
        return "ContactForm[<redacted>]";
    }
}
