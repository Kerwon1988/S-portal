package com.simbio.core.simbiosphere.web;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.simbiosphere.api.ProvisioningStatus;
import com.simbio.core.simbiosphere.application.AdminProvisioningService;

@Controller
@RequestMapping("/gestion/provisioning")
public class AdminProvisioningController {
    private final AdminProvisioningService provisioning;

    public AdminProvisioningController(AdminProvisioningService provisioning) {
        this.provisioning = provisioning;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            @RequestParam(required = false) ProvisioningStatus status, Model model) {
        model.addAttribute("provisionings", provisioning.search(actor, status));
        model.addAttribute("statuses", ProvisioningStatus.values());
        model.addAttribute("status", status);
        return "simbiosphere/admin-list";
    }

    @PostMapping("/{publicId}/retry")
    public String retry(@AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            @PathVariable UUID publicId,
            @RequestParam(defaultValue = "false") boolean confirmed,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            RedirectAttributes redirect) {
        try {
            provisioning.retry(actor, publicId, confirmed, correlationId);
            redirect.addFlashAttribute("provisioningRetrySucceeded", true);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("provisioningRetryRejected", true);
        }
        return "redirect:/gestion/provisioning";
    }
}
