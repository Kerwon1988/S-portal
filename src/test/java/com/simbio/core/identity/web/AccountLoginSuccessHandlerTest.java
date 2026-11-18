package com.simbio.core.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.simbio.core.identity.application.LoginAttemptService;
import com.simbio.core.identity.application.MfaService;
import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.identity.security.PortalUserPrincipal;

class AccountLoginSuccessHandlerTest {

    @Test
    void enrolledEmployeeMustVerifyMfaAfterLogin() throws Exception {
        UUID userId = UUID.randomUUID();
        PortalUserPrincipal principal = new PortalUserPrincipal(new UserAccountEntity(
                userId,
                "employee@example.test",
                "synthetic-hash",
                "en",
                Instant.now(),
                new RoleEntity("EMPLOYEE", "Employee")));
        LoginAttemptService loginAttempts = mock(LoginAttemptService.class);
        MfaService mfaService = mock(MfaService.class);
        when(mfaService.isEnrolled(userId)).thenReturn(true);
        AccountLoginSuccessHandler handler = new AccountLoginSuccessHandler(loginAttempts, mfaService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, principal.getPassword(), principal.getAuthorities());

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("/cuenta/seguridad/mfa");
        assertThat(request.getSession(false).getAttribute("mfaContinue")).isEqualTo("/es");
        assertThat(request.getSession(false).getAttribute("mfaPending")).isEqualTo(true);
        verify(mfaService).isEnrolled(userId);
    }
}
