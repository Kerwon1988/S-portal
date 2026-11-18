package com.simbio.core.identity.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.simbio.core.identity.api.IdentityUserDirectory;
import com.simbio.core.identity.application.AdminUserActions;
import com.simbio.core.identity.security.PortalUserPrincipal;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@Controller
public class UserAdminActionsController {

    private final AdminUserActions actions;
    private final IdentityUserDirectory users;

    public UserAdminActionsController(AdminUserActions actions, IdentityUserDirectory users) {
        this.actions = actions;
        this.users = users;
    }

    @PostMapping("/gestion/usuarios/{userPublicId}/estado")
    public String setActive(
            @PathVariable UUID userPublicId,
            @RequestParam boolean active,
            @RequestParam boolean confirmed,
            @AuthenticationPrincipal PortalUserPrincipal principal,
            RedirectAttributes redirectAttributes) {
        AuthenticatedUser actor = requireUserManagement(principal);
        requireConfirmation(confirmed);
        requireVisibleUser(actor, userPublicId);
        return applyAction(() -> actions.setActive(actor, userPublicId, active), "status", userPublicId,
                redirectAttributes);
    }

    @PostMapping("/gestion/usuarios/{userPublicId}/desbloquear")
    public String unlock(
            @PathVariable UUID userPublicId,
            @RequestParam boolean confirmed,
            @AuthenticationPrincipal PortalUserPrincipal principal,
            RedirectAttributes redirectAttributes) {
        AuthenticatedUser actor = requireUserManagement(principal);
        requireConfirmation(confirmed);
        requireVisibleUser(actor, userPublicId);
        return applyAction(() -> actions.unlock(actor, userPublicId), "unlock", userPublicId, redirectAttributes);
    }

    @PostMapping("/gestion/usuarios/{userPublicId}/restablecer")
    public String requestPasswordReset(
            @PathVariable UUID userPublicId,
            @RequestParam boolean confirmed,
            @AuthenticationPrincipal PortalUserPrincipal principal,
            RedirectAttributes redirectAttributes) {
        AuthenticatedUser actor = requireUserManagement(principal);
        requireConfirmation(confirmed);
        requireVisibleUser(actor, userPublicId);
        return applyAction(() -> actions.sendPasswordReset(actor, userPublicId), "password", userPublicId,
                redirectAttributes);
    }

    @PostMapping("/gestion/usuarios/{userPublicId}/rol")
    public String changeRole(
            @PathVariable UUID userPublicId,
            @RequestParam String role,
            @RequestParam boolean confirmed,
            @AuthenticationPrincipal PortalUserPrincipal principal,
            RedirectAttributes redirectAttributes) {
        AuthenticatedUser actor = requireUserManagement(principal);
        requireConfirmation(confirmed);
        requireVisibleUser(actor, userPublicId);
        return applyAction(() -> actions.changeRole(actor, userPublicId, role), "role", userPublicId,
                redirectAttributes);
    }

    private String applyAction(
            Runnable action,
            String successMessage,
            UUID userPublicId,
            RedirectAttributes redirectAttributes) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("accountAction", successMessage);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("accountActionRejected", true);
        }
        return "redirect:/gestion/usuarios/" + userPublicId;
    }

    private void requireVisibleUser(AuthenticatedUser actor, UUID userPublicId) {
        if (users.find(actor, userPublicId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private static AuthenticatedUser requireUserManagement(PortalUserPrincipal principal) {
        AuthenticatedUser actor = principal.authenticatedUser();
        if (actor.role() != Role.ADMIN || !actor.hasPermission(Permission.USERS_MANAGE)) {
            throw new org.springframework.security.access.AccessDeniedException("User management permission required");
        }
        return actor;
    }

    private static void requireConfirmation(boolean confirmed) {
        if (!confirmed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirmation is required");
        }
    }
}
