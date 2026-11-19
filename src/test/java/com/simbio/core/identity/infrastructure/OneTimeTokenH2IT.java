package com.simbio.core.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.identity.application.OneTimeTokenService;
import com.simbio.core.identity.application.InvitationService;
import com.simbio.core.identity.application.PendingInvitationSummary;
import com.simbio.core.identity.domain.TokenPurpose;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@SpringBootTest
@ActiveProfiles("test")
class OneTimeTokenH2IT {

    @Autowired
    private OneTimeTokenService tokenService;

    @Autowired
    private InvitationService invitationService;

    @Test
    void tokenIsConsumedExactlyOnceWhenRequestsRace() throws Exception {
        String token = tokenService.issueForUser(UUID.randomUUID(), TokenPurpose.PASSWORD_RESET,
                Duration.ofMinutes(5));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger consumed = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);
        try {
            for (int index = 0; index < 2; index++) {
                executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent test did not start");
                    }
                    if (tokenService.consume(token, TokenPurpose.PASSWORD_RESET).isPresent()) {
                        consumed.incrementAndGet();
                    }
                    return null;
                });
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
            assertThat(consumed.get()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void invitationCorrelationSurvivesPersistenceAndConsumption() {
        UUID reference = UUID.randomUUID();
        String token = tokenService.issueInvitation("synthetic@example.test", "CLIENT", reference,
                Duration.ofMinutes(5));

        var consumed = tokenService.consume(token, TokenPurpose.INVITATION);

        assertThat(consumed).isPresent();
        assertThat(consumed.orElseThrow().invitationReference()).isEqualTo(reference);
        assertThat(consumed.orElseThrow().toString()).doesNotContain("synthetic@example.test");
    }

    @Test
    void pendingInvitationCanBeListedAndResentWithoutChangingItsOwnerScope() {
        AuthenticatedUser actor = new AuthenticatedUser(
                UUID.randomUUID(), Role.ADMIN, Set.of(Permission.STAFF_MANAGE));
        Set<UUID> previousInvitations = invitationService.pendingInvitations(
                        actor, "EMPLOYEE", new PageQuery(0, 25)).content().stream()
                .map(PendingInvitationSummary::id)
                .collect(java.util.stream.Collectors.toSet());
        String email = "worker-" + UUID.randomUUID() + "@example.test";

        assertThat(invitationService.invite(actor, email, "EMPLOYEE", Locale.ENGLISH)).isTrue();

        var afterInvite = invitationService.pendingInvitations(actor, "EMPLOYEE", new PageQuery(0, 25));
        PendingInvitationSummary pending = afterInvite.content().stream()
                .filter(invitation -> !previousInvitations.contains(invitation.id()))
                .findFirst()
                .orElseThrow();
        assertThat(pending.maskedEmail()).doesNotContain(email).endsWith("@example.test");
        assertThat(pending.roleCode()).isEqualTo("EMPLOYEE");

        assertThat(invitationService.resendInvitation(actor, pending.id(), "EMPLOYEE", Locale.ENGLISH)).isTrue();

        var afterResend = invitationService.pendingInvitations(actor, "EMPLOYEE", new PageQuery(0, 25));
        assertThat(afterResend.totalElements()).isEqualTo(afterInvite.totalElements());
        assertThat(afterResend.content()).noneMatch(invitation -> invitation.id().equals(pending.id()));
        assertThat(afterResend.content()).anySatisfy(invitation -> {
            assertThat(invitation.maskedEmail()).isEqualTo(pending.maskedEmail());
            assertThat(invitation.roleCode()).isEqualTo("EMPLOYEE");
            assertThat(previousInvitations).doesNotContain(invitation.id());
        });
    }

    @Test
    void pendingInvitationsAreStableAcrossPages() {
        AuthenticatedUser actor = new AuthenticatedUser(
                UUID.randomUUID(), Role.ADMIN, Set.of(Permission.STAFF_MANAGE));
        long previousCount = invitationService.pendingInvitations(
                actor, "EMPLOYEE", new PageQuery(0, 1)).totalElements();

        assertThat(invitationService.invite(
                actor, "first-" + UUID.randomUUID() + "@example.test", "EMPLOYEE", Locale.ENGLISH)).isTrue();
        assertThat(invitationService.invite(
                actor, "second-" + UUID.randomUUID() + "@example.test", "EMPLOYEE", Locale.ENGLISH)).isTrue();

        var firstPage = invitationService.pendingInvitations(actor, "EMPLOYEE", new PageQuery(0, 1));
        var repeatedFirstPage = invitationService.pendingInvitations(actor, "EMPLOYEE", new PageQuery(0, 1));
        var secondPage = invitationService.pendingInvitations(actor, "EMPLOYEE", new PageQuery(1, 1));
        assertThat(firstPage.content()).hasSize(1);
        assertThat(repeatedFirstPage.content()).hasSize(1);
        assertThat(secondPage.content()).hasSize(1);
        assertThat(firstPage.totalElements()).isEqualTo(previousCount + 2);
        assertThat(firstPage.totalElements()).isEqualTo(secondPage.totalElements());
        assertThat(firstPage.content().get(0).id()).isEqualTo(repeatedFirstPage.content().get(0).id())
                .isNotEqualTo(secondPage.content().get(0).id());
    }
}
