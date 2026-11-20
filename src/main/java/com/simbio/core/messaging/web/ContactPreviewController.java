package com.simbio.core.messaging.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.simbio.core.messaging.application.ContactAttachmentInput;
import com.simbio.core.messaging.application.ContactSubmissionCommand;
import com.simbio.core.messaging.application.ContactSubmissionRejectedException;
import com.simbio.core.messaging.application.ContactSubmissionService;
import com.simbio.core.messaging.application.ContactUnavailableException;
import com.simbio.core.messaging.config.MessagingContactProperties;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.AntiBotVerification;

@Controller
public class ContactPreviewController {

    private final ContactSubmissionService submissions;
    private final MessagingContactProperties properties;
    private final AntiBotVerification antiBot;
    private final ContactChannelLinkFactory channelLinks;

    public ContactPreviewController(ContactSubmissionService submissions,
            MessagingContactProperties properties, AntiBotVerification antiBot,
            ContactChannelLinkFactory channelLinks) {
        this.submissions = submissions;
        this.properties = properties;
        this.antiBot = antiBot;
        this.channelLinks = channelLinks;
    }

    @GetMapping("/{lang:es|en}/contacto")
    public String preview(@PathVariable String lang,
            @RequestParam(defaultValue = "false") boolean sent,
            Model model) {
        return prepareForm(model, lang, sent);
    }

    @PostMapping("/{lang:es|en}/contacto")
    public String submit(
            @PathVariable String lang,
            @Valid @ModelAttribute("contactForm") ContactForm form,
            BindingResult errors,
            @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments,
            @RequestParam(value = "audio", required = false) MultipartFile audio,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        if (errors.hasErrors()) {
            return prepareForm(model, lang, false);
        }
        try {
            List<ContactAttachmentInput> uploads = uploads(attachments, audio, form.audioDurationSeconds());
            UUID authenticatedClientId = actor != null && actor.role() == Role.CLIENT ? actor.userId() : null;
            submissions.submit(new ContactSubmissionCommand(
                    form.personName(), form.email(), form.phone(), form.reason(), form.message(), lang,
                    form.consentAccepted(), form.consentVersion(), form.antiBotPayload(),
                    form.websiteAddress(), form.humanConfirmation(), authenticatedClientId, uploads));
            return "redirect:/" + lang + "/contacto?sent=true";
        } catch (IOException | ContactSubmissionRejectedException exception) {
            errors.reject("messaging.contact.rejected");
            return prepareForm(model, lang, false);
        } catch (ContactUnavailableException exception) {
            return prepareForm(model, lang, false);
        }
    }

    private String prepareForm(Model model, String lang, boolean sent) {
        String locale = "en".equals(lang) ? "en" : "es";
        boolean available = properties.available(locale);
        model.addAttribute("contactAvailable", available);
        model.addAttribute("sent", sent);
        model.addAttribute("formLocale", locale);
        model.addAttribute("contactConsentText", properties.consentText(locale));
        model.addAttribute("contactChannels", channelLinks.forLocale(Locale.forLanguageTag(locale)));
        if (!model.containsAttribute("contactForm")) {
            model.addAttribute("contactForm", ContactForm.empty(properties.consentVersion()));
        }
        model.addAttribute("challenge", antiBot.issueChallenge());
        return "messaging/contact-preview";
    }

    private List<ContactAttachmentInput> uploads(List<MultipartFile> attachments,
            MultipartFile audio, Integer audioDurationSeconds) throws IOException {
        List<ContactAttachmentInput> result = new ArrayList<>();
        if (attachments != null) {
            for (MultipartFile attachment : attachments) {
                if (attachment != null && !attachment.isEmpty()) {
                    result.add(new ContactAttachmentInput(attachment.getOriginalFilename(),
                            attachment.getSize(), boundedRead(attachment), null));
                }
            }
        }
        if (audio != null && !audio.isEmpty()) {
            result.add(new ContactAttachmentInput(audio.getOriginalFilename(), audio.getSize(),
                    boundedRead(audio), audioDurationSeconds));
        }
        return result;
    }

    private static byte[] boundedRead(MultipartFile file) throws IOException {
        long limit = 25L * 1024 * 1024;
        if (file.getSize() < 1 || file.getSize() > limit) {
            throw new ContactSubmissionRejectedException();
        }
        byte[] content = file.getInputStream().readNBytes((int) limit + 1);
        if (content.length != file.getSize()) {
            throw new ContactSubmissionRejectedException();
        }
        return content;
    }
}
