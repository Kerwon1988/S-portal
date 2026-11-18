package com.simbio.core.identity.web;

import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.simbio.core.identity.application.EmailVerificationService;
import com.simbio.core.identity.application.InvitationService;
import com.simbio.core.identity.application.PasswordRecoveryService;

@Controller
public class IdentityFlowController {

    private final EmailVerificationService verificationService;
    private final PasswordRecoveryService recoveryService;
    private final InvitationService invitationService;

    public IdentityFlowController(
            EmailVerificationService verificationService,
            PasswordRecoveryService recoveryService,
            InvitationService invitationService) {
        this.verificationService = verificationService;
        this.recoveryService = recoveryService;
        this.invitationService = invitationService;
    }

    @GetMapping("/verificar-email")
    public String verificationForm(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", safeToken(token));
        return "identity/verify-email";
    }

    @PostMapping("/verificar-email")
    public String verifyEmail(@RequestParam String token, Model model) {
        model.addAttribute("success", verificationService.verify(safeToken(token)));
        return "identity/verification-result";
    }

    @PostMapping("/verificar-email/reenviar")
    public String resendVerification(@RequestParam(required = false) String email, Model model) {
        verificationService.resend(email);
        model.addAttribute("generic", true);
        return "identity/verification-result";
    }

    @GetMapping("/recuperar")
    public String recoveryForm() {
        return "identity/recover";
    }

    @PostMapping("/recuperar")
    public String requestRecovery(@RequestParam(required = false) String email, Model model) {
        recoveryService.requestReset(email);
        model.addAttribute("generic", true);
        return "identity/recovery-result";
    }

    @GetMapping("/restablecer")
    public String resetForm(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", safeToken(token));
        return "identity/reset-password";
    }

    @PostMapping("/restablecer")
    public String resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            Model model) {
        model.addAttribute("success", recoveryService.resetPassword(safeToken(token), newPassword));
        return "identity/reset-result";
    }

    @GetMapping("/invitacion/aceptar")
    public String invitationForm(
            @RequestParam(required = false) String token,
            Locale requestLocale,
            Model model) {
        model.addAttribute("token", safeToken(token));
        model.addAttribute("locale", "en".equals(requestLocale.getLanguage()) ? "en" : "es");
        return "identity/accept-invitation";
    }

    @PostMapping("/invitacion/aceptar")
    public String acceptInvitation(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String displayName,
            @RequestParam(defaultValue = "false") boolean termsAccepted,
            @RequestParam(defaultValue = "false") boolean privacyAccepted,
            Locale requestLocale,
            Model model) {
        String acceptedLocale = "en".equals(requestLocale.getLanguage()) ? "en" : "es";
        model.addAttribute("success", invitationService.accept(safeToken(token), newPassword,
                displayName, acceptedLocale, termsAccepted, privacyAccepted));
        return "identity/invitation-result";
    }

    private static String safeToken(String token) {
        return token != null && token.length() <= 128 ? token : "";
    }
}
