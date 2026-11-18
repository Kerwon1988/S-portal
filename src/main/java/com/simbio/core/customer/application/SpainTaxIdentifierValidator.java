package com.simbio.core.customer.application;

import org.springframework.stereotype.Component;

import com.simbio.core.customer.domain.TaxIdentifierValidator;

@Component
public class SpainTaxIdentifierValidator implements TaxIdentifierValidator {

    private static final String NIF_CONTROL_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";
    private static final String CIF_CONTROL_LETTERS = "JABCDEFGHI";

    @Override
    public String countryCode() {
        return "ES";
    }

    @Override
    public boolean accepts(String value) {
        if (value == null) {
            return false;
        }
        return isNif(value) || isNie(value) || isCif(value);
    }

    private static boolean isNif(String value) {
        if (!value.matches("[0-9]{8}[A-Z]")) {
            return false;
        }
        int number = Integer.parseInt(value.substring(0, 8));
        return value.charAt(8) == NIF_CONTROL_LETTERS.charAt(number % 23);
    }

    private static boolean isNie(String value) {
        if (!value.matches("[XYZ][0-9]{7}[A-Z]")) {
            return false;
        }
        char prefix = value.charAt(0);
        int prefixValue = prefix == 'X' ? 0 : prefix == 'Y' ? 1 : 2;
        int number = Integer.parseInt(prefixValue + value.substring(1, 8));
        return value.charAt(8) == NIF_CONTROL_LETTERS.charAt(number % 23);
    }

    private static boolean isCif(String value) {
        if (!value.matches("[ABCDEFGHJNPQRSUVW][0-9]{7}[0-9A-J]")) {
            return false;
        }
        int sum = 0;
        for (int position = 0; position < 7; position++) {
            int digit = value.charAt(position + 1) - '0';
            if (position % 2 == 0) {
                int doubled = digit * 2;
                sum += doubled / 10 + doubled % 10;
            } else {
                sum += digit;
            }
        }
        int control = (10 - sum % 10) % 10;
        char controlDigit = (char) ('0' + control);
        char controlLetter = CIF_CONTROL_LETTERS.charAt(control);
        char actual = value.charAt(8);
        char prefix = value.charAt(0);
        if ("ABEH".indexOf(prefix) >= 0) {
            return actual == controlDigit;
        }
        if ("PQS".indexOf(prefix) >= 0) {
            return actual == controlLetter;
        }
        return actual == controlDigit || actual == controlLetter;
    }
}
