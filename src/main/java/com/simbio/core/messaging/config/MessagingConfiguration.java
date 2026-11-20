package com.simbio.core.messaging.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({MessagingChannelProperties.class, MessagingRetentionProperties.class,
        MessagingContactProperties.class, MessagingPollingProperties.class})
class MessagingConfiguration {
}
