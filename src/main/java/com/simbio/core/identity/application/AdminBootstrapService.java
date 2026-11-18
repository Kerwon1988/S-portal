package com.simbio.core.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.api.IdentityAccountChanged;

@Service
public class AdminBootstrapService {

    private final UserAccountStore accounts;
    private final BootstrapStateStore bootstrapState;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder auditRecorder;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Autowired
    public AdminBootstrapService(UserAccountStore accounts, BootstrapStateStore bootstrapState,
            PasswordPolicy passwordPolicy, PasswordEncoder passwordEncoder, AuditRecorder auditRecorder,
            ApplicationEventPublisher events) {
        this(accounts, bootstrapState, passwordPolicy, passwordEncoder, auditRecorder, events, Clock.systemUTC());
    }

    AdminBootstrapService(UserAccountStore accounts, BootstrapStateStore bootstrapState,
            PasswordPolicy passwordPolicy, PasswordEncoder passwordEncoder, AuditRecorder auditRecorder,
            ApplicationEventPublisher events, Clock clock) {
        this.accounts = Objects.requireNonNull(accounts);
        this.bootstrapState = Objects.requireNonNull(bootstrapState);
        this.passwordPolicy = Objects.requireNonNull(passwordPolicy);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.auditRecorder = Objects.requireNonNull(auditRecorder);
        this.events = Objects.requireNonNull(events);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public boolean bootstrap(String email, String password, String locale) {
        if (bootstrapState.wasConsumed()) {
            return false;
        }
        Instant now = Instant.now(clock);
        if (accounts.existsAdmin()) {
            bootstrapState.consume(now);
            return false;
        }
        String normalizedEmail = normalizeEmail(email);
        passwordPolicy.requireAccepted(password);
        var administrator = accounts.createAdmin(normalizedEmail, passwordEncoder.encode(password),
                "en".equals(locale) ? "en" : "es");
        events.publishEvent(new IdentityAccountChanged(administrator.getPublicId(), "ADMIN", "ACTIVE"));
        bootstrapState.consume(now);
        auditRecorder.record(new AuditRecord(null, "IDENTITY.ADMIN_BOOTSTRAP", "USER",
                administrator.getPublicId(), "bootstrap"));
        return true;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank() || email.length() > 320
                || !email.matches("(?i)^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Bootstrap email is invalid");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
