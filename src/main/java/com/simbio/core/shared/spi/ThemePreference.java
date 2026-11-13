package com.simbio.core.shared.spi;

import java.util.Optional;
import java.util.UUID;

public interface ThemePreference {

    Optional<String> findTheme(UUID userId);

    void saveTheme(UUID userId, String theme);
}
