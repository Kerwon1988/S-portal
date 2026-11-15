package com.simbio.core.identity.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.simbio.core.identity.application.AdminBootstrapService;

@Component
class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminBootstrapService bootstrapService;
    private final Environment environment;
    private final boolean enabled;
    private final String email;
    private final String password;
    private final String locale;

    AdminBootstrapRunner(AdminBootstrapService bootstrapService, Environment environment,
            @Value("${portal.bootstrap.enabled:false}") boolean enabled,
            @Value("${portal.bootstrap.admin-email:}") String email,
            @Value("${portal.bootstrap.admin-password:}") String password,
            @Value("${portal.bootstrap.locale:es}") String locale) {
        this.bootstrapService = bootstrapService;
        this.environment = environment;
        this.enabled = enabled;
        this.email = email;
        this.password = password;
        this.locale = locale;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!enabled) {
            return;
        }
        if (environment.matchesProfiles("prod")) {
            throw new IllegalStateException("Admin bootstrap must not be enabled in production");
        }
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("External admin bootstrap properties are required");
        }
        bootstrapService.bootstrap(email, password, locale);
    }
}
