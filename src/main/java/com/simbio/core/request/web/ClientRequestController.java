package com.simbio.core.request.web;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import com.simbio.core.messaging.api.ConversationApi;
import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.request.api.QuoteDecision;
import com.simbio.core.request.api.RequestStatus;
import com.simbio.core.request.api.RequestSummary;
import com.simbio.core.request.api.RequestType;
import com.simbio.core.request.application.CreateRequestCommand;
import com.simbio.core.request.application.RequestConflictException;
import com.simbio.core.request.application.RequestDetail;
import com.simbio.core.request.application.QuoteService;
import com.simbio.core.request.application.RequestService;
import com.simbio.core.request.domain.RequestNotFoundException;
import com.simbio.core.simbiosphere.api.LinkedBusinessQuery;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.paging.PageQuery;

@Controller
@RequestMapping("/cuenta/solicitudes")
public class ClientRequestController {
    private final RequestService requests;
    private final ConversationApi conversations;
    private final QuoteService quotes;
    private final LinkedBusinessQuery linkedBusinesses;

    public ClientRequestController(RequestService requests, ConversationApi conversations,
            QuoteService quotes, LinkedBusinessQuery linkedBusinesses) {
        this.requests = requests;
        this.conversations = conversations;
        this.quotes = quotes;
        this.linkedBusinesses = linkedBusinesses;
    }

    @GetMapping
    public String list(@RequestParam(required = false) RequestType type,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        requireClient(actor);
        int safePage = Math.max(page, 0);
        var result = requests.listOwned(actor, type, status, reference,
                createdFrom == null ? null : createdFrom.atStartOfDay().toInstant(ZoneOffset.UTC),
                createdTo == null ? null : createdTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                PageQuery.of(safePage, 10));
        model.addAttribute("requests", result);
        model.addAttribute("types", RequestType.values());
        model.addAttribute("statuses", RequestStatus.values());
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        model.addAttribute("reference", reference);
        model.addAttribute("createdFrom", createdFrom);
        model.addAttribute("createdTo", createdTo);
        return "request/list";
    }

    @GetMapping("/nueva")
    public String newCustom(HttpServletRequest http,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor, Model model) {
        requireClient(actor);
        model.addAttribute("requestForm", formFromSession(http));
        model.addAttribute("suggestion", false);
        return "request/new";
    }

    @GetMapping("/sugerencia")
    public String newSuggestion(HttpServletRequest http,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor, Model model) {
        requireClient(actor);
        model.addAttribute("requestForm", formFromSession(http));
        model.addAttribute("suggestion", true);
        model.addAttribute("linkedBusinesses", linkedBusinesses.findOwnedBy(actor.userId()));
        return "request/new";
    }

    @PostMapping("/nueva")
    public String createCustom(@Valid @ModelAttribute("requestForm") RequestForm form,
            BindingResult errors, @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            HttpServletRequest http, Model model, SessionStatus sessionStatus) {
        requireClient(actor);
        http.getSession().setAttribute("requestDraft", form);
        if (errors.hasErrors()) {
            model.addAttribute("suggestion", false);
            return "request/new";
        }
        RequestSummary created = create(actor, RequestType.CUSTOM_DEVELOPMENT, form);
        http.getSession().removeAttribute("requestDraft");
        return "redirect:/cuenta/solicitudes/" + created.publicId() + "?created=true";
    }

    @PostMapping("/sugerencia")
    public String createSuggestion(@Valid @ModelAttribute("requestForm") RequestForm form,
            BindingResult errors, @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            HttpServletRequest http, Model model) {
        requireClient(actor);
        http.getSession().setAttribute("requestDraft", form);
        if (errors.hasErrors()) {
            model.addAttribute("suggestion", true);
            model.addAttribute("linkedBusinesses", linkedBusinesses.findOwnedBy(actor.userId()));
            return "request/new";
        }
        RequestSummary created = create(actor, RequestType.SIMBIOSPHERE_MODULE, form);
        http.getSession().removeAttribute("requestDraft");
        return "redirect:/cuenta/solicitudes/" + created.publicId() + "?created=true";
    }

    @GetMapping("/{publicId}")
    public String detail(@PathVariable UUID publicId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        RequestDetail detail;
        try {
            detail = requests.ownedDetail(actor, publicId);
        } catch (RequestNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("request", detail);
        model.addAttribute("quotes", quotes.forOwner(actor, publicId));
        model.addAttribute("conversation", conversations.findRequestForActor(actor, publicId)
                .map(conversation -> new com.simbio.core.messaging.api.ConversationView(
                        conversation.publicId(), conversation.contextType(), conversation.status(),
                        conversation.createdAt(), conversation.messages().stream()
                                .filter(message -> message.visibility() == MessageVisibility.PUBLIC)
                                .toList()))
                .orElse(null));
        return "request/detail";
    }

    @PostMapping("/{publicId}/mensaje")
    public String postMessage(@PathVariable UUID publicId, @RequestParam String body,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        try {
            requests.addInformation(actor, publicId, body);
        } catch (RequestNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/cuenta/solicitudes/" + publicId;
    }

    @PostMapping("/{publicId}/presupuesto/{version}/decision")
    public String decide(@PathVariable UUID publicId, @PathVariable int version,
            @RequestParam QuoteDecision decision,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        try {
            quotes.decide(actor, publicId, version, decision);
        } catch (RequestNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        return "redirect:/cuenta/solicitudes/" + publicId + "?quoteDecision=" + decision;
    }

    @PostMapping("/{publicId}/cancelar")
    public String cancel(@PathVariable UUID publicId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        try {
            requests.cancel(actor, publicId);
        } catch (RequestNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/cuenta/solicitudes/" + publicId + "?cancelled=true";
    }

    @PostMapping("/{publicId}/adjuntos")
    public String attach(@PathVariable UUID publicId, @RequestParam MultipartFile file,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) throws Exception {
        try {
            requests.attach(actor, publicId, file.getInputStream(), file.getOriginalFilename(), file.getSize());
        } catch (RequestNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/cuenta/solicitudes/" + publicId + "?attached=true";
    }

    @GetMapping("/{publicId}/adjuntos/{filePublicId}")
    public ResponseEntity<byte[]> download(@PathVariable UUID publicId, @PathVariable UUID filePublicId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) throws Exception {
        byte[] bytes;
        try (var stream = requests.downloadAttachment(actor, publicId, filePublicId)) {
            bytes = stream.readAllBytes();
        } catch (RequestNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("adjunto-" + filePublicId).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(bytes);
    }

    private RequestSummary create(AuthenticatedUser actor, RequestType type, RequestForm form) {
        return requests.create(actor, new CreateRequestCommand(type, form.category(), form.title(),
                form.description(), form.objective(), form.impact(), form.relatedModule(),
                form.linkedBusinessPublicId(), form.estimatedBudget(), form.targetDate()));
    }

    private static RequestForm formFromSession(HttpServletRequest request) {
        Object saved = request.getSession().getAttribute("requestDraft");
        return saved instanceof RequestForm form ? form :
                new RequestForm("WEB_APPLICATION", "", "", "", "", "", null, null, null);
    }

    private static void requireClient(AuthenticatedUser actor) {
        if (actor == null || actor.role() != Role.CLIENT) {
            throw new AccessDeniedException("Client session required");
        }
    }

}
