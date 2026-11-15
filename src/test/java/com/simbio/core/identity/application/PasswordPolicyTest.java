package com.simbio.core.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsInclusiveCharacterBoundsIncludingSupplementaryCharacters() {
        assertThat(policy.accepts("abcdefghijkl")).isTrue();
        assertThat(policy.accepts("😀".repeat(12))).isTrue();
        assertThat(policy.accepts("x".repeat(64))).isTrue();
    }

    @Test
    void rejectsEmptyShortAndOverlongPasswords() {
        assertThat(policy.accepts(null)).isFalse();
        assertThat(policy.accepts("")).isFalse();
        assertThat(policy.accepts("short")).isFalse();
        assertThat(policy.accepts("x".repeat(65))).isFalse();
        assertThatThrownBy(() -> policy.requireAccepted("short"))
                .isInstanceOf(InvalidPasswordException.class);
    }
}
