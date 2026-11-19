package com.simbio.core.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.simbio.core.identity.application.InvitationService;
import com.simbio.core.identity.application.PendingInvitationSummary;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.paging.PageResult;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

class InvitationManagementControllerTest {

    private final InvitationService invitations = mock(InvitationService.class);
    private final InvitationManagementController controller = new InvitationManagementController(invitations);

    @Test
    void clientInvitationListUsesItsDedicatedScopeAndView() {
        AuthenticatedUser actor = admin(Permission.USERS_MANAGE);
        PendingInvitationSummary invitation = new PendingInvitationSummary(
                UUID.randomUUID(), "s***@example.test", "CLIENT",
                Instant.parse("2026-11-18T10:00:00Z"), Instant.parse("2026-11-19T10:00:00Z"), false);
        PageResult<PendingInvitationSummary> page = new PageResult<>(List.of(invitation), 0, 25, 1);
        when(invitations.pendingInvitations(actor, "CLIENT", new PageQuery(0, 25))).thenReturn(page);
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.pendingClientInvitations(actor, 0, model)).isEqualTo("identity/pending-invitations");
        assertThat(model.get("scope")).isEqualTo("clientes");
        assertThat(model.get("invitations")).isEqualTo(page);
        assertThat(model.get("hasPrevious")).isEqualTo(false);
        assertThat(model.get("hasNext")).isEqualTo(false);
        verify(invitations).pendingInvitations(actor, "CLIENT", new PageQuery(0, 25));
    }

    @Test
    void employeeInvitationResendReturnsToItsScopedListWithGenericResult() {
        AuthenticatedUser actor = admin(Permission.STAFF_MANAGE);
        UUID invitationId = UUID.randomUUID();
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);
        when(invitations.resendInvitation(actor, invitationId, "EMPLOYEE", Locale.ENGLISH)).thenReturn(true);

        assertThat(controller.resendEmployeeInvitation(
                invitationId, actor, Locale.ENGLISH, redirectAttributes))
                .isEqualTo("redirect:/gestion/identidad/invitaciones/pendientes/trabajadores");
        verify(redirectAttributes).addFlashAttribute("resendResult", true);
        verify(invitations).resendInvitation(actor, invitationId, "EMPLOYEE", Locale.ENGLISH);
    }

    private static AuthenticatedUser admin(Permission permission) {
        return new AuthenticatedUser(UUID.randomUUID(), Role.ADMIN, java.util.Set.of(permission));
    }
}
