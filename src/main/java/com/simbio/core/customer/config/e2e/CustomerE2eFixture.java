package com.simbio.core.customer.config.e2e;

import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.customer.api.CustomerAccountCreator;
import com.simbio.core.customer.api.CustomerDirectory;
import com.simbio.core.identity.api.E2eIdentityFixtureApi;
import com.simbio.core.identity.api.Role;

@Component
@Profile("e2e")
@Order(100)
class CustomerE2eFixture implements ApplicationRunner {
    @Value("${portal.e2e.client.email:sportal-e2e-client@example.test}")
    private String clientEmail;
    @Value("${portal.e2e.client.password:SyntheticE2E!2026}")
    private String clientPassword;
    @Value("${portal.e2e.admin.email:sportal-e2e-admin@example.test}")
    private String adminEmail;
    @Value("${portal.e2e.admin.password:SyntheticE2EAdmin!2026}")
    private String adminPassword;

    private final E2eIdentityFixtureApi identities;
    private final CustomerAccountCreator customers;
    private final CustomerDirectory directory;

    CustomerE2eFixture(E2eIdentityFixtureApi identities, CustomerAccountCreator customers,
            CustomerDirectory directory) {
        this.identities = identities;
        this.customers = customers;
        this.directory = directory;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        for (String email : java.util.List.of(clientEmail,
                "sportal-e2e-client@example.test",
                "sportal-e2e-client-20261121b@example.test",
                "sportal-e2e-client-20261121c@example.test")) {
            UUID client = identities.ensureVerifiedUser(email, clientPassword,
                    "S-portal E2E Client", Role.CLIENT);
            if (directory.findOwnedBy(client).isEmpty()) {
                customers.createOwnerAccount(client, "COMPANY", "S-portal E2E Account");
            }
        }
        identities.ensureVerifiedUser(adminEmail, adminPassword,
                "S-portal E2E Admin", Role.ADMIN);
    }
}
