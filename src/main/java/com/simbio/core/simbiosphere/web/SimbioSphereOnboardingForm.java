package com.simbio.core.simbiosphere.web;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.simbio.core.simbiosphere.application.ProvisioningIntent;

public record SimbioSphereOnboardingForm(
        @NotBlank @Size(max = 200) String businessName,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{3,50}$") String businessSlug,
        @NotBlank @Pattern(regexp = "RESTAURANT|RETAIL|SERVICES|REPAIR_SHOP") String businessType,
        @NotBlank @Size(max = 200) String locationName,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{2,50}$") String locationSlug,
        List<@Pattern(regexp = "^[A-Z0-9_-]{1,64}$") String> modules,
        @AssertTrue boolean termsAccepted) {

    public static SimbioSphereOnboardingForm empty() {
        return new SimbioSphereOnboardingForm(null, null, null, null, null, null, false);
    }

    public ProvisioningIntent toIntent(String termsVersion, Instant acceptedAt) {
        return new ProvisioningIntent(businessName, businessSlug, businessType, locationName,
                locationSlug, modules, termsVersion, acceptedAt);
    }
}
