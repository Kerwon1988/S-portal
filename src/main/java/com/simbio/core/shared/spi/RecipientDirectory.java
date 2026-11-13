package com.simbio.core.shared.spi;

import java.util.Optional;
import java.util.UUID;

import com.simbio.core.shared.value.EmailAddress;

public interface RecipientDirectory {

    Optional<EmailAddress> emailAddressOf(UUID userId);
}
