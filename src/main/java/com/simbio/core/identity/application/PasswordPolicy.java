package com.simbio.core.identity.application;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    public boolean accepts(String password) {
        if (password == null) {
            return false;
        }
        int length = password.codePointCount(0, password.length());
        return length >= 12 && length <= 64;
    }

    public void requireAccepted(String password) {
        if (!accepts(password)) {
            throw new InvalidPasswordException();
        }
    }
}
