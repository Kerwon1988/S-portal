package com.simbio.core.simbiosphere.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.simbio.core.identity.api.EmailVerified;

@Component
class SimbioSphereEmailVerificationListener {
    private final ProvisioningService provisioning;

    SimbioSphereEmailVerificationListener(ProvisioningService provisioning) {
        this.provisioning = provisioning;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onEmailVerified(EmailVerified event) {
        provisioning.markReadyAfterVerification(event.userId());
    }
}
