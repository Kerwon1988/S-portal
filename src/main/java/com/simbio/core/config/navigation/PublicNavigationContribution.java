package com.simbio.core.config.navigation;

import java.util.List;
import java.util.Optional;

import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.spi.NavContribution;
import com.simbio.core.shared.spi.NavigationItem;
import org.springframework.stereotype.Component;

@Component
public class PublicNavigationContribution implements NavContribution {

    @Override
    public List<NavigationItem> itemsFor(Optional<AuthenticatedUser> user) {
        return List.of(new NavigationItem("home", "nav.home", "/{lang}", null));
    }
}
