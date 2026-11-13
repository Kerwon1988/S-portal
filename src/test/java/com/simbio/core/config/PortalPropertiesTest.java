package com.simbio.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;

class PortalPropertiesTest {

    @Test
    void acceptsNonBlankPortalProperties() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();

        assertThat(validator.validate(new PortalProperties("https://portal.example.invalid", "Simbio")))
                .isEmpty();
    }

    @Test
    void rejectsBlankPortalProperties() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();

        assertThat(validator.validate(new PortalProperties(" ", "")))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("baseUrl", "brandName");
    }
}
