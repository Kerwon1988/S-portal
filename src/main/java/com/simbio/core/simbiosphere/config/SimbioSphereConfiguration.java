package com.simbio.core.simbiosphere.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.simbio.core.simbiosphere.infrastructure.SimbioSphereClientProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({SimbioSphereFallbackProperties.class, SimbioSphereClientProperties.class})
class SimbioSphereConfiguration {
}
