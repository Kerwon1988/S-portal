package com.simbio.core.config.navigation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.NavContribution;
import com.simbio.core.shared.spi.NavigationItem;
import org.junit.jupiter.api.Test;

class NavigationComposerTest {

    @Test
    void filtersNavigationByExplicitPermissionsForAnonymousAndAuthenticatedRoles() {
        NavContribution sharedItems = user -> List.of(
                new NavigationItem("home", "nav.home", "/{lang}", null),
                new NavigationItem("requests", "nav.requests", "/cuenta/solicitudes", Permission.REQUESTS_VIEW_ALL),
                new NavigationItem("cms", "nav.cms", "/gestion/contenido", Permission.CONTENT_EDIT));
        NavigationComposer composer = new NavigationComposer(List.of(sharedItems));

        assertThat(composer.itemsFor(Optional.empty())).extracting(NavigationItem::id).containsExactly("home");

        AuthenticatedUser client = new AuthenticatedUser(UUID.randomUUID(), Role.CLIENT, Set.of());
        assertThat(composer.itemsFor(Optional.of(client))).extracting(NavigationItem::id).containsExactly("home");

        AuthenticatedUser employee = new AuthenticatedUser(
                UUID.randomUUID(), Role.EMPLOYEE, Set.of(Permission.REQUESTS_VIEW_ALL));
        assertThat(composer.itemsFor(Optional.of(employee))).extracting(NavigationItem::id)
                .containsExactly("home", "requests");

        AuthenticatedUser admin = new AuthenticatedUser(
                UUID.randomUUID(), Role.ADMIN, Set.of(Permission.CONTENT_EDIT, Permission.REQUESTS_VIEW_ALL));
        assertThat(composer.itemsFor(Optional.of(admin))).extracting(NavigationItem::id)
                .containsExactly("home", "requests", "cms");
    }
}
