package com.simbio.core.shared.spi;

import java.util.UUID;

public interface ClientAccountOwnership {

    boolean hasActiveClientAccount(UUID userPublicId);
}
