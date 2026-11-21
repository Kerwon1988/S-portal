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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.simbiosphere.application.DemoProvisioningClient;
import com.simbio.core.simbiosphere.application.ProvisioningService;

@Controller
public class MySimbioSphereController {

    private static final String TERMS_VERSION = "SIM-TERMS-1";

    private final ProvisioningService provisioning;
    private final DemoProvisioningClient demoClient;
    private final Clock clock;

    @Autowired
    public MySimbioSphereController(ProvisioningService provisioning, DemoProvisioningClient demoClient) {
        this(provisioning, demoClient, Clock.systemUTC());
    }

    MySimbioSphereController(ProvisioningService provisioning, DemoProvisioningClient demoClient,
            Clock clock) {
        this.provisioning = provisioning;
        this.demoClient = demoClient;
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

    @GetMapping("/cuenta/simbiosphere/slug")
    public String slugAvailability(
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            @RequestParam("businessSlug") String slug,
            Model model) {
        requireVerifiedClientOwner(actor);
        try {
            model.addAttribute("slugAvailability", demoClient.checkSlug(slug));
        } catch (RuntimeException exception) {
            model.addAttribute("slugAvailabilityUnavailable", true);
        }
        return "simbiosphere/slug-availability :: result";
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

    @PostMapping("/cuenta/simbiosphere/correccion-slug")
    public String correctSlug(
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            @RequestParam("businessSlug") String businessSlug,
            RedirectAttributes redirect) {
        requireVerifiedClientOwner(actor);
        try {
            provisioning.correctActionRequired(actor.userId(), businessSlug);
            return "redirect:/cuenta/simbiosphere?corrected=true";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("simbiosphereCorrectionError", true);
            return "redirect:/cuenta/simbiosphere";
        }
    }

    @PostMapping("/cuenta/simbiosphere/activacion")
    public String activate(
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            RedirectAttributes redirect) {
        requireVerifiedClientOwner(actor);
        try {
            return "redirect:" + provisioning.activationLink(actor.userId(), demoClient);
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("simbiosphereActivationError", true);
            return "redirect:/cuenta/simbiosphere";
        }
    }

    private static void requireVerifiedClientOwner(AuthenticatedUser actor) {
        if (actor == null || actor.role() != Role.CLIENT) {
            throw new AccessDeniedException("Verified client owner required");
        }
    }
}
