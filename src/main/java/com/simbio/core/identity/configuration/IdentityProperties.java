package com.simbio.core.identity.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "portal.identity")
public class IdentityProperties {

    private Duration emailVerificationTtl = Duration.ofHours(24);
    private Duration passwordRecoveryTtl = Duration.ofMinutes(15);
    private Duration invitationTtl = Duration.ofDays(7);
    private String totpIssuer = "Simbio";

    public Duration getEmailVerificationTtl() {
        return emailVerificationTtl;
    }

    public void setEmailVerificationTtl(Duration emailVerificationTtl) {
        this.emailVerificationTtl = positive(emailVerificationTtl);
    }

    public Duration getPasswordRecoveryTtl() {
        return passwordRecoveryTtl;
    }

    public void setPasswordRecoveryTtl(Duration passwordRecoveryTtl) {
        this.passwordRecoveryTtl = positive(passwordRecoveryTtl);
    }

    public Duration getInvitationTtl() {
        return invitationTtl;
    }

    public void setInvitationTtl(Duration invitationTtl) {
        this.invitationTtl = positive(invitationTtl);
    }

    public String getTotpIssuer() {
        return totpIssuer;
    }

    public void setTotpIssuer(String totpIssuer) {
        if (totpIssuer == null || totpIssuer.isBlank() || totpIssuer.length() > 64) {
            throw new IllegalArgumentException("TOTP issuer must contain 1 to 64 characters");
        }
        this.totpIssuer = totpIssuer;
    }

    private static Duration positive(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Token lifetime must be positive");
        }
        return duration;
    }
}
