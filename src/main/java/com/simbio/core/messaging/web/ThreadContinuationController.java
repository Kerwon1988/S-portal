package com.simbio.core.messaging.web;

import java.io.IOException;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import com.simbio.core.messaging.application.ConversationNotFoundException;
import com.simbio.core.messaging.application.ThreadAccessService;

@Controller
public class ThreadContinuationController {

    private final ThreadAccessService access;

    public ThreadContinuationController(ThreadAccessService access) {
        this.access = access;
    }

    @GetMapping("/continuar/{token}")
    public String thread(@PathVariable String token, Model model) {
        try {
            model.addAttribute("conversation", access.read(token));
            model.addAttribute("continuationForm", MessagePostForm.empty());
            model.addAttribute("continuationToken", token);
            return "messaging/continuation";
        } catch (ConversationNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/continuar/{token}/adjuntos/{filePublicId}")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable String token,
            @PathVariable UUID filePublicId) throws IOException {
        try {
            var download = access.download(token, filePublicId);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(download.contentType()))
                    .contentLength(download.sizeBytes())
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=conversation-attachment-" + filePublicId + ".bin")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(new InputStreamResource(download.content()));
        } catch (ConversationNotFoundException | com.simbio.core.messaging.application.ContactAttachmentNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/continuar/{token}")
    public String reply(
            @PathVariable String token,
            @Valid @ModelAttribute("continuationForm") MessagePostForm form,
            BindingResult errors,
            Model model) {
        if (form.visibility() != com.simbio.core.messaging.api.MessageVisibility.PUBLIC) {
            errors.rejectValue("visibility", "messaging.continuation.visibility");
        }
        if (errors.hasErrors()) {
            try {
                model.addAttribute("conversation", access.read(token));
                model.addAttribute("continuationToken", token);
                return "messaging/continuation";
            } catch (ConversationNotFoundException exception) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        try {
            access.reply(token, form.body());
            return "messaging/continuation-result";
        } catch (ConversationNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
