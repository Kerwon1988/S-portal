package com.simbio.core.legal.api;

import java.util.UUID;

import com.simbio.core.shared.security.AuthenticatedUser;

public interface ConsentRecorder {

    UUID recordAcceptance(AuthenticatedUser actor, ConsentAcceptance acceptance);

    ConsentDocumentVersions currentDocumentVersions(String locale);

    void recordRequiredAcceptances(AuthenticatedUser actor, String locale, ConsentSource source);

    void recordRequiredAcceptances(
            AuthenticatedUser actor,
            String locale,
            ConsentSource source,
            int termsVersion,
            int privacyVersion);

    void recordMarketingAcceptance(AuthenticatedUser actor, String locale, int marketingVersion);

    boolean hasMarketingConsent(UUID subjectUserId);
}
