package com.simbio.core.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "portal.messaging.channels")
public record MessagingChannelProperties(
        String whatsappE164Number,
        String whatsappTextEs,
        String whatsappTextEn,
        String contactEmail) {
}
