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
        messageSource.setBasename("i18n.config.messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(Locale.forLanguageTag("es"));
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}
