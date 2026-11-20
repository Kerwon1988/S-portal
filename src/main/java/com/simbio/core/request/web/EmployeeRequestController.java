package com.simbio.core.request.web;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.simbio.core.messaging.api.MessageVisibility;
import com.simbio.core.request.api.QuoteStatus;
import com.simbio.core.request.api.RequestStatus;
import com.simbio.core.request.api.RequestType;
import com.simbio.core.request.application.EmployeeRequestService;
import com.simbio.core.request.application.QuoteService;
import com.simbio.core.request.application.RequestConflictException;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.staff.api.StaffDirectory;

@Controller
@RequestMapping("/gestion")
public class EmployeeRequestController {
    private final EmployeeRequestService requests;
    private final QuoteService quotes;
    private final StaffDirectory staff;

    public EmployeeRequestController(EmployeeRequestService requests, QuoteService quotes, StaffDirectory staff) {
        this.requests = requests;
        this.quotes = quotes;
        this.staff = staff;
    }

    @GetMapping
    public String dashboard(@AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        model.addAttribute("dashboard", requests.dashboard(actor));
        return "request/employee-dashboard";
    }

    @GetMapping("/inicio")
    public String dashboardFragment(@AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        model.addAttribute("dashboard", requests.dashboard(actor));
        return "request/employee-dashboard :: cards";
    }

    @GetMapping("/solicitudes")
    public String list(@RequestParam(required = false) RequestType type,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) BigDecimal amountFrom,
            @RequestParam(required = false) BigDecimal amountTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        var result = requests.search(actor, type, status, category, assignedTo,
                from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC),
                to == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                amountFrom, amountTo, search, PageQuery.of(Math.max(page, 0), 20));
        populateList(model, result.content(), result.totalElements());
        model.addAttribute("requests", result);
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        model.addAttribute("category", category);
        model.addAttribute("assignedTo", assignedTo);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("amountFrom", amountFrom);
        model.addAttribute("amountTo", amountTo);
        model.addAttribute("search", search);
        model.addAttribute("staff", staff.search(null, "ACTIVE", PageQuery.of(0, 100)).content());
        return "request/employee-list";
    }

    @GetMapping("/solicitudes/resultados")
    public String listFragment(@RequestParam(required = false) RequestType type,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) BigDecimal amountFrom,
            @RequestParam(required = false) BigDecimal amountTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor, Model model) {
        var result = requests.search(actor, type, status, category, assignedTo,
                from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC),
                to == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                amountFrom, amountTo, search, PageQuery.of(Math.max(page, 0), 20));
        model.addAttribute("requests", result);
        return "request/employee-list :: results";
    }

    @GetMapping("/solicitudes/export.csv")
    public ResponseEntity<byte[]> csv(
            @RequestParam(required = false) RequestType type,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID assignedTo,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        var result = requests.export(actor, type, status, category, assignedTo);
        StringBuilder csv = new StringBuilder("reference,type,status,category,title,priority,estimate_hours,created_at\r\n");
        result.forEach(item -> csv.append(cell(item.reference())).append(',')
                .append(cell(item.type().name())).append(',').append(cell(item.status().name())).append(',')
                .append(cell(item.category())).append(',').append(cell(maskPii(item.title()))).append(',')
                .append(cell(item.priority())).append(',').append(cell(item.estimateHours())).append(',')
                .append(cell(item.createdAt())).append("\r\n"));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"requests.csv\"").contentType(new MediaType("text", "csv",
                StandardCharsets.UTF_8)).body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/solicitudes/{publicId}")
    public String detail(@PathVariable UUID publicId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor, Model model) {
        model.addAttribute("requestDetail", requests.detail(actor, publicId));
        model.addAttribute("assignments", requests.assignments(actor, publicId));
        model.addAttribute("quotes", quotes.forEmployee(actor, publicId));
        model.addAttribute("conversation", requests.conversation(actor, publicId));
        model.addAttribute("staff", staff.search(null, "ACTIVE", PageQuery.of(0, 100)).content());
        model.addAttribute("statuses", RequestStatus.values());
        model.addAttribute("quoteStatuses", QuoteStatus.values());
        model.addAttribute("canAssign", actor.role() == com.simbio.core.shared.security.Role.ADMIN
                || actor.hasPermission(com.simbio.core.shared.security.Permission.REQUESTS_ASSIGN));
        model.addAttribute("canQuote", actor.role() == com.simbio.core.shared.security.Role.ADMIN
                || actor.hasPermission(com.simbio.core.shared.security.Permission.QUOTES_MANAGE));
        model.addAttribute("canInternal", actor.role() == com.simbio.core.shared.security.Role.ADMIN
                || actor.hasPermission(com.simbio.core.shared.security.Permission.INBOX_MANAGE));
        return "request/employee-detail";
    }

    @GetMapping("/solicitudes/{publicId}/adjuntos/{filePublicId}")
    public ResponseEntity<byte[]> attachment(@PathVariable UUID publicId, @PathVariable UUID filePublicId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) throws Exception {
        byte[] bytes;
        try (var stream = requests.downloadAttachment(actor, publicId, filePublicId)) {
            bytes = stream.readAllBytes();
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"adjunto-" + filePublicId + "\"")
                .header("X-Content-Type-Options", "nosniff").body(bytes);
    }

    @GetMapping("/solicitudes/{publicId}/conversacion")
    public String conversation(@PathVariable UUID publicId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor, Model model) {
        model.addAttribute("conversation", requests.conversation(actor, publicId));
        return "request/employee-detail :: conversation";
    }

    @GetMapping("/solicitudes/{publicId}/presupuesto")
    public String quote(@PathVariable UUID publicId,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor, Model model) {
        model.addAttribute("quotes", quotes.forEmployee(actor, publicId));
        return "request/employee-detail :: quote-panel";
    }

    @PostMapping("/solicitudes/{publicId}/asignacion")
    public String assign(@PathVariable UUID publicId, @RequestParam UUID responsible,
            @RequestParam(required = false) List<UUID> collaborators,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        requests.assign(actor, publicId, responsible, collaborators);
        return "redirect:/gestion/solicitudes/" + publicId;
    }

    @PostMapping("/solicitudes/{publicId}/operacion")
    public String update(@PathVariable UUID publicId, @RequestParam(required = false) RequestStatus target,
            @RequestParam(defaultValue = "NORMAL") String priority,
            @RequestParam(required = false) BigDecimal estimateHours,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String originalReference,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        requests.update(actor, publicId, target, priority, estimateHours, reason, originalReference);
        return "redirect:/gestion/solicitudes/" + publicId;
    }

    @PostMapping("/solicitudes/{publicId}/presupuesto")
    public String createQuote(@PathVariable UUID publicId, @RequestParam BigDecimal netAmount,
            @RequestParam(defaultValue = "EUR") String currency, @RequestParam BigDecimal taxRate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validUntil,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        quotes.createVersion(actor, publicId, netAmount, currency, taxRate, validUntil);
        return "redirect:/gestion/solicitudes/" + publicId + "#quote";
    }

    @PostMapping("/solicitudes/{publicId}/presupuesto/{version}/enviar")
    public String sendQuote(@PathVariable UUID publicId, @PathVariable int version,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        quotes.send(actor, publicId, version);
        return "redirect:/gestion/solicitudes/" + publicId + "#quote";
    }

    @PostMapping("/solicitudes/{publicId}/mensajes")
    public String postMessage(@PathVariable UUID publicId, @RequestParam MessageVisibility visibility,
            @RequestParam String body,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor) {
        requests.postMessage(actor, publicId, visibility, body);
        return "redirect:/gestion/solicitudes/" + publicId + "#conversation";
    }

    @GetMapping("/carga")
    public String workload(@RequestParam(required = false) String staffFilter,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor, Model model) {
        model.addAttribute("workload", requests.workload(actor, staffFilter));
        return "request/employee-workload";
    }

    private static void populateList(Model model, List<?> content, long total) {
        model.addAttribute("types", RequestType.values());
        model.addAttribute("statuses", RequestStatus.values());
        model.addAttribute("resultCount", total);
    }

    static String maskPii(String value) {
        return value == null ? "" : value.replaceAll("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", "[REDACTED]");
    }

    static String cell(Object value) {
        String text = value == null ? "" : value.toString();
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) text = "'" + text;
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
