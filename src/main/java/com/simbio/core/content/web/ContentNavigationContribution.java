package com.simbio.core.content.web;

import java.util.List;
import java.util.Optional;

import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.spi.NavContribution;
import com.simbio.core.shared.spi.NavigationItem;
import org.springframework.stereotype.Component;

@Component
public class ContentNavigationContribution implements NavContribution {

    @Override
    public List<NavigationItem> itemsFor(Optional<AuthenticatedUser> user) {
        return List.of(new NavigationItem("content", "content.nav.manage", "/gestion/contenido",
                Permission.CONTENT_EDIT));
    }
}
