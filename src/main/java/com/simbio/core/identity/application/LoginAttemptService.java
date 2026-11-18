package com.simbio.core.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {

    private final UserAccountStore accounts;
    private final Clock clock;

    @Autowired
    public LoginAttemptService(UserAccountStore accounts) {
        this(accounts, Clock.systemUTC());
    }

    LoginAttemptService(UserAccountStore accounts, Clock clock) {
        this.accounts = Objects.requireNonNull(accounts);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public void recordFailure(String normalizedEmail) {
        accounts.findByEmail(normalizedEmail).ifPresent(account -> {
            account.recordFailedLogin(Instant.now(clock));
            accounts.save(account);
        });
    }

    @Transactional
    public void resetFailures(String normalizedEmail) {
        accounts.findByEmail(normalizedEmail).ifPresent(account -> {
            account.resetLoginFailures();
            accounts.save(account);
        });
    }
}
