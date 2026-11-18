package com.simbio.core.config.web;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.HexFormat;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.simbio.core.config.errortracking.ErrorInventoryPersistenceException;
import com.simbio.core.config.errortracking.ErrorInventoryService;
import com.simbio.core.config.errortracking.ErrorInventorySnapshot;
import com.simbio.core.config.errortracking.ErrorInventoryDisplaySanitizer;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.CurrentUser;
import com.simbio.core.shared.spi.AuditEvent;
import com.simbio.core.shared.spi.AuditEventRecorder;
import com.simbio.core.shared.spi.ErrorReporter.Source;

@Controller
@RequestMapping("/gestion/errores")
public class ErrorInventoryController {

    private static final int PAGE_SIZE = 20;
    private static final List<String> SEVERITIES = List.of("LOW", "MEDIUM", "HIGH");
    private static final List<String> STATUSES = List.of("OPEN", "RESOLVED", "IGNORED");

    private final ErrorInventoryService inventoryService;
    private final CurrentUser currentUser;
    private final AuditEventRecorder auditRecorder;

    public ErrorInventoryController(
            ErrorInventoryService inventoryService,
            CurrentUser currentUser,
            AuditEventRecorder auditRecorder) {
        this.inventoryService = inventoryService;
        this.currentUser = currentUser;
        this.auditRecorder = auditRecorder;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Source source,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            Model model) {
        requireAuthorizedUser();
        validatePage(page);
            String normalizedSeverity = normalizeFilter(severity, SEVERITIES);
            String normalizedStatus = normalizeFilter(status, STATUSES);

        ErrorInventorySnapshot inventory = inventoryService.snapshot();
        List<ErrorEntryView> filtered = inventory.errors().stream()
                .filter(record -> source == null || source == record.source())
                    .filter(record -> normalizedSeverity == null || normalizedSeverity.equals(record.severity()))
                    .filter(record -> normalizedStatus == null || normalizedStatus.equals(record.status()))
                .map(ErrorEntryView::from)
                .toList();
        int totalPages = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int currentPage = Math.min(page, totalPages - 1);
        int firstIndex = currentPage * PAGE_SIZE;
        int lastIndex = Math.min(firstIndex + PAGE_SIZE, filtered.size());

        model.addAttribute("available", inventory.available());
        model.addAttribute("storedEntries", inventory.errors().size());
        model.addAttribute("maxEntries", inventory.maxEntries());
        model.addAttribute("entries", filtered.subList(firstIndex, lastIndex));
        model.addAttribute("sources", Source.values());
        model.addAttribute("severities", SEVERITIES);
        model.addAttribute("statuses", STATUSES);
        model.addAttribute("source", source);
        model.addAttribute("severity", normalizedSeverity);
        model.addAttribute("status", normalizedStatus);
        model.addAttribute("page", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalEntries", filtered.size());
        return "config/errortracking/list";
    }

    @PostMapping("/{fingerprint}/estado")
    public String updateStatus(
            @PathVariable String fingerprint,
            @RequestParam String status,
            @RequestParam String resolutionNote,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Source source,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String filterStatus,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        AuthenticatedUser actor = requireAuthorizedUser();
        validatePage(page);
        severity = normalizeFilter(severity, SEVERITIES);
        filterStatus = normalizeFilter(filterStatus, STATUSES);
        if (resolutionNote.isBlank() || resolutionNote.length() > 2_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (!fingerprint.matches("[0-9a-f]{64}")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!List.of("RESOLVED", "IGNORED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        try {
            inventoryService.updateStatus(fingerprint, status, resolutionNote);
        } catch (NoSuchElementException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        } catch (ErrorInventoryPersistenceException exception) {
            redirectAttributes.addFlashAttribute("notice", "unavailable");
            addFilters(redirectAttributes, page, source, severity, filterStatus);
            return "redirect:/gestion/errores";
        }

        String correlationId = (String) request.getAttribute("correlationId");
        auditRecorder.record(new AuditEvent(
                actor.userId(),
                "ERROR_INVENTORY." + status,
                "ERROR_INVENTORY",
                UUID.nameUUIDFromBytes(HexFormat.of().parseHex(fingerprint)),
                correlationId));
        redirectAttributes.addFlashAttribute("notice", status.toLowerCase(java.util.Locale.ROOT));
        addFilters(redirectAttributes, page, source, severity, filterStatus);
        return "redirect:/gestion/errores#entry-" + fingerprint;
    }

    private AuthenticatedUser requireAuthorizedUser() {
        return currentUser.currentUser()
                .filter(user -> user.role() == Role.ADMIN
                        && user.hasPermission(Permission.ERROR_INVENTORY_MANAGE))
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Error inventory access denied"));
    }

    private static void validatePage(int page) {
        if (page < 0 || page > 100_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private static String normalizeFilter(String value, List<String> allowedValues) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!allowedValues.contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private static void addFilters(
            RedirectAttributes attributes,
            int page,
            Source source,
            String severity,
            String status) {
        attributes.addAttribute("page", page);
        if (source != null) {
            attributes.addAttribute("source", source);
        }
        if (severity != null) {
            attributes.addAttribute("severity", severity);
        }
        if (status != null) {
            attributes.addAttribute("status", status);
        }
    }

    public record ErrorEntryView(
            String fingerprint,
            Source source,
            String category,
            String exceptionType,
            String path,
            String severity,
            String status,
            String resolutionNote,
            String firstSeen,
            String lastSeen,
            long occurrences) {

        private static ErrorEntryView from(com.simbio.core.config.errortracking.ErrorRecord record) {
            return new ErrorEntryView(
                    record.fingerprint(),
                    record.source(),
                    safeDisplayToken(record.category()),
                    safeDisplayToken(record.exceptionType()),
                    ErrorInventoryDisplaySanitizer.path(record.path()),
                    record.severity(),
                    record.status(),
                    ErrorInventoryDisplaySanitizer.resolutionNote(record.resolutionNote()),
                    record.firstSeen(),
                    record.lastSeen(),
                    record.occurrences());
        }

        private static String safeDisplayToken(String value) {
            return value != null && value.matches("[A-Za-z0-9_.$:/-]{1,256}") ? value : "[redacted]";
        }

    }
}
