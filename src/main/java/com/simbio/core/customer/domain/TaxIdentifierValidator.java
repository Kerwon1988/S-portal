package com.simbio.core.customer.domain;

public interface TaxIdentifierValidator {

    String countryCode();

    boolean accepts(String normalizedValue);
}
