package com.simbio.core.config.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;

@Configuration(proxyBeanMethods = false)
public class LocaleConfig {

    @Bean
    LocaleResolver localeResolver() {
        return new PortalLocaleResolver();
    }

    @Bean
    MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames(
                "i18n.config.messages",
                "i18n.audit.messages",
                "i18n.identity.messages",
                "i18n.notification.messages",
                "i18n.customer.messages",
                "i18n.staff.messages",
                "i18n.legal.messages",
                "i18n.messaging.messages",
                "i18n.request.messages",
                "i18n.simbiosphere.messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(Locale.forLanguageTag("es"));
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}
