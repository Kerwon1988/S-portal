package com.simbio.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ProdProfileTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PortalPropertiesConfiguration.class)
            .withPropertyValues(
                    "portal.error-tracking.inventory-path=target/test-error-inventory.json",
                    "portal.error-tracking.max-entries=100",
                    "portal.error-tracking.max-file-bytes=1024",
                    "portal.error-tracking.max-field-length=128",
                    "portal.error-tracking.release=test-release");

    @Test
    void loadsProdConfigurationWhenBaseUrlUsesHttps() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "portal.base-url=https://portal.example.invalid",
                        "portal.brand-name=Simbio")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void rejectsHttpBaseUrlInProd() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "portal.base-url=http://portal.example.invalid",
                        "portal.brand-name=Simbio")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessage("portal.base-url must be an absolute HTTPS URL in prod");
                });
    }
}
