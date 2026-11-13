package com.simbio.core.shared.spi;

import java.util.Objects;

import com.simbio.core.shared.security.Permission;

public record NavigationItem(String id, String labelKey, String href, Permission requiredPermission) {

    public NavigationItem {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(labelKey, "labelKey must not be null");
        Objects.requireNonNull(href, "href must not be null");
        if (!href.startsWith("/") || href.startsWith("//")) {
            throw new IllegalArgumentException("Navigation href must be an absolute local path");
        }
    }
}
