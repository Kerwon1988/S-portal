package com.simbio.core.customer.application;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.shared.spi.ClientAccountOwnership;

@Component
class ClientAccountOwnershipAdapter implements ClientAccountOwnership {

    private final ClientAccountStore accounts;

    ClientAccountOwnershipAdapter(ClientAccountStore accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveClientAccount(UUID userPublicId) {
        return userPublicId != null && accounts.findOwnedBy(userPublicId)
                .filter(account -> "ACTIVE".equals(account.getStatus()))
                .isPresent();
    }
}
