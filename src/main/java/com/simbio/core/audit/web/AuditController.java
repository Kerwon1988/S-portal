package com.simbio.core.audit.web;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.simbio.core.audit.application.AuditFilter;
import com.simbio.core.audit.application.AuditPage;
import com.simbio.core.audit.application.AuditRecorderService;

@Controller
@RequestMapping("/gestion/auditoria")
public class AuditController {

    private static final Pattern ACTION = Pattern.compile("[A-Z0-9][A-Z0-9_.-]{0,63}");
    private static final AuditPage EMPTY_PAGE = new AuditPage(List.of(), 0, 0, 0);
    private final AuditRecorderService auditService;

    public AuditController(AuditRecorderService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String actionCode,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            Model model) {
        if (page < 0 || page > 100_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        UUID actor;
        String action;
        Instant fromInclusive;
        Instant toExclusive;
        String parsedEntityType;
        UUID parsedEntityId;
        try {
            actor = parseUuid(actorId);
            action = parseAction(actionCode);
            LocalDate from = parseDate(fromDate);
            LocalDate to = parseDate(toDate);
            if (from != null && to != null && to.isBefore(from)) {
                throw new IllegalArgumentException("Invalid range");
            }
            if ((entityId == null) != (entityType == null)) {
                throw new IllegalArgumentException("Entity type and ID must be provided together");
            }
            parsedEntityType = entityType == null ? null : parseAction(entityType);
            parsedEntityId = entityId == null ? null : UUID.fromString(entityId);
            fromInclusive = from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC);
            toExclusive = to == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (IllegalArgumentException | DateTimeException exception) {
            model.addAttribute("auditPage", EMPTY_PAGE);
            model.addAttribute("entityHistory", null);
            model.addAttribute("invalidFilter", true);
            model.addAttribute("entityType", entityType);
            model.addAttribute("entityPublicId", entityId);
            model.addAttribute("actorId", actorId);
            model.addAttribute("actionCode", actionCode);
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);
            return "audit/list";
        }

        AuditFilter filter = new AuditFilter(actor, action, fromInclusive, toExclusive);
        model.addAttribute("auditPage", auditService.page(filter, page, 25));
        model.addAttribute("entityHistory", parsedEntityId == null
                ? null : auditService.forEntity(parsedEntityType, parsedEntityId));
        model.addAttribute("entityType", parsedEntityType);
        model.addAttribute("entityPublicId", parsedEntityId);
        model.addAttribute("actorId", actorId);
        model.addAttribute("actionCode", actionCode);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        return "audit/list";
    }

    @GetMapping("/usuarios/{userPublicId}/panel")
    public String userPanel(
            @PathVariable UUID userPublicId,
            Model model) {
        model.addAttribute("entityPublicId", userPublicId);
        model.addAttribute("entityHistory", auditService.forEntity("USER", userPublicId));
        return "audit/user-panel :: panel";
    }

    private static UUID parseUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private static String parseAction(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!ACTION.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid action code");
        }
        return value;
    }

    private static LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}
