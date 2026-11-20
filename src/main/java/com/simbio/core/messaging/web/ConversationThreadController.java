package com.simbio.core.messaging.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.MessageSource;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.simbio.core.messaging.api.ConversationApi;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.messaging.application.ContactAttachmentInput;
import com.simbio.core.messaging.application.ContactAttachmentService;
import com.simbio.core.messaging.application.ContactSubmissionRejectedException;
import com.simbio.core.messaging.application.StaffReplyService;
import com.simbio.core.messaging.config.MessagingPollingProperties;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;

@Controller
public class ConversationThreadController {

    private final ConversationApi conversations;
    private final ContactAttachmentService contactAttachments;
    private final StaffReplyService replies;
    private final MessageSource messages;
    private final MessagingPollingProperties polling;

    public ConversationThreadController(
            ConversationApi conversations,
            ContactAttachmentService contactAttachments,
            StaffReplyService replies,
            MessageSource messages,
            MessagingPollingProperties polling) {
        this.conversations = conversations;
        this.contactAttachments = contactAttachments;
        this.replies = replies;
        this.messages = messages;
        this.polling = polling;
    }

    @GetMapping("/gestion/bandeja/{conversationPublicId}")
    public String thread(
            @PathVariable UUID conversationPublicId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        if (actor == null || (actor.role() != Role.EMPLOYEE && actor.role() != Role.ADMIN)) {
            throw new AccessDeniedException("Staff access required");
        }
        var conversation = conversations.findForActor(actor, conversationPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("conversation", conversation);
        if (conversation.contextType() == ConversationContextType.CONTACT) {
            model.addAttribute("contactAttachments", contactAttachments.list(actor, conversationPublicId));
        }
        model.addAttribute("messageForm", MessagePostForm.empty());
        model.addAttribute("pollIntervalSeconds", polling.interval().toSeconds());
        model.addAttribute("pollErrorBackoffSeconds", polling.errorBackoff().toSeconds());
        prepareReplyForm(model);
        return "messaging/thread";
    }

    @GetMapping("/gestion/bandeja/{conversationPublicId}/poll")
    public String poll(
            @PathVariable UUID conversationPublicId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        if (actor == null || (actor.role() != Role.EMPLOYEE && actor.role() != Role.ADMIN)) {
            throw new AccessDeniedException("Staff access required");
        }
        var conversation = conversations.findForActor(actor, conversationPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("conversation", conversation);
        model.addAttribute("pollIntervalSeconds", polling.interval().toSeconds());
        model.addAttribute("pollErrorBackoffSeconds", polling.errorBackoff().toSeconds());
        return "messaging/thread :: messages";
    }

    @PostMapping("/gestion/bandeja/{conversationPublicId}/responder")
    public String reply(
            @PathVariable UUID conversationPublicId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            @Valid @ModelAttribute("messageForm") MessagePostForm form,
            BindingResult bindingResult,
            @org.springframework.web.bind.annotation.RequestParam(value = "attachments", required = false)
                    List<MultipartFile> attachments,
            Locale locale,
            Model model,
            RedirectAttributes redirectAttributes) {
        requireStaff(actor);
        String templateBody = templateBody(form.templateCode(), locale);
        String body = form.selectedBody(templateBody);
        if (body == null || body.isBlank()) {
            bindingResult.rejectValue("body", "messaging.thread.body.required");
        }
        if (bindingResult.hasErrors()) {
            var conversation = conversations.findForActor(actor, conversationPublicId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            model.addAttribute("conversation", conversation);
            preparePolling(model);
            prepareReplyForm(model);
            return "messaging/thread";
        }

        try {
            replies.reply(actor, conversationPublicId, form.visibility(), body, uploads(attachments));
        } catch (IOException | ContactSubmissionRejectedException exception) {
            bindingResult.reject("messaging.thread.reply.invalid");
            var conversation = conversations.findForActor(actor, conversationPublicId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            model.addAttribute("conversation", conversation);
            preparePolling(model);
            prepareReplyForm(model);
            return "messaging/thread";
        }
        redirectAttributes.addFlashAttribute("messageSent", true);
        return "redirect:/gestion/bandeja/" + conversationPublicId;
    }

    private void preparePolling(Model model) {
        model.addAttribute("pollIntervalSeconds", polling.interval().toSeconds());
        model.addAttribute("pollErrorBackoffSeconds", polling.errorBackoff().toSeconds());
    }

    private static void prepareReplyForm(Model model) {
        model.addAttribute("messageVisibilities", List.of(MessageVisibility.PUBLIC, MessageVisibility.INTERNAL));
        model.addAttribute("replyTemplates", List.of("ACKNOWLEDGED", "NEED_INFORMATION", "RESOLVED"));
    }

    private String templateBody(String code, Locale locale) {
        if (code == null || code.isBlank()) {
            return "";
        }
        if (!List.of("ACKNOWLEDGED", "NEED_INFORMATION", "RESOLVED").contains(code)) {
            return "";
        }
        return messages.getMessage("messaging.thread.template." + code, null, locale);
    }

    private static List<ContactAttachmentInput> uploads(List<MultipartFile> attachments) throws IOException {
        List<ContactAttachmentInput> inputs = new ArrayList<>();
        if (attachments == null) {
            return inputs;
        }
        for (MultipartFile attachment : attachments) {
            if (attachment == null || attachment.isEmpty()) {
                continue;
            }
            long limit = 25L * 1024 * 1024;
            if (attachment.getSize() < 1 || attachment.getSize() > limit) {
                throw new ContactSubmissionRejectedException();
            }
            byte[] content = attachment.getInputStream().readNBytes((int) limit + 1);
            if (content.length != attachment.getSize()) {
                throw new ContactSubmissionRejectedException();
            }
            inputs.add(new ContactAttachmentInput(
                    attachment.getOriginalFilename(), attachment.getSize(), content, null));
        }
        return inputs;
    }

    private static void requireStaff(AuthenticatedUser actor) {
        if (actor == null || (actor.role() != Role.EMPLOYEE && actor.role() != Role.ADMIN)) {
            throw new AccessDeniedException("Staff access required");
        }
    }
}
