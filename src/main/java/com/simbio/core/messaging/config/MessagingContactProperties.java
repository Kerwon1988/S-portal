package com.simbio.core.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "portal.messaging.contact")
public record MessagingContactProperties(
        String consentTextEs,
        String consentTextEn,
        int consentVersion,
        int maxAttachments,
        long maxAttachmentBytes,
        long maxTotalAttachmentBytes,
        long maxAudioBytes) {

    public MessagingContactProperties {
        consentTextEs = clean(consentTextEs);
        consentTextEn = clean(consentTextEn);
        if (maxAttachments < 1 || maxAttachmentBytes < 1 || maxTotalAttachmentBytes < 1 || maxAudioBytes < 1) {
            throw new IllegalArgumentException("Contact attachment limits must be positive");
        }
    }

    public boolean available(String locale) {
        return consentVersion > 0 && !consentText(locale).isBlank();
    }

    public String consentText(String locale) {
        return "en".equals(locale) ? consentTextEn : consentTextEs;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
