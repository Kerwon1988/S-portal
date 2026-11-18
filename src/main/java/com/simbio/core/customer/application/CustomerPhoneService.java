package com.simbio.core.customer.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.customer.api.CustomerPhoneManager;
import com.simbio.core.customer.domain.ClientAccountEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.SensitiveDataProtector;

@Service
public class CustomerPhoneService implements CustomerPhoneManager {

    private final ClientAccountStore store;
    private final SensitiveDataProtector protector;
    private final AuditRecorder auditRecorder;

    public CustomerPhoneService(
            ClientAccountStore store,
            SensitiveDataProtector protector,
            AuditRecorder auditRecorder) {
        this.store = Objects.requireNonNull(store);
        this.protector = Objects.requireNonNull(protector);
        this.auditRecorder = Objects.requireNonNull(auditRecorder);
    }

    @Override
    @Transactional
    public void setForOwnedAccount(AuthenticatedUser actor, UUID accountPublicId, String phoneNumber) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(accountPublicId, "accountPublicId must not be null");
        if (actor.role() != Role.CLIENT) {
            throw new ClientAccountNotFoundException();
        }
        if (phoneNumber == null || !phoneNumber.trim().matches("\\+[1-9][0-9]{0,14}")) {
            throw new IllegalArgumentException("Phone number must use E.164 format");
        }
        ClientAccountEntity account = store.findOwnedAccountBy(accountPublicId, actor.userId())
                .orElseThrow(ClientAccountNotFoundException::new);
        account.setPhoneCiphertext(protector.protect(phoneNumber.trim()));
        store.save(account);
        auditRecorder.record(new AuditRecord(actor.userId(), "CLIENT_PHONE_SET",
                "CLIENT_ACCOUNT", account.getPublicId(), null));
    }
}
