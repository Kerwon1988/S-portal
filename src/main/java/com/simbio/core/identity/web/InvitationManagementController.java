package com.simbio.core.identity.web;

import java.util.Locale;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.simbio.core.identity.application.InvitationService;
import com.simbio.core.identity.security.PortalUserPrincipal;
import com.simbio.core.shared.paging.PageQuery;
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

    @GetMapping("/gestion/identidad/invitaciones/pendientes/clientes")
    public String pendingClientInvitations(
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        return pendingInvitations(actor, "CLIENT", "clientes", page, model);
    }

    @GetMapping("/gestion/identidad/invitaciones/pendientes/trabajadores")
    public String pendingEmployeeInvitations(
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        return pendingInvitations(actor, "EMPLOYEE", "trabajadores", page, model);
    }

    @PostMapping("/gestion/identidad/invitaciones/pendientes/clientes/{invitationId}/reenviar")
    public String resendClientInvitation(
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Locale locale,
            RedirectAttributes redirectAttributes) {
        boolean resent = invitationService.resendInvitation(actor, invitationId, "CLIENT", locale);
        redirectAttributes.addFlashAttribute("resendResult", resent);
        return "redirect:/gestion/identidad/invitaciones/pendientes/clientes";
    }

    @PostMapping("/gestion/identidad/invitaciones/pendientes/trabajadores/{invitationId}/reenviar")
    public String resendEmployeeInvitation(
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Locale locale,
            RedirectAttributes redirectAttributes) {
        boolean resent = invitationService.resendInvitation(actor, invitationId, "EMPLOYEE", locale);
        redirectAttributes.addFlashAttribute("resendResult", resent);
        return "redirect:/gestion/identidad/invitaciones/pendientes/trabajadores";
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

    private String pendingInvitations(
            AuthenticatedUser actor,
            String roleCode,
            String scope,
            int page,
            Model model) {
        var invitations = invitationService.pendingInvitations(actor, roleCode, new PageQuery(page, 25));
        model.addAttribute("invitations", invitations);
        model.addAttribute("scope", scope);
        model.addAttribute("hasPrevious", invitations.page() > 0);
        model.addAttribute("hasNext",
                ((long) invitations.page() + 1) * invitations.size() < invitations.totalElements());
        return "identity/pending-invitations";
    }
}
