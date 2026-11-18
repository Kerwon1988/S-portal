package com.simbio.core.customer.web;

import java.util.Locale;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.simbio.core.customer.application.CustomerInvitationService;
import com.simbio.core.shared.security.AuthenticatedUser;

@Controller
public class AdminCustomerInvitationController {

    private final CustomerInvitationService invitations;

    public AdminCustomerInvitationController(CustomerInvitationService invitations) {
        this.invitations = invitations;
    }

    @GetMapping("/gestion/clientes/nuevo")
    public String form(Model model) {
        model.addAttribute("invitationForm", new AdminCustomerInvitationForm("", "INDIVIDUAL", ""));
        return "customer/invite-client";
    }

    @PostMapping("/gestion/clientes/nuevo")
    public String invite(
            @Valid @ModelAttribute("invitationForm") AdminCustomerInvitationForm form,
            BindingResult errors,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Locale locale,
            Model model) {
        if (errors.hasErrors()) {
            return "customer/invite-client";
        }
        invitations.inviteClient(actor, form.email(),
                form.accountType(), form.accountName(), locale);
        model.addAttribute("submitted", true);
        model.addAttribute("invitationForm", new AdminCustomerInvitationForm("", "INDIVIDUAL", ""));
        return "customer/invite-client";
    }
}
