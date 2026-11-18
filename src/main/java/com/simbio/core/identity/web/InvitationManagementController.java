package com.simbio.core.identity.web;

import java.util.Locale;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.simbio.core.identity.application.InvitationService;
import com.simbio.core.identity.security.PortalUserPrincipal;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;

@Controller
public class InvitationManagementController {

    private final InvitationService invitationService;

    public InvitationManagementController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping("/gestion/identidad/invitaciones")
    public String inviteForm() {
        return "identity/create-invitation";
    }

    @PostMapping("/gestion/identidad/invitaciones")
    public String invite(
            @RequestParam String email,
            @AuthenticationPrincipal PortalUserPrincipal principal,
            Locale locale,
            Model model) {
        AuthenticatedUser actor = principal.authenticatedUser();
        if (actor.role() != com.simbio.core.shared.security.Role.ADMIN
                || !actor.hasPermission(Permission.USERS_MANAGE)) {
            throw new org.springframework.security.access.AccessDeniedException("Administrator permission required");
        }
        model.addAttribute("sent", invitationService.invite(actor, email, "CLIENT", locale));
        model.addAttribute("email", "");
        return "identity/create-invitation";
    }
}
