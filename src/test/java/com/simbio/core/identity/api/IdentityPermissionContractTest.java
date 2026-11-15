package com.simbio.core.identity.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdentityPermissionContractTest {

    @Test
    void identityPermissionsMatchSharedNavigationPermissions() {
        assertThat(java.util.Arrays.stream(Permission.values()).map(Enum::name))
                .containsExactlyInAnyOrderElementsOf(
                        java.util.Arrays.stream(com.simbio.core.shared.security.Permission.values())
                                .map(Enum::name).toList());
        assertThat(java.util.Arrays.stream(Role.values()).map(Enum::name))
                .containsExactlyInAnyOrderElementsOf(
                        java.util.Arrays.stream(com.simbio.core.shared.security.Role.values())
                                .map(Enum::name).toList());
    }
}
