package com.simbio.core.messaging.web;

import java.util.UUID;

import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.simbio.core.messaging.application.InboxActionService;
import com.simbio.core.shared.security.AuthenticatedUser;

@Controller
public class InboxActionController {

    private final InboxActionService actions;

    public InboxActionController(InboxActionService actions) {
        this.actions = actions;
    }

    @PostMapping("/gestion/bandeja/{conversationId}/asignar")
    public String assign(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) UUID assigneeId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            RedirectAttributes redirect) {
        actions.assign(actor, conversationId, assigneeId);
        redirect.addFlashAttribute("inboxActionCompleted", true);
        return thread(conversationId);
    }

    @PostMapping("/gestion/bandeja/{conversationId}/etiquetar")
    public String label(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "") @Size(max = 255) String labels,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        actions.updateLabels(actor, conversationId, labels);
        return thread(conversationId);
    }

    @PostMapping("/gestion/bandeja/{conversationId}/leido")
    public String markRead(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        actions.markRead(actor, conversationId);
        return thread(conversationId);
    }

    @PostMapping("/gestion/bandeja/{conversationId}/no-leido")
    public String markUnread(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        actions.markUnread(actor, conversationId);
        return thread(conversationId);
    }

    @PostMapping("/gestion/bandeja/{conversationId}/archivar")
    public String archive(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        actions.archive(actor, conversationId);
        return "redirect:/gestion/bandeja";
    }

    @PostMapping("/gestion/bandeja/{conversationId}/papelera")
    public String trash(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        actions.moveToTrash(actor, conversationId);
        return "redirect:/gestion/bandeja";
    }

    @PostMapping("/gestion/bandeja/{conversationId}/restaurar")
    public String restore(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        actions.restore(actor, conversationId);
        return "redirect:/gestion/bandeja";
    }

    @PostMapping("/gestion/bandeja/{conversationId}/purgar")
    public String purge(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        actions.purge(actor, conversationId);
        return "redirect:/gestion/bandeja";
    }

    private static String thread(UUID conversationId) {
        return "redirect:/gestion/bandeja/" + conversationId;
    }
}
