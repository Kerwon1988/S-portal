package com.simbio.core.identity.web;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.simbio.core.identity.application.MfaEnrollment;
import com.simbio.core.identity.application.MfaService;
import com.simbio.core.identity.security.PortalUserPrincipal;

@Controller
public class MfaController {

    private static final String MFA_AUTHORITY = "MFA_VERIFIED";
    private final MfaService mfaService;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public MfaController(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    @GetMapping("/cuenta/seguridad/mfa")
    public String mfaChallenge(@AuthenticationPrincipal PortalUserPrincipal principal, Model model) {
        if (!mfaService.isEnrolled(principal.publicUserId())) {
            return "redirect:/cuenta/seguridad/mfa/enrolar";
        }
        return "identity/mfa-challenge";
    }

    @GetMapping("/cuenta/seguridad/mfa/enrolar")
    public String enrollmentForm(@AuthenticationPrincipal PortalUserPrincipal principal, Model model) {
        if (mfaService.isEnrolled(principal.publicUserId())) {
            return "redirect:/cuenta/seguridad/mfa";
        }
        model.addAttribute("enrollment", mfaService.beginEnrollment(principal.publicUserId()));
        return "identity/mfa-enroll";
    }

    @PostMapping("/cuenta/seguridad/mfa/enrolar")
    public String enroll(
            @RequestParam String code,
            @AuthenticationPrincipal PortalUserPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        List<String> recoveryCodes = mfaService.confirmEnrollment(principal.publicUserId(), code);
        if (recoveryCodes.isEmpty()) {
            model.addAttribute("enrollment", mfaService.beginEnrollment(principal.publicUserId()));
            model.addAttribute("invalidCode", true);
            return "identity/mfa-enroll";
        }
        completeMfa(request, response);
        model.addAttribute("recoveryCodes", recoveryCodes);
        return "identity/mfa-recovery-codes";
    }

    @PostMapping("/cuenta/seguridad/mfa/verificar")
    public String verify(
            @RequestParam String code,
            @AuthenticationPrincipal PortalUserPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        if (!mfaService.verifyLogin(principal.publicUserId(), code)) {
            model.addAttribute("invalidCode", true);
            return "identity/mfa-challenge";
        }
        completeMfa(request, response);
        Object destination = request.getSession(false) == null
                ? null : request.getSession(false).getAttribute("mfaContinue");
        if (request.getSession(false) != null) {
            request.getSession(false).removeAttribute("mfaContinue");
        }
        return "redirect:" + safeDestination(destination);
    }

    @PostMapping("/cuenta/seguridad/mfa/codigos-recuperacion")
    public String regenerateRecoveryCodes(
            @RequestParam String code,
            @AuthenticationPrincipal PortalUserPrincipal principal,
            Model model) {
        List<String> recoveryCodes = mfaService.regenerateRecoveryCodes(principal.publicUserId(), code);
        if (recoveryCodes.isEmpty()) {
            model.addAttribute("invalidCode", true);
            return "identity/mfa-challenge";
        }
        model.addAttribute("recoveryCodes", recoveryCodes);
        return "identity/mfa-recovery-codes";
    }

    private void completeMfa(HttpServletRequest request, HttpServletResponse response) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        List<GrantedAuthority> authorities = new ArrayList<>(current.getAuthorities());
        if (authorities.stream().noneMatch(authority -> MFA_AUTHORITY.equals(authority.getAuthority()))) {
            authorities.add(new SimpleGrantedAuthority(MFA_AUTHORITY));
        }
        Authentication verified = new UsernamePasswordAuthenticationToken(
                current.getPrincipal(), null, authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(verified);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        if (request.getSession(false) != null) {
            request.getSession(false).removeAttribute("mfaPending");
        }
    }

    private static String safeDestination(Object value) {
        if (value instanceof String path && path.startsWith("/")
                && !path.startsWith("//") && !path.startsWith("/\\")) {
            return path;
        }
        return "/es";
    }
}
