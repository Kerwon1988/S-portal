package com.simbio.core.shared.spi;

import java.util.List;
import java.util.Optional;

import com.simbio.core.shared.security.AuthenticatedUser;

public interface NavContribution {

    List<NavigationItem> itemsFor(Optional<AuthenticatedUser> user);
}
