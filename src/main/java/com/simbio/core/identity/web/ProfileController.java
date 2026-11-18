package com.simbio.core.identity.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

import com.simbio.core.identity.application.ProfileService;
import com.simbio.core.identity.application.PasswordRecoveryService;
import com.simbio.core.identity.security.PortalUserPrincipal;

@Controller
public class ProfileController {

    private final ProfileService profileService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final LocaleResolver localeResolver;

    public ProfileController(
            ProfileService profileService,
            PasswordRecoveryService passwordRecoveryService,
            LocaleResolver localeResolver) {
        this.profileService = profileService;
        this.passwordRecoveryService = passwordRecoveryService;
        this.localeResolver = localeResolver;
    }

    @GetMapping("/cuenta/perfil")
    public String profile(@AuthenticationPrincipal PortalUserPrincipal principal, Model model) {
        var profile = profileService.get(principal.publicUserId());
        model.addAttribute("profile", profile);
        model.addAttribute("form", ProfileForm.from(profile));
        return "identity/profile";
    }

    @PostMapping("/cuenta/perfil")
    public String updateProfile(
            @Valid @ModelAttribute("form") ProfileForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal PortalUserPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("profile", profileService.get(principal.publicUserId()));
            return "identity/profile";
        }
        profileService.update(principal.publicUserId(), form.displayName(), form.locale(), form.theme());
        localeResolver.setLocale(request, response, java.util.Locale.forLanguageTag(form.locale()));
        model.addAttribute("profile", profileService.get(principal.publicUserId()));
        model.addAttribute("saved", true);
        model.addAttribute("form", ProfileForm.from(profileService.get(principal.publicUserId())));
        return "identity/profile";
    }

    @GetMapping("/cuenta/seguridad/password")
    public String passwordForm() {
        return "identity/change-password";
    }

    @PostMapping("/cuenta/seguridad/password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @AuthenticationPrincipal PortalUserPrincipal principal,
            Model model) {
        model.addAttribute("changed", passwordRecoveryService.changePassword(
                principal.publicUserId(), currentPassword, newPassword));
        return "identity/change-password-result";
    }

    public record ProfileForm(
            @NotBlank @Size(max = 120) String displayName,
            @NotNull @Pattern(regexp = "es|en") String locale,
            @NotNull @Pattern(regexp = "light|dark") String theme) {

        static ProfileForm from(com.simbio.core.identity.application.ProfileData profile) {
            return new ProfileForm(profile.displayName() == null ? "" : profile.displayName(),
                    profile.locale(), profile.theme() == null ? "light" : profile.theme());
        }
    }
}
