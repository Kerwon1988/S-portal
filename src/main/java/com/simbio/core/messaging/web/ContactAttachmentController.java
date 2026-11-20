package com.simbio.core.messaging.web;

import java.io.IOException;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.simbio.core.messaging.application.ContactAttachmentService;
import com.simbio.core.messaging.application.ContactAttachmentDownload;
import com.simbio.core.shared.security.AuthenticatedUser;

@Controller
public class ContactAttachmentController {

    private final ContactAttachmentService attachments;

    public ContactAttachmentController(ContactAttachmentService attachments) {
        this.attachments = attachments;
    }

    @GetMapping({
            "/gestion/bandeja/{conversationPublicId}/adjuntos/{filePublicId}",
            "/cuenta/mensajes/{conversationPublicId}/adjuntos/{filePublicId}"})
    public ResponseEntity<InputStreamResource> download(
            @PathVariable UUID conversationPublicId,
            @PathVariable UUID filePublicId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor)
            throws IOException {
        ContactAttachmentDownload download;
        try {
            download = attachments.download(actor, conversationPublicId, filePublicId);
        } catch (com.simbio.core.messaging.application.ContactAttachmentNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=contact-attachment-" + filePublicId + ".bin")
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(download.content()));
    }
}
