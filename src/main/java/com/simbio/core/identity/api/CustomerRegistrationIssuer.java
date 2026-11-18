package com.simbio.core.identity.api;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRegistrationIssuer {

    Optional<UUID> registerCustomer(String email, String password, String displayName, String locale);
}
