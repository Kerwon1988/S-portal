package com.simbio.core.customer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.customer.application.ManagedUserSummary;
import com.simbio.core.customer.application.UserManagementService;
import com.simbio.core.identity.api.IdentityUserSummary;
import com.simbio.core.identity.api.UserDirectoryOrder;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.paging.PageResult;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

class UserManagementControllerTest {

    private final UserManagementService users = org.mockito.Mockito.mock(UserManagementService.class);
    private final AuditRecorder auditRecorder = org.mockito.Mockito.mock(AuditRecorder.class);
    private final UserManagementController controller = new UserManagementController(users, auditRecorder);

    @Test
    void neutralizesSpreadsheetFormulasAndQuotesCsvValues() {
        assertThat(UserManagementController.csvCell("=HYPERLINK(\"https://example.test\")"))
                .isEqualTo("\"'=HYPERLINK(\"\"https://example.test\"\")\"");
        assertThat(UserManagementController.csvCell("  +SUM(1,2)"))
                .isEqualTo("\"'  +SUM(1,2)\"");
        assertThat(UserManagementController.csvCell("Simbio"))
                .isEqualTo("\"Simbio\"");
    }

    @Test
    void neutralizesFormulaAfterLeadingControlWhitespace() {
        assertThat(UserManagementController.csvCell("\t@SUM(1,2)"))
                .isEqualTo("\"'\t@SUM(1,2)\"");
    }

    @Test
    void clientCsvExportRequiresBothAdminAndExportPermissions() {
        AuthenticatedUser employee = new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE,
                Set.of(Permission.USERS_MANAGE, Permission.CLIENTS_EXPORT));
        AuthenticatedUser adminWithoutExport = new AuthenticatedUser(UUID.randomUUID(), Role.ADMIN,
                Set.of(Permission.USERS_MANAGE));

        assertThatThrownBy(() -> export(employee))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> export(adminWithoutExport))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(users, auditRecorder);
    }

    @Test
    void clientCsvExportRejectsMoreThanOneThousandRowsBeforeAuditing() {
        AuthenticatedUser admin = authorizedAdmin();
        when(users.search(admin, "CLIENT", null, null, null, null, null, UserDirectoryOrder.DEFAULT,
                new PageQuery(0, 100)))
                .thenReturn(new PageResult<>(List.of(client("client@example.test", "Client")), 0, 100, 1001));

        assertThatThrownBy(() -> export(admin))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);

        verify(users).search(admin, "CLIENT", null, null, null, null, null, UserDirectoryOrder.DEFAULT,
                new PageQuery(0, 100));
        verifyNoInteractions(auditRecorder);
    }

    @Test
    void clientCsvExportPreservesFiltersAndOrderAcrossPagesAndAuditsTheExport() {
        AuthenticatedUser admin = authorizedAdmin();
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 30);
        UserDirectoryOrder order = new UserDirectoryOrder(
                UserDirectoryOrder.Field.DISPLAY_NAME, UserDirectoryOrder.Direction.ASC);
        List<ManagedUserSummary> firstPage = IntStream.range(0, 100)
                .mapToObj(index -> client("client-" + index + "@example.test",
                        index == 0 ? "=1+1" : "Client " + index))
                .toList();
        List<ManagedUserSummary> secondPage = List.of(client("last@example.test", "Last Client"));
        Instant fromInclusive = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        when(users.search(admin, "CLIENT", "Client", "ACTIVE", "en", fromInclusive, toExclusive, order,
                new PageQuery(0, 100)))
                .thenReturn(new PageResult<>(firstPage, 0, 100, 101));
        when(users.search(admin, "CLIENT", "Client", "ACTIVE", "en", fromInclusive, toExclusive, order,
                new PageQuery(1, 100)))
                .thenReturn(new PageResult<>(secondPage, 1, 100, 101));

        var response = controller.exportClients("Client", "ACTIVE", "en", from, to,
                order.field(), order.direction(), admin);
        String csv = new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("attachment; filename=\"clients.csv\"");
        assertThat(csv).startsWith("email,display_name,role,status,locale,created_at\r\n");
        assertThat(csv).contains("\"'=1+1\"");
        assertThat(csv).contains("\"last@example.test\",\"Last Client\"");
        verify(users).search(admin, "CLIENT", "Client", "ACTIVE", "en", fromInclusive, toExclusive, order,
                new PageQuery(0, 100));
        verify(users).search(admin, "CLIENT", "Client", "ACTIVE", "en", fromInclusive, toExclusive, order,
                new PageQuery(1, 100));
        verify(auditRecorder).record(org.mockito.ArgumentMatchers.argThat(record ->
                record.actorId().equals(admin.userId())
                        && "CLIENT_DIRECTORY_EXPORTED".equals(record.actionCode())));
    }

    private org.springframework.http.ResponseEntity<byte[]> export(AuthenticatedUser actor) {
        return controller.exportClients(null, null, null, null, null,
                UserDirectoryOrder.Field.CREATED_AT, UserDirectoryOrder.Direction.DESC, actor);
    }

    private static AuthenticatedUser authorizedAdmin() {
        return new AuthenticatedUser(UUID.randomUUID(), Role.ADMIN,
                Set.of(Permission.USERS_MANAGE, Permission.CLIENTS_EXPORT));
    }

    private static ManagedUserSummary client(String email, String displayName) {
        IdentityUserSummary identity = new IdentityUserSummary(UUID.randomUUID(), email, displayName,
                "CLIENT", "ACTIVE", "en", true, false, Instant.parse("2026-11-19T12:00:00Z"));
        return new ManagedUserSummary(identity, Optional.empty(), Optional.empty());
    }
}
