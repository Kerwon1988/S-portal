package com.simbio.core.staff.web;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Arrays;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.simbio.core.identity.api.IdentityUserDirectory;
import com.simbio.core.identity.api.InvitationIssuer;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.staff.application.StaffProfileService;
import com.simbio.core.staff.api.StaffDirectory;

@Controller
public class StaffManagementController {

    private final StaffProfileService profiles;
    private final StaffDirectory directory;
    private final IdentityUserDirectory identityUsers;
    private final InvitationIssuer invitations;

    public StaffManagementController(
            StaffProfileService profiles,
            StaffDirectory directory,
            IdentityUserDirectory identityUsers,
            InvitationIssuer invitations) {
        this.profiles = profiles;
        this.directory = directory;
        this.identityUsers = identityUsers;
        this.invitations = invitations;
    }

    @GetMapping("/gestion/trabajadores/invitacion")
    public String inviteForm(
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        requireStaffManagement(actor);
        model.addAttribute("form", new InviteForm(""));
        return "staff/invitation";
    }

    @PostMapping("/gestion/trabajadores/invitacion")
    public String invite(
            @Valid @ModelAttribute("form") InviteForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Locale locale,
            Model model) {
        requireStaffManagement(actor);
        if (bindingResult.hasErrors()) {
            return "staff/invitation";
        }
        boolean sent = invitations.invite(actor, form.email(), "EMPLOYEE", locale);
        model.addAttribute("form", new InviteForm(""));
        model.addAttribute("sent", sent);
        return "staff/invitation";
    }

    @GetMapping("/gestion/trabajadores/{userPublicId}")
    public String detail(
            @PathVariable UUID userPublicId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        requireStaffManagement(actor);
        var identity = identityUsers.find(actor, userPublicId)
                .filter(user -> "EMPLOYEE".equals(user.role()) || "ADMIN".equals(user.role()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var profile = directory.findByUserId(userPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("identity", identity);
        model.addAttribute("profile", profile);
        model.addAttribute("form", StaffForm.from(profile));
        return "staff/detail";
    }

    @PostMapping("/gestion/trabajadores/{userPublicId}")
    public String update(
            @PathVariable UUID userPublicId,
            @Valid @ModelAttribute("form") StaffForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        requireStaffManagement(actor);
        var identity = identityUsers.find(actor, userPublicId)
                .filter(user -> "EMPLOYEE".equals(user.role()) || "ADMIN".equals(user.role()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var profile = directory.findByUserId(userPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (bindingResult.hasErrors()) {
            model.addAttribute("identity", identity);
            model.addAttribute("profile", profile);
            return "staff/detail";
        }
        List<String> specialties = form.specialties().isBlank() ? List.of()
                : Arrays.stream(form.specialties().split("\\R")).toList();
        var updated = profiles.update(actor, userPublicId, form.jobTitle(), form.status(), specialties);
        model.addAttribute("identity", identity);
        model.addAttribute("profile", updated);
        model.addAttribute("form", StaffForm.from(updated));
        model.addAttribute("saved", true);
        return "staff/detail";
    }

    private static void requireStaffManagement(AuthenticatedUser actor) {
        if (actor == null || actor.role() != Role.ADMIN || !actor.hasPermission(Permission.STAFF_MANAGE)) {
            throw new AccessDeniedException("Worker management permission required");
        }
    }

    public record InviteForm(@NotBlank @Email @Size(max = 320) String email) {
    }

    public record StaffForm(
            @Size(max = 120) String jobTitle,
            @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE") String status,
            @Size(max = 1600) String specialties) {

        static StaffForm from(com.simbio.core.staff.api.StaffSummary profile) {
            return new StaffForm(profile.jobTitle(), profile.status(), String.join("\n", profile.specialties()));
        }
    }
}
