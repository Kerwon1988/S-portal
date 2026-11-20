package com.simbio.core.messaging.web;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.simbio.core.messaging.application.ThreadAccessService;
import com.simbio.core.shared.security.AuthenticatedUser;

@Controller
public class ThreadAccessIssuanceController {

    private final ThreadAccessService access;

    public ThreadAccessIssuanceController(ThreadAccessService access) {
        this.access = access;
    }

    @PostMapping("/gestion/bandeja/{conversationId}/enlace-continuacion")
    public String issue(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            RedirectAttributes redirect) {
        ThreadAccessService.IssuedThreadAccess issued = access.issue(actor, conversationId);
        redirect.addFlashAttribute("threadAccessToken", issued.rawToken());
        return "redirect:/gestion/bandeja/" + conversationId;
    }
}
