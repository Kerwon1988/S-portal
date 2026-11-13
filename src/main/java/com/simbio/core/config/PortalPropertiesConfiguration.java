package com.simbio.core.config;

import java.net.URI;
import java.util.Arrays;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.simbio.core.config.errortracking.ErrorTrackingProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({PortalProperties.class, ErrorTrackingProperties.class})
public class PortalPropertiesConfiguration implements EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Bean
    SmartInitializingSingleton validateProductionBaseUrl(PortalProperties properties) {
        return () -> {
            boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");
            if (production) {
                URI baseUri;
                try {
                    baseUri = URI.create(properties.baseUrl());
                } catch (IllegalArgumentException exception) {
                    throw new IllegalStateException("portal.base-url must be an absolute HTTPS URL in prod", exception);
                }
                if (!"https".equalsIgnoreCase(baseUri.getScheme()) || baseUri.getHost() == null) {
                    throw new IllegalStateException("portal.base-url must be an absolute HTTPS URL in prod");
                }
            }
        };
    }
}
