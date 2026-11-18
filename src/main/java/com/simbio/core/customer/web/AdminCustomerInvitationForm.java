package com.simbio.core.customer.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCustomerInvitationForm(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Pattern(regexp = "COMPANY|SOLE_TRADER|INDIVIDUAL") String accountType,
        @NotBlank @Size(max = 160) String accountName) {

    @Override
    public String toString() {
        return "AdminCustomerInvitationForm[<redacted>]";
    }
}
