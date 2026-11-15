package com.simbio.core.identity.application;

import java.util.Optional;

import com.simbio.core.shared.security.AuthenticatedUser;

public interface CurrentAuthenticatedUser {

    Optional<AuthenticatedUser> currentUser();
}
