package com.simbio.core.request.web;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.NavContribution;
import com.simbio.core.shared.spi.NavigationItem;

@Component
class RequestNavigationContribution implements NavContribution {
    @Override
    public List<NavigationItem> itemsFor(Optional<AuthenticatedUser> user) {
        if (user.isPresent() && user.get().role() == Role.CLIENT) {
            return List.of(new NavigationItem("requests", "request.nav", "/cuenta/solicitudes", null));
        }
        return user.filter(actor -> actor.role() == Role.ADMIN
                        || actor.hasPermission(Permission.REQUESTS_VIEW_ALL))
                .map(ignored -> List.of(new NavigationItem(
                        "request-operations", "employee.requests.heading", "/gestion/solicitudes", null)))
                .orElseGet(List::of);
    }
}
