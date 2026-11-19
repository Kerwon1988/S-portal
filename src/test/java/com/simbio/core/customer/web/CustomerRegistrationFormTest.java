package com.simbio.core.customer.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CustomerRegistrationFormTest {

    @Test
    void prefersAutomaticProofAndFallsBackToAccessiblePayloadWhenEmpty() {
        CustomerRegistrationForm automatic = form("automatic-proof", "accessible-proof");
        CustomerRegistrationForm accessible = form("", "accessible-proof");

        assertThat(automatic.toCommand("es").altchaPayload()).isEqualTo("automatic-proof");
        assertThat(accessible.toCommand("es").altchaPayload()).isEqualTo("accessible-proof");
    }

    private static CustomerRegistrationForm form(String altcha, String fallback) {
        return new CustomerRegistrationForm("", "", "", "", "INDIVIDUAL", "", "ES", "",
                false, false, false, 1, 1, 1, altcha, fallback, "", false);
    }
}
