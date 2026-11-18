package com.simbio.core.shared.spi;

import java.util.Optional;
import java.util.UUID;

public interface LocalePreference {

    Optional<String> findLocale(UUID userId);

    void saveLocale(UUID userId, String locale);
}
