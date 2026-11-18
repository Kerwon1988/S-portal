package com.simbio.core.config.web;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.NavContribution;
import com.simbio.core.shared.spi.NavigationItem;

@Component
public class ErrorInventoryNavigationContribution implements NavContribution {

    @Override
    public List<NavigationItem> itemsFor(Optional<AuthenticatedUser> user) {
        return user.filter(actor -> actor.role() == Role.ADMIN
                        && actor.hasPermission(Permission.ERROR_INVENTORY_MANAGE))
                .map(actor -> List.of(new NavigationItem(
                "error-inventory",
                        "nav.error-inventory",
                "/gestion/errores",
                Permission.ERROR_INVENTORY_MANAGE)))
                .orElseGet(List::of);
    }
}
