package com.simbio.core.simbiosphere.web;

import java.time.Clock;
import java.time.Instant;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.simbiosphere.application.ProvisioningService;

@Controller
public class MySimbioSphereController {

    private static final String TERMS_VERSION = "SIM-TERMS-1";

    private final ProvisioningService provisioning;
    private final Clock clock;

    @Autowired
    public MySimbioSphereController(ProvisioningService provisioning) {
        this(provisioning, Clock.systemUTC());
    }

    MySimbioSphereController(ProvisioningService provisioning, Clock clock) {
        this.provisioning = provisioning;
        this.clock = clock;
    }

    @GetMapping("/cuenta/simbiosphere")
    public String panel(
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        requireVerifiedClientOwner(actor);
        model.addAttribute("provisioning", provisioning.ownedBy(actor.userId()).orElse(null));
        if (!model.containsAttribute("onboardingForm")) {
            model.addAttribute("onboardingForm", SimbioSphereOnboardingForm.empty());
        }
        model.addAttribute("simbiosphereTermsVersion", TERMS_VERSION);
        return "simbiosphere/account-panel";
    }

    @PostMapping("/cuenta/simbiosphere")
    public String start(
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            @Valid @ModelAttribute("onboardingForm") SimbioSphereOnboardingForm form,
            BindingResult errors,
            Model model) {
        requireVerifiedClientOwner(actor);
        if (errors.hasErrors()) {
            return panel(actor, model);
        }
        try {
            provisioning.begin(actor.userId(), form.toIntent(TERMS_VERSION, Instant.now(clock)));
            return "redirect:/cuenta/simbiosphere";
        } catch (IllegalArgumentException exception) {
            errors.reject("simbiosphere.onboarding.invalid");
            return panel(actor, model);
        }
    }

    private static void requireVerifiedClientOwner(AuthenticatedUser actor) {
        if (actor == null || actor.role() != Role.CLIENT) {
            throw new AccessDeniedException("Verified client owner required");
        }
    }
}
