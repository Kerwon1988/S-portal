package com.simbio.core.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.simbio.core.customer.domain.TaxIdentifier;

class SpainTaxIdentifierValidatorTest {

    private final SpainTaxIdentifierValidator validator = new SpainTaxIdentifierValidator();

    @Test
    void acceptsValidNifNieAndCifAfterLocalNormalization() {
        assertThat(validator.accepts(TaxIdentifier.normalize("1234 5678-z"))).isTrue();
        assertThat(validator.accepts(TaxIdentifier.normalize("X1234567-l"))).isTrue();
        assertThat(validator.accepts(TaxIdentifier.normalize("B12345674"))).isTrue();
    }

    @Test
    void rejectsInvalidChecksumsAndDoesNotAcceptAnotherCountry() {
        assertThat(validator.accepts("12345678A")).isFalse();
        assertThat(validator.accepts("X1234567A")).isFalse();
        assertThat(validator.accepts("B12345675")).isFalse();
        assertThat(validator.countryCode()).isEqualTo("ES");
        assertThatThrownBy(() -> TaxIdentifier.normalize(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void masksAllButTheLastFourCharacters() {
        assertThat(new TaxIdentifier("ES", "12345678Z").masked()).isEqualTo("*****678Z");
    }

    @Test
    void extensionPointRejectsCountriesWithoutAnInstalledValidator() {
        List<com.simbio.core.customer.domain.TaxIdentifierValidator> validators = List.of(validator);
        assertThat(validators.stream().noneMatch(candidate -> candidate.countryCode().equals("FR"))).isTrue();
    }
}
