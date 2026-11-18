package com.simbio.core.customer.api;

import java.util.Optional;
import java.util.UUID;

import com.simbio.core.shared.security.AuthenticatedUser;

public interface CustomerTaxIdentifierManager {

    String setForOwnedAccount(AuthenticatedUser actor, UUID accountPublicId, String countryCode, String taxId);

    Optional<String> maskedForOwnedAccount(AuthenticatedUser actor, UUID accountPublicId);
}
