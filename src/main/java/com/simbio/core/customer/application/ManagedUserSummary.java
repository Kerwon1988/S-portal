package com.simbio.core.customer.application;

import java.util.Optional;

import com.simbio.core.customer.api.CustomerAccountSummary;
import com.simbio.core.identity.api.IdentityUserSummary;
import com.simbio.core.staff.api.StaffSummary;

public record ManagedUserSummary(
        IdentityUserSummary identity,
        Optional<CustomerAccountSummary> customerAccount,
        Optional<StaffSummary> staffProfile) {

    public ManagedUserSummary {
        customerAccount = Optional.ofNullable(customerAccount).orElseGet(Optional::empty);
        staffProfile = Optional.ofNullable(staffProfile).orElseGet(Optional::empty);
    }
}
