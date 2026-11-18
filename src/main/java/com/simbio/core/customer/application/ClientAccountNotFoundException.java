package com.simbio.core.customer.application;

public class ClientAccountNotFoundException extends RuntimeException {

    public ClientAccountNotFoundException() {
        super("Client account was not found");
    }
}
