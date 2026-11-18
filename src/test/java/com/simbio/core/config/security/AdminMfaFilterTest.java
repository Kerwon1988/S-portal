package com.simbio.core.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AdminMfaFilterTest {

    private final AdminMfaFilter filter = new AdminMfaFilter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksEnrolledEmployeeFromProtectedRouteUntilMfaIsVerified() throws Exception {
        authenticate("ROLE_EMPLOYEE");
        MockHttpServletRequest request = request("/gestion/identidad/invitaciones");
        request.getSession(true).setAttribute("mfaPending", true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getRedirectedUrl()).isEqualTo("/cuenta/seguridad/mfa");
        verifyNoInteractions(chain);
    }

    @Test
    void blocksAccountProfileUntilEmployeeCompletesChallenge() throws Exception {
        authenticate("ROLE_EMPLOYEE");
        MockHttpServletRequest request = request("/cuenta/perfil");
        request.getSession(true).setAttribute("mfaPending", true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getRedirectedUrl()).isEqualTo("/cuenta/seguridad/mfa");
        verifyNoInteractions(chain);
    }

    @Test
    void permitsMfaChallengeWhileOtherRoutesAreBlocked() throws Exception {
        authenticate("ROLE_EMPLOYEE");
        MockHttpServletRequest request = request("/cuenta/seguridad/mfa/verificar");
        request.getSession(true).setAttribute("mfaPending", true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
    }

    @Test
    void allowsEnrolledEmployeeAfterMfaVerification() throws Exception {
        authenticate("ROLE_EMPLOYEE", "MFA_VERIFIED");
        MockHttpServletRequest request = request("/gestion/identidad/invitaciones");
        request.getSession(true).setAttribute("mfaPending", true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
    }

    @Test
    void allowsEmployeeWithoutMfaOptIn() throws Exception {
        authenticate("ROLE_EMPLOYEE");
        MockHttpServletRequest request = request("/cuenta/perfil");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
    }

    @Test
    void continuesToRequireMfaForAdministrators() throws Exception {
        authenticate("ROLE_ADMIN");
        MockHttpServletRequest request = request("/gestion/identidad/invitaciones");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getRedirectedUrl()).isEqualTo("/cuenta/seguridad/mfa");
        verifyNoInteractions(chain);
    }

    private static void authenticate(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "synthetic-user",
                "n/a",
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath(path);
        return request;
    }
}
