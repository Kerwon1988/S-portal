package com.simbio.core.identity.web;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.spi.NavContribution;
import com.simbio.core.shared.spi.NavigationItem;

@Component
class IdentityNavigationContribution implements NavContribution {

    @Override
    public List<NavigationItem> itemsFor(Optional<AuthenticatedUser> user) {
        return user.isPresent() ? List.of()
                : List.of(new NavigationItem("access", "nav.access", "/acceso", null));
    }
}
