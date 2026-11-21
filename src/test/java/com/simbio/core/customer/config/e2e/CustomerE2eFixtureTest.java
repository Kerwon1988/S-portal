package com.simbio.core.customer.config.e2e;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import com.simbio.core.customer.api.CustomerAccountCreator;
import com.simbio.core.customer.api.CustomerAccountSummary;
import com.simbio.core.customer.api.CustomerDirectory;
import com.simbio.core.identity.api.E2eIdentityFixtureApi;

class CustomerE2eFixtureTest {
    @Test
    void doesNotCreateDuplicateOwnerAccountOnRestart() {
        E2eIdentityFixtureApi identities = org.mockito.Mockito.mock(E2eIdentityFixtureApi.class);
        CustomerAccountCreator creator = org.mockito.Mockito.mock(CustomerAccountCreator.class);
        CustomerDirectory directory = org.mockito.Mockito.mock(CustomerDirectory.class);
        UUID client = UUID.randomUUID();
        when(identities.ensureVerifiedUser(org.mockito.ArgumentMatchers.contains("client"),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any())).thenReturn(client);
        when(directory.findOwnedBy(client)).thenReturn(Optional.of(new CustomerAccountSummary(
                UUID.randomUUID(), "COMPANY", "Synthetic", "ACTIVE", java.time.Instant.now())));

        CustomerE2eFixture fixture = new CustomerE2eFixture(identities, creator, directory);
        org.springframework.test.util.ReflectionTestUtils.setField(fixture, "clientEmail",
                "sportal-e2e-client@example.test");
        org.springframework.test.util.ReflectionTestUtils.setField(fixture, "clientPassword", "synthetic");
        org.springframework.test.util.ReflectionTestUtils.setField(fixture, "adminEmail",
                "sportal-e2e-admin@example.test");
        org.springframework.test.util.ReflectionTestUtils.setField(fixture, "adminPassword", "synthetic-admin");
        assertThatCode(() -> fixture.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
        verify(creator, never()).createOwnerAccount(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
