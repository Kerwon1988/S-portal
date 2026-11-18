package com.simbio.core.shared.spi;

public interface AntiBotVerification {

    AntiBotChallenge issueChallenge();

    boolean verify(String altchaPayload, String honeypot, boolean accessibleConfirmation);
}
