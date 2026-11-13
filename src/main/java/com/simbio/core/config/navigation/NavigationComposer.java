package com.simbio.core.config.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.spi.NavContribution;
import com.simbio.core.shared.spi.NavigationItem;
import org.springframework.stereotype.Component;

@Component
public class NavigationComposer {

    private final List<NavContribution> contributions;

    public NavigationComposer(List<NavContribution> contributions) {
        this.contributions = List.copyOf(contributions);
    }

    public List<NavigationItem> itemsFor(Optional<AuthenticatedUser> user) {
        List<NavigationItem> items = new ArrayList<>();
        for (NavContribution contribution : contributions) {
            for (NavigationItem item : contribution.itemsFor(user)) {
                if (item.requiredPermission() == null
                        || user.filter(authenticatedUser -> authenticatedUser.hasPermission(item.requiredPermission()))
                                .isPresent()) {
                    items.add(item);
                }
            }
        }
        return List.copyOf(items);
    }
}
