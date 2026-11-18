package com.simbio.core.customer.api;

import java.util.UUID;

public interface CustomerAccountCreator {

    CustomerAccountSummary createOwnerAccount(UUID ownerUserId, String accountType, String displayName);

    CustomerAccountSummary createOwnerAccount(
            UUID creatorUserId,
            UUID ownerUserId,
            String accountType,
            String displayName);
}
