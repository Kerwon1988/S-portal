package com.simbio.core.shared.spi;

import java.util.Optional;

import com.simbio.core.shared.security.AuthenticatedUser;

public interface CurrentUser {

    Optional<AuthenticatedUser> currentUser();
}
