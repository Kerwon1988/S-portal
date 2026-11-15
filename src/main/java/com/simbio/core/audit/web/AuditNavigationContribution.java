package com.simbio.core.audit.web;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.spi.NavContribution;
import com.simbio.core.shared.spi.NavigationItem;

@Component
class AuditNavigationContribution implements NavContribution {

    @Override
    public List<NavigationItem> itemsFor(Optional<AuthenticatedUser> user) {
        return List.of(new NavigationItem("audit", "nav.audit", "/gestion/auditoria", Permission.AUDIT_VIEW));
    }
}
