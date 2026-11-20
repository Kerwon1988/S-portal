package com.simbio.core.simbiosphere.application;

import java.util.List;

public interface DemoProvisioningClient {
    OwnerOnboardingResult provision(OwnerOnboardingCommand command, String idempotencyKey);
    SlugAvailability checkSlug(String slug);
    ActivationLinkResult createActivationLink(long customerAccountId, String idempotencyKey);

    record OwnerOnboardingCommand(
            String email,
            String firstName,
            String lastName,
            String locale,
            Verification verification,
            String accountName,
            String businessName,
            String businessSlug,
            String businessType,
            List<String> modules,
            String locationName,
            String locationSlug,
            Terms terms,
            ExternalReference externalReference) {
    }

    record Verification(String status, String verifiedAt, String evidence) { }
    record Terms(String version, String acceptedAt) { }
    record ExternalReference(String source, String externalAccountId) { }

    record OwnerOnboardingResult(
            String externalAccountId,
            long customerAccountId,
            long tenantId,
            String tenantSlug,
            List<String> modulesApplied,
            String status) {
    }

    record SlugAvailability(String slug, boolean available, List<String> suggestions) { }
    record ActivationLinkResult(String activationUrl, String expiresAt) { }
}
