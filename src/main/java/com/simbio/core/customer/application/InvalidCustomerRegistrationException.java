package com.simbio.core.customer.application;

public class InvalidCustomerRegistrationException extends RuntimeException {

    private final String field;

    public InvalidCustomerRegistrationException(String field) {
        super("Customer registration data is invalid");
        this.field = field;
    }

    public String field() {
        return field;
    }
}
