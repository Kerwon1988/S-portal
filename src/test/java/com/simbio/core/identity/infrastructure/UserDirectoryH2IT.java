package com.simbio.core.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.identity.api.UserDirectoryOrder;
import com.simbio.core.identity.application.IdentityUserDirectoryService;
import com.simbio.core.identity.application.UserAccountStore;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserDirectoryH2IT {

    @Autowired
    private IdentityUserDirectoryService directory;

    @Autowired
    private UserAccountStore accounts;

    @Test
    void clientSearchPreservesFiltersTotalAndStableOrderingAcrossPages() {
        String discriminator = UUID.randomUUID().toString();
        String displayName = "Synthetic directory " + discriminator;
        var first = accounts.createUser(
                "first-" + discriminator + "@example.test", "synthetic-password-hash", "es", "CLIENT", displayName);
        var second = accounts.createUser(
                "second-" + discriminator + "@example.test", "synthetic-password-hash", "es", "CLIENT", displayName);
        AuthenticatedUser admin = new AuthenticatedUser(
                UUID.randomUUID(), Role.ADMIN, Set.of(Permission.USERS_MANAGE));
        UserDirectoryOrder order = new UserDirectoryOrder(
                UserDirectoryOrder.Field.DISPLAY_NAME, UserDirectoryOrder.Direction.ASC);

        var firstPage = directory.search(admin, discriminator, "CLIENT", "ACTIVE", "es", null, null,
                order, new PageQuery(0, 1));
        var secondPage = directory.search(admin, discriminator, "CLIENT", "ACTIVE", "es", null, null,
                order, new PageQuery(1, 1));

        assertThat(firstPage.totalElements()).isEqualTo(2);
        assertThat(secondPage.totalElements()).isEqualTo(2);
        assertThat(firstPage.content()).singleElement().satisfies(summary -> {
            assertThat(summary.publicId()).isIn(first.getPublicId(), second.getPublicId());
            assertThat(summary.role()).isEqualTo("CLIENT");
            assertThat(summary.status()).isEqualTo("ACTIVE");
            assertThat(summary.locale()).isEqualTo("es");
        });
        assertThat(secondPage.content()).singleElement().satisfies(summary -> {
            assertThat(summary.publicId()).isIn(first.getPublicId(), second.getPublicId());
            assertThat(summary.publicId()).isNotEqualTo(firstPage.content().get(0).publicId());
        });
    }
}
