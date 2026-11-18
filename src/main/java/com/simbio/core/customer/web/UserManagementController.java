package com.simbio.core.customer.web;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.customer.application.ManagedUserSummary;
import com.simbio.core.customer.application.UserManagementService;
import com.simbio.core.identity.api.UserDirectoryOrder;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@Controller
public class UserManagementController {

    private static final int PAGE_SIZE = 25;
    private static final int EXPORT_PAGE_SIZE = 100;
    private static final int EXPORT_LIMIT = 1000;

    private final UserManagementService users;
    private final AuditRecorder auditRecorder;

    public UserManagementController(UserManagementService users, AuditRecorder auditRecorder) {
        this.users = users;
        this.auditRecorder = auditRecorder;
    }

    @GetMapping("/gestion/clientes")
    public String clients(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(defaultValue = "CREATED_AT") UserDirectoryOrder.Field sort,
            @RequestParam(defaultValue = "DESC") UserDirectoryOrder.Direction direction,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        return list("CLIENT", "/gestion/clientes", q, status, locale, createdFrom, createdTo, sort, direction,
                page, actor, model);
    }

    @GetMapping("/gestion/trabajadores")
    public String employees(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(defaultValue = "CREATED_AT") UserDirectoryOrder.Field sort,
            @RequestParam(defaultValue = "DESC") UserDirectoryOrder.Direction direction,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        return list("STAFF", "/gestion/trabajadores", q, status, locale, createdFrom, createdTo, sort, direction,
                page, actor, model);
    }

    @GetMapping("/gestion/clientes/export.csv")
    public ResponseEntity<byte[]> exportClients(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(defaultValue = "CREATED_AT") UserDirectoryOrder.Field sort,
            @RequestParam(defaultValue = "DESC") UserDirectoryOrder.Direction direction,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        if (actor.role() != Role.ADMIN || !actor.hasPermission(Permission.USERS_MANAGE)
                || !actor.hasPermission(Permission.CLIENTS_EXPORT)) {
            throw new AccessDeniedException("Client export permission required");
        }
        java.time.Instant from = startOfDay(createdFrom);
        java.time.Instant before = startOfNextDay(createdTo);
        var firstPage = users.search(actor, "CLIENT", q, status, locale, from, before,
                new UserDirectoryOrder(sort, direction), new PageQuery(0, EXPORT_PAGE_SIZE));
        if (firstPage.totalElements() > EXPORT_LIMIT) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Narrow the filters to export at most 1000 clients");
        }
        var rows = new ArrayList<>(firstPage.content());
        int pageCount = (int) ((firstPage.totalElements() + EXPORT_PAGE_SIZE - 1) / EXPORT_PAGE_SIZE);
        for (int pageIndex = 1; pageIndex < pageCount; pageIndex++) {
            rows.addAll(users.search(actor, "CLIENT", q, status, locale, from, before,
                    new UserDirectoryOrder(sort, direction), new PageQuery(pageIndex, EXPORT_PAGE_SIZE)).content());
        }
        auditRecorder.record(new AuditRecord(actor.userId(), "CLIENT_DIRECTORY_EXPORTED",
                "CLIENT_DIRECTORY", null, null));
        String csv = "email,display_name,role,status,locale,created_at\r\n"
                + rows.stream().map(UserManagementController::csvRow)
                        .collect(Collectors.joining("\r\n"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"clients.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/gestion/usuarios/{userPublicId}")
    public String detail(
            @PathVariable UUID userPublicId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        ManagedUserSummary user = users.find(actor, userPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("user", user);
        model.addAttribute("canManageStaff", actor.hasPermission(Permission.STAFF_MANAGE));
        model.addAttribute("canViewAudit", actor.hasPermission(Permission.AUDIT_VIEW));
        return "customer/user-detail";
    }

    private String list(
            String role,
            String path,
            String search,
            String status,
            String locale,
            LocalDate createdFrom,
            LocalDate createdTo,
            UserDirectoryOrder.Field sort,
            UserDirectoryOrder.Direction direction,
            int page,
            AuthenticatedUser actor,
            Model model) {
        var usersPage = users.search(actor, role, search, status, locale, startOfDay(createdFrom),
                startOfNextDay(createdTo), new UserDirectoryOrder(sort, direction), new PageQuery(page, PAGE_SIZE));
        model.addAttribute("users", usersPage);
        model.addAttribute("searchPath", path);
        model.addAttribute("role", role);
        model.addAttribute("q", search == null ? "" : search);
        model.addAttribute("status", status == null ? "" : status);
        model.addAttribute("locale", locale == null ? "" : locale);
        model.addAttribute("createdFrom", createdFrom);
        model.addAttribute("createdTo", createdTo);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("hasNext", (long) (page + 1) * PAGE_SIZE < usersPage.totalElements());
        model.addAttribute("hasPrevious", page > 0);
        model.addAttribute("page", page);
        return "customer/user-list";
    }

    private static java.time.Instant startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private static java.time.Instant startOfNextDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private static String csvRow(ManagedUserSummary user) {
        var identity = user.identity();
        return String.join(",",
                csvCell(identity.email()),
                csvCell(identity.displayName()),
                csvCell(identity.role()),
                csvCell(identity.status()),
                csvCell(identity.locale()),
                csvCell(identity.createdAt().toString()));
    }

    static String csvCell(String value) {
        String cell = value == null ? "" : value;
        int firstNonWhitespace = 0;
        while (firstNonWhitespace < cell.length()
                && Character.isWhitespace(cell.charAt(firstNonWhitespace))) {
            firstNonWhitespace++;
        }
        if ((!cell.isEmpty() && (cell.charAt(0) == '\t' || cell.charAt(0) == '\r' || cell.charAt(0) == '\n'))
                || (firstNonWhitespace < cell.length()
                        && "=+-@".indexOf(cell.charAt(firstNonWhitespace)) >= 0)) {
            cell = "'" + cell;
        }
        return "\"" + cell.replace("\"", "\"\"") + "\"";
    }
}
