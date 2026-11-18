package com.simbio.core.identity.application;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.configuration.IdentityProperties;
import com.simbio.core.identity.domain.MfaFactor;
import com.simbio.core.shared.spi.SensitiveDataProtector;

@Service
public class MfaService {

    private final MfaFactorStore factors;
    private final UserAccountStore accounts;
    private final SensitiveDataProtector dataProtector;
    private final IdentityProperties properties;
    private final AuditRecorder auditRecorder;
    private final SecureRandom secureRandom;
    private final Clock clock;

    @Autowired
    public MfaService(
            MfaFactorStore factors,
            UserAccountStore accounts,
            SensitiveDataProtector dataProtector,
            IdentityProperties properties,
            AuditRecorder auditRecorder) {
        this(factors, accounts, dataProtector, properties, auditRecorder,
                new SecureRandom(), Clock.systemUTC());
    }

    MfaService(
            MfaFactorStore factors,
            UserAccountStore accounts,
            SensitiveDataProtector dataProtector,
            IdentityProperties properties,
            AuditRecorder auditRecorder,
            SecureRandom secureRandom,
            Clock clock) {
        this.factors = factors;
        this.accounts = accounts;
        this.dataProtector = dataProtector;
        this.properties = properties;
        this.auditRecorder = auditRecorder;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @Transactional
    public MfaEnrollment beginEnrollment(UUID userId) {
        var account = accounts.findByPublicId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account is unavailable"));
        Instant now = Instant.now(clock);
        MfaFactor existing = factors.find(userId).orElse(null);
        if (existing != null && existing.isEnabled()) {
            throw new IllegalStateException("MFA is already enabled");
        }
        String secret = existing == null ? newSecret() : dataProtector.reveal(existing.getSecretEncrypted());
        if (existing == null) {
            factors.save(new MfaFactor(userId, dataProtector.protect(secret), now));
        }
        String label = URLEncoder.encode(properties.getTotpIssuer() + ":" + account.getEmailNormalized(),
                StandardCharsets.UTF_8);
        String issuer = URLEncoder.encode(properties.getTotpIssuer(), StandardCharsets.UTF_8);
        return new MfaEnrollment(secret, "otpauth://totp/" + label + "?secret=" + secret
                + "&issuer=" + issuer + "&algorithm=SHA1&digits=6&period=30");
    }

    @Transactional
    public List<String> confirmEnrollment(UUID userId, String code) {
        MfaFactor factor = factors.find(userId).orElse(null);
        if (factor == null || factor.isEnabled()) {
            return List.of();
        }
        String secret = dataProtector.reveal(factor.getSecretEncrypted());
        long step = Totp.matchingStep(secret, code, currentStep(), -1);
        if (step < 0) {
            return List.of();
        }
        List<String> recoveryCodes = generateRecoveryCodes();
        String hashes = recoveryCodes.stream().map(Totp::recoveryHash)
                .collect(java.util.stream.Collectors.joining(","));
        factor.enable(hashes, step, Instant.now(clock));
        factors.save(factor);
        auditRecorder.record(new AuditRecord(userId, "MFA_ENABLED", "USER", userId, null));
        return recoveryCodes;
    }

    @Transactional
    public boolean verifyLogin(UUID userId, String code) {
        MfaFactor factor = factors.find(userId).orElse(null);
        if (factor == null || !factor.isEnabled()) {
            return false;
        }
        String secret = dataProtector.reveal(factor.getSecretEncrypted());
        long step = Totp.matchingStep(secret, code, currentStep(), factor.getLastAcceptedStep());
        if (step >= 0) {
            factor.acceptStep(step, Instant.now(clock));
            factors.save(factor);
            return true;
        }
        if (code == null || code.length() > 64) {
            return false;
        }
        boolean recoveryAccepted = factor.consumeRecoveryHash(Totp.recoveryHash(code), Instant.now(clock));
        if (recoveryAccepted) {
            factors.save(factor);
        }
        return recoveryAccepted;
    }

    @Transactional
    public List<String> regenerateRecoveryCodes(UUID userId, String totpCode) {
        MfaFactor factor = factors.find(userId).orElse(null);
        if (factor == null || !factor.isEnabled()) {
            return List.of();
        }
        String secret = dataProtector.reveal(factor.getSecretEncrypted());
        long step = Totp.matchingStep(secret, totpCode, currentStep(), factor.getLastAcceptedStep());
        if (step < 0) {
            return List.of();
        }
        factor.acceptStep(step, Instant.now(clock));
        List<String> codes = generateRecoveryCodes();
        factor.replaceRecoveryHashes(codes.stream().map(Totp::recoveryHash)
                .collect(java.util.stream.Collectors.joining(",")), Instant.now(clock));
        factors.save(factor);
        auditRecorder.record(new AuditRecord(userId, "MFA_RECOVERY_CODES_REGENERATED",
                "USER", userId, null));
        return codes;
    }

    @Transactional(readOnly = true)
    public boolean isEnrolled(UUID userId) {
        return factors.find(userId).map(MfaFactor::isEnabled).orElse(false);
    }

    private long currentStep() {
        return Totp.currentStep(Instant.now(clock).getEpochSecond());
    }

    private String newSecret() {
        byte[] bytes = new byte[20];
        secureRandom.nextBytes(bytes);
        return Totp.encodeBase32(bytes);
    }

    private List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            byte[] bytes = new byte[10];
            secureRandom.nextBytes(bytes);
            codes.add(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        }
        return List.copyOf(codes);
    }
}
