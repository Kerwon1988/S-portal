package com.simbio.core.customer.api;

import java.util.UUID;

import com.simbio.core.shared.security.AuthenticatedUser;

public interface CustomerPhoneManager {

    void setForOwnedAccount(AuthenticatedUser actor, UUID accountPublicId, String phoneNumber);
}
