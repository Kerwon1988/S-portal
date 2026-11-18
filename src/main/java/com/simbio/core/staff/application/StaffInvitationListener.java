package com.simbio.core.staff.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.simbio.core.identity.api.InvitationAccepted;
import com.simbio.core.identity.api.IdentityAccountChanged;

@Component
class StaffInvitationListener {

    private final StaffProfileService profiles;

    StaffInvitationListener(StaffProfileService profiles) {
        this.profiles = profiles;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onInvitationAccepted(InvitationAccepted event) {
        profiles.createFromInvitation(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onIdentityAccountChanged(IdentityAccountChanged event) {
        profiles.synchronizeIdentityAccount(event);
    }
}
