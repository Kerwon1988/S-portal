package com.simbio.core.identity.api;

import java.util.Locale;
import java.util.UUID;

import com.simbio.core.shared.security.AuthenticatedUser;

public interface InvitationIssuer {

    boolean invite(AuthenticatedUser actor, String email, String roleCode, Locale locale);

    boolean invite(
            AuthenticatedUser actor,
            String email,
            String roleCode,
            Locale locale,
            UUID invitationReference);
}
