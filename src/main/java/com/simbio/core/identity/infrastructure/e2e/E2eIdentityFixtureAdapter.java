package com.simbio.core.identity.infrastructure.e2e;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.identity.api.E2eIdentityFixtureApi;
import com.simbio.core.identity.api.Role;
import com.simbio.core.identity.application.E2eIdentityFixturePort;
import com.simbio.core.identity.application.UserAccountStore;

@Service
@Profile("e2e")
class E2eIdentityFixtureAdapter implements E2eIdentityFixturePort, E2eIdentityFixtureApi {
    private final UserAccountStore users;
    private final PasswordEncoder passwords;

    E2eIdentityFixtureAdapter(UserAccountStore users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Override
    @Transactional
    public UUID ensureVerifiedUser(String email, String password, String displayName, Role role) {
        return users.findByEmail(email).orElseGet(() -> users.createUser(
                email, passwords.encode(password), "en", role.name(), displayName)).getPublicId();
    }
}
