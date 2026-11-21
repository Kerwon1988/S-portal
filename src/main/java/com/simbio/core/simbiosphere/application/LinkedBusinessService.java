package com.simbio.core.simbiosphere.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.customer.api.CustomerDirectory;
import com.simbio.core.simbiosphere.api.LinkedBusinessQuery;
import com.simbio.core.simbiosphere.api.LinkedBusinessSummary;

@Service
public class LinkedBusinessService implements LinkedBusinessQuery {
    private final CustomerDirectory customers;
    private final LinkedBusinessStore businesses;

    public LinkedBusinessService(CustomerDirectory customers, LinkedBusinessStore businesses) {
        this.customers = customers;
        this.businesses = businesses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkedBusinessSummary> findOwnedBy(UUID userPublicId) {
        return customers.findOwnedBy(userPublicId).map(account -> businesses
                        .findByClientAccount(account.publicId()).stream()
                        .map(value -> summary(value, account.publicId())).toList())
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public LinkedBusinessSummary requireOwnedBy(UUID userPublicId, UUID businessPublicId) {
        return findOwnedBy(userPublicId).stream()
                .filter(value -> value.publicId().equals(businessPublicId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Linked business is not owned by the actor"));
    }

    private static LinkedBusinessSummary summary(com.simbio.core.simbiosphere.domain.LinkedBusinessEntity value,
            UUID accountPublicId) {
        return new LinkedBusinessSummary(value.getPublicId(), accountPublicId,
                value.getExternalBusinessId(), value.getBusinessName(), value.getBusinessType(), value.getTenantSlug());
    }
}
