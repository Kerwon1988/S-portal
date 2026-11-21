package com.simbio.core.simbiosphere.application;

import java.time.Clock;
import java.time.Instant;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.customer.api.CustomerAccountSummary;
import com.simbio.core.customer.api.CustomerDirectory;
import com.simbio.core.identity.api.IdentityOwnerProfileApi;
import com.simbio.core.simbiosphere.api.ProvisioningStateChanged;
import com.simbio.core.simbiosphere.api.ProvisioningSummary;
import com.simbio.core.simbiosphere.domain.ProvisioningEntity;

import tools.jackson.databind.ObjectMapper;

@Service
public class ProvisioningService {
    private static final String DEMO = "SIMBIOSPHERE";

    private final ProvisioningStore store;
    private final CustomerDirectory customers;
    private final IdentityOwnerProfileApi identities;
    private final ApplicationEventPublisher events;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ProvisioningService(ProvisioningStore store, CustomerDirectory customers,
            IdentityOwnerProfileApi identities, ApplicationEventPublisher events, ObjectMapper objectMapper) {
        this(store, customers, identities, events, objectMapper, Clock.systemUTC());
    }

    ProvisioningService(ProvisioningStore store, CustomerDirectory customers,
            IdentityOwnerProfileApi identities, ApplicationEventPublisher events,
            ObjectMapper objectMapper, Clock clock) {
        this.store = store;
        this.customers = customers;
        this.identities = identities;
        this.events = events;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public ProvisioningSummary begin(UUID ownerUserPublicId, ProvisioningIntent intent) {
        var ownerProfile = requireVerifiedOwner(ownerUserPublicId);
        CustomerAccountSummary account = requireAccount(ownerUserPublicId);
        Optional<ProvisioningEntity> existing = store.findProvisioning(account.publicId());
        if (existing.isPresent()) {
            return summary(existing.get());
        }
        UUID publicId = UUID.randomUUID();
        ProvisioningEntity provisioning = new ProvisioningEntity(
                publicId, account.publicId(), DEMO, publicId.toString(), Instant.now(clock));
        provisioning.attachIntent(ownerUserPublicId, intent.businessName(), intent.businessSlug(),
                intent.businessType(), intent.locationName(), intent.locationSlug(),
                encodeModules(intent.modules()), intent.termsVersion(), intent.termsAcceptedAt());
        if (ownerProfile.isEmailVerified()) {
            provisioning.markReady(Instant.now(clock));
        }
        return summary(store.save(provisioning));
    }

    @Transactional
    public boolean markReadyAfterVerification(UUID ownerUserPublicId) {
        Optional<CustomerAccountSummary> account = customers.findOwnedBy(ownerUserPublicId);
        if (account.isEmpty()) {
            return false;
        }
        Optional<ProvisioningEntity> found = store.findProvisioning(account.get().publicId());
        if (found.isEmpty() || found.get().getStatus() != com.simbio.core.simbiosphere.api.ProvisioningStatus.PENDING) {
            return false;
        }
        ProvisioningEntity provisioning = found.get();
        Instant now = Instant.now(clock);
        provisioning.markReady(now);
        store.save(provisioning);
        publishAfterCommit(new ProvisioningStateChanged(provisioning.getPublicId(),
                provisioning.getClientAccountPublicId(), provisioning.getStatus(), now));
        return true;
    }

    @Transactional
    public boolean correctActionRequired(UUID ownerUserPublicId, String businessSlug) {
        CustomerAccountSummary account = requireAccount(ownerUserPublicId);
        ProvisioningEntity value = store.findProvisioning(account.publicId()).orElseThrow(
                () -> new IllegalArgumentException("Provisioning not found"));
        if (value.getStatus() != com.simbio.core.simbiosphere.api.ProvisioningStatus.ACTION_REQUIRED) {
            throw new IllegalStateException("Provisioning does not require correction");
        }
        if (!"SLUG_TAKEN".equals(value.getErrorCode())) {
            throw new IllegalArgumentException("This error cannot be corrected in the portal");
        }
        Instant now = Instant.now(clock);
        value.correctAndRequeue(businessSlug,
                value.getPublicId() + ":correction:" + UUID.randomUUID(), now);
        store.save(value);
        publishAfterCommit(new ProvisioningStateChanged(value.getPublicId(),
                value.getClientAccountPublicId(), value.getStatus(), now));
        return true;
    }

    public URI activationLink(UUID ownerUserPublicId, DemoProvisioningClient client) {
        CustomerAccountSummary account = requireAccount(ownerUserPublicId);
        ProvisioningEntity value = store.findProvisioning(account.publicId())
                .orElseThrow(() -> new IllegalArgumentException("Provisioning not found"));
        if (value.getStatus() != com.simbio.core.simbiosphere.api.ProvisioningStatus.PROVISIONED
                || value.getExternalCustomerAccountId() == null) {
            throw new IllegalStateException("Provisioning is not ready for activation");
        }
        DemoProvisioningClient.ActivationLinkResult result = client.createActivationLink(
                value.getExternalCustomerAccountId(), value.getIdempotencyKey() + ":activation");
        URI target = URI.create(result.activationUrl());
        if (!"https".equalsIgnoreCase(target.getScheme())
                && !("http".equalsIgnoreCase(target.getScheme()) && isLoopback(target.getHost()))) {
            throw new IllegalStateException("Activation URL is not safe");
        }
        return target;
    }

    @Transactional(readOnly = true)
    public Optional<ProvisioningSummary> ownedBy(UUID ownerUserPublicId) {
        return customers.findOwnedBy(ownerUserPublicId)
                .flatMap(account -> store.findProvisioning(account.publicId()))
                .map(ProvisioningService::summary);
    }

    private com.simbio.core.identity.api.IdentityOwnerProfile requireVerifiedOwner(UUID ownerUserPublicId) {
        var profile = identities.findOwnerProfile(ownerUserPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Owner identity is required"));
        if (!profile.isEmailVerified()) {
            throw new IllegalArgumentException("Verified owner identity is required");
        }
        return profile;
    }

    private CustomerAccountSummary requireAccount(UUID ownerUserPublicId) {
        return customers.findOwnedBy(ownerUserPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Active owner account is required"));
    }

    private static boolean isLoopback(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    private String encodeModules(java.util.List<String> modules) {
        if (modules == null) return null;
        try {
            return objectMapper.writeValueAsString(modules);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Modules could not be encoded", exception);
        }
    }

    private void publishAfterCommit(ProvisioningStateChanged event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { events.publishEvent(event); }
            });
        } else {
            events.publishEvent(event);
        }
    }

    private static ProvisioningSummary summary(ProvisioningEntity value) {
        return new ProvisioningSummary(value.getPublicId(), value.getClientAccountPublicId(),
                value.getStatus(), value.getErrorCode(), value.getAttemptCount(),
                value.getCreatedAt(), value.getUpdatedAt());
    }
}
