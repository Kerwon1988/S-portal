package com.simbio.core.customer.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.simbio.core.identity.api.InvitationAccepted;

@Component
class CustomerInvitationListener {

    private final CustomerInvitationService invitations;

    CustomerInvitationListener(CustomerInvitationService invitations) {
        this.invitations = invitations;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onInvitationAccepted(InvitationAccepted event) {
        invitations.completeInvitation(event);
    }
}
