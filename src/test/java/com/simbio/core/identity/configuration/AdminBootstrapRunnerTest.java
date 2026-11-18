package com.simbio.core.identity.configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import com.simbio.core.identity.application.AdminBootstrapService;

class AdminBootstrapRunnerTest {

    @Test
    void doesNothingWhenBootstrapIsDisabled() {
        AdminBootstrapService bootstrapService = mock(AdminBootstrapService.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                bootstrapService, environment, false, "", "", "es");

        runner.run(new DefaultApplicationArguments());

        verifyNoInteractions(bootstrapService);
    }

    @Test
    void bootstrapsOnlyInAnExplicitlyAllowedProfile() {
        AdminBootstrapService bootstrapService = mock(AdminBootstrapService.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                bootstrapService, environment, true, "admin@example.test", "synthetic-password", "en");

        runner.run(new DefaultApplicationArguments());

        verify(bootstrapService).bootstrap("admin@example.test", "synthetic-password", "en");
    }

    @Test
    void rejectsProductionAndUnlistedProfilesWithoutCallingBootstrap() {
        for (String profile : new String[] {"prod", "staging"}) {
            AdminBootstrapService bootstrapService = mock(AdminBootstrapService.class);
            MockEnvironment environment = new MockEnvironment();
            environment.setActiveProfiles(profile);
            AdminBootstrapRunner runner = new AdminBootstrapRunner(
                    bootstrapService, environment, true, "admin@example.test", "synthetic-password", "en");

            assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                    .isInstanceOf(IllegalStateException.class);
            verifyNoInteractions(bootstrapService);
        }
    }

    @Test
    void requiresExternalCredentialsWhenEnabled() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                mock(AdminBootstrapService.class), environment, true, "", "", "es");

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class);
    }
}
