package com.simbio.core.identity.configuration;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
class PasswordEncoderConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder("argon2id",
                Map.of("argon2id", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
    }
}
