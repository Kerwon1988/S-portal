package com.simbio.core.simbiosphere.api;

import java.util.UUID;

/**
 * R2 contract for linking an existing external account after verified proof of possession.
 * No production implementation exists in MVP.
 */
public interface AccountLinkPort {
    AccountLinkChallenge issueChallenge(UUID clientAccountPublicId, String externalAccountReference,
            AccountLinkLocale locale);
    AccountLinkResult confirmChallenge(UUID clientAccountPublicId, String challengeId,
            String proof);
}
