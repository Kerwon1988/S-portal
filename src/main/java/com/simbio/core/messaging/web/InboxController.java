package com.simbio.core.messaging.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.ConversationStatus;
import com.simbio.core.messaging.application.InboxAssignmentFilter;
import com.simbio.core.messaging.application.InboxFilters;
import com.simbio.core.messaging.application.InboxService;
import com.simbio.core.messaging.api.ConversationInboxState;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class InboxController {

    private static final int PAGE_SIZE = 20;

    private final InboxService inbox;

    public InboxController(InboxService inbox) {
        this.inbox = inbox;
    }

    @GetMapping("/gestion/bandeja")
    public String inbox(
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            Model model) {
        return render(actor, new InboxFilters(null, null, InboxAssignmentFilter.ALL, false, ""), 0, model, false);
    }

    @PostMapping("/gestion/bandeja/buscar")
    public String search(
            @RequestParam(required = false) ConversationContextType contextType,
            @RequestParam(required = false) ConversationStatus status,
            @RequestParam(defaultValue = "ACTIVE") ConversationInboxState inboxState,
            @RequestParam(defaultValue = "ALL") InboxAssignmentFilter assignment,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "") String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal(expression = "authenticatedUser()") AuthenticatedUser actor,
            HttpServletRequest request,
            Model model) {
        if (page < 0 || searchTerm.length() > InboxFilters.MAX_SEARCH_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        InboxFilters filters = new InboxFilters(contextType, status, inboxState, assignment, unreadOnly, searchTerm);
        return render(actor, filters, page, model, isHtmx(request));
    }

    private String render(
            AuthenticatedUser actor,
            InboxFilters filters,
            int page,
            Model model,
            boolean htmxRequest) {
        requireStaffInboxAccess(actor);
        var result = inbox.search(actor, filters, PageQuery.of(page, PAGE_SIZE));
        model.addAttribute("filters", filters);
        model.addAttribute("results", result);
        model.addAttribute("contextTypes", ConversationContextType.values());
        model.addAttribute("statuses", ConversationStatus.values());
        model.addAttribute("assignmentFilters", InboxAssignmentFilter.values());
        model.addAttribute("inboxStates", ConversationInboxState.values());
        model.addAttribute("canViewRequests", actor.hasPermission(Permission.REQUESTS_VIEW_ALL));
        model.addAttribute("hasPreviousPage", result.page() > 0);
        model.addAttribute("hasNextPage", (long) (result.page() + 1) * result.size() < result.totalElements());
        return htmxRequest ? "messaging/inbox :: results" : "messaging/inbox";
    }

    private static void requireStaffInboxAccess(AuthenticatedUser actor) {
        if (actor == null
                || (actor.role() != Role.EMPLOYEE && actor.role() != Role.ADMIN)
                || !actor.hasPermission(Permission.INBOX_MANAGE)) {
            throw new AccessDeniedException("Inbox management permission required");
        }
    }

    private static boolean isHtmx(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
    }
}
