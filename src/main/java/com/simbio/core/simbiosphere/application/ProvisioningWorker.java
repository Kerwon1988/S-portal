package com.simbio.core.simbiosphere.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.simbio.core.identity.api.IdentityOwnerProfileApi;
import com.simbio.core.simbiosphere.domain.ProvisioningEntity;

@Service
public class ProvisioningWorker {
    private final ProvisioningClaimService claims;
    private final ProvisioningStore store;
    private final IdentityOwnerProfileApi owners;
    private final DemoProvisioningClient client;
    private final ProvisioningOutcomeService outcomes;
    private final SimbioSphereWorkerProperties properties;

    public ProvisioningWorker(ProvisioningClaimService claims, ProvisioningStore store,
            IdentityOwnerProfileApi owners, DemoProvisioningClient client,
            ProvisioningOutcomeService outcomes, SimbioSphereWorkerProperties properties) {
        this.claims = claims;
        this.store = store;
        this.owners = owners;
        this.client = client;
        this.outcomes = outcomes;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${portal.simbiosphere.worker.poll-interval:5s}")
    public void poll() {
        if (!properties.isEnabled()) return;
        claims.claim(properties.workerName()).ifPresent(this::process);
    }

    void process(UUID provisioningId) {
        ProvisioningEntity value = store.findByPublicId(provisioningId).orElse(null);
        if (value == null) return;
        var profile = owners.findOwnerProfile(value.getOwnerUserPublicId());
        if (profile.isEmpty() || !profile.get().isEmailVerified()) {
            outcomes.technicalFailure(provisioningId, "OWNER_VERIFICATION_UNAVAILABLE");
            return;
        }
        try {
            DemoProvisioningClient.OwnerOnboardingResult result = client.provision(command(value,
                    profile.get()), value.getIdempotencyKey());
            outcomes.complete(provisioningId, result);
        } catch (DemoClientException exception) {
            if (exception.retryable()) {
                outcomes.technicalFailure(provisioningId, exception.code());
            } else {
                outcomes.actionRequired(provisioningId, exception.code());
            }
        }
    }

    private DemoProvisioningClient.OwnerOnboardingCommand command(ProvisioningEntity value,
            com.simbio.core.identity.api.IdentityOwnerProfile profile) {
        String[] names = (profile.displayName() == null ? "" : profile.displayName().trim()).split("\\s+", 2);
        return new DemoProvisioningClient.OwnerOnboardingCommand(
                profile.email(), names[0], names.length > 1 ? names[1] : names[0], profile.locale(),
                new DemoProvisioningClient.Verification("VERIFIED",
                        profile.emailVerifiedAt().toString(), "EMAIL_OTP"),
                value.getBusinessName(), value.getBusinessName(), value.getBusinessSlug(),
                value.getBusinessType(), decodeModules(value.getModulesJson()), value.getLocationName(),
                value.getLocationSlug(),
                new DemoProvisioningClient.Terms(value.getTermsVersion(), value.getTermsAcceptedAt().toString()),
                new DemoProvisioningClient.ExternalReference("S_PORTAL", value.getClientAccountPublicId().toString()));
    }

    private List<String> decodeModules(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) return null;
        if ("[]".equals(json)) return List.of();
        try {
            return List.of(json.replace("[", "").replace("]", "").replace("\"", "")
                    .split(","));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Stored modules are invalid", exception);
        }
    }
}
