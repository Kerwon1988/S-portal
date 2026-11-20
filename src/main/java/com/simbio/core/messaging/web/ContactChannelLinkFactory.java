package com.simbio.core.messaging.web;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.simbio.core.messaging.config.MessagingChannelProperties;
import com.simbio.core.shared.value.EmailAddress;

@Component
public class ContactChannelLinkFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactChannelLinkFactory.class);
    private static final Pattern E164 = Pattern.compile("\\+[1-9][0-9]{1,14}");
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final MessagingChannelProperties properties;
    private final String whatsappDigits;
    private final String mailtoAddress;

    public ContactChannelLinkFactory(MessagingChannelProperties properties) {
        this.properties = properties;
        this.whatsappDigits = normalizeWhatsapp(properties.whatsappE164Number());
        this.mailtoAddress = normalizeEmail(properties.contactEmail());
    }

    public ContactChannelLinks forLocale(Locale locale) {
        Objects.requireNonNull(locale, "Locale must not be null");
        String whatsappHref = whatsappDigits == null
                ? null
                : createWhatsappHref(whatsappDigits, whatsappText(locale));
        String mailtoHref = mailtoAddress == null
                ? null
                : URI.create("mailto:" + encodeEmail(mailtoAddress)).toASCIIString();
        return new ContactChannelLinks(whatsappHref, mailtoHref);
    }

    private String whatsappText(Locale locale) {
        String text = "en".equalsIgnoreCase(locale.getLanguage())
                ? properties.whatsappTextEn()
                : properties.whatsappTextEs();
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim();
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            LOGGER.warn("Configured WhatsApp message is too long and will be omitted");
            return null;
        }
        return normalized;
    }

    private static String createWhatsappHref(String digits, String text) {
        String baseHref = "https://wa.me/" + digits;
        String href = text == null ? baseHref : baseHref + "?text=" + encodeQueryValue(text);
        return URI.create(href).toASCIIString();
    }

    private static String normalizeWhatsapp(String configuredNumber) {
        if (configuredNumber == null || configuredNumber.isBlank()) {
            return null;
        }
        String normalized = configuredNumber.trim();
        if (!E164.matcher(normalized).matches()) {
            LOGGER.warn("Configured WhatsApp number is invalid and the channel will be hidden");
            return null;
        }
        return normalized.substring(1);
    }

    private static String normalizeEmail(String configuredEmail) {
        if (configuredEmail == null || configuredEmail.isBlank()) {
            return null;
        }
        try {
            EmailAddress address = EmailAddress.of(configuredEmail);
            if (address.value().length() > MAX_EMAIL_LENGTH) {
                LOGGER.warn("Configured contact email is invalid and the channel will be hidden");
                return null;
            }
            return address.value();
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Configured contact email is invalid and the channel will be hidden");
            return null;
        }
    }

    private static String encodeQueryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String encodeEmail(String email) {
        return encodeQueryValue(email).replace("%40", "@");
    }
}
