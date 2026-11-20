package com.simbio.core.notification.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.simbio.core.notification.application.NotificationPreferenceService;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.CurrentUser;

class NotificationPreferenceMvcTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CurrentUser currentUser = mock(CurrentUser.class);
        when(currentUser.currentUser()).thenReturn(Optional.of(
                new AuthenticatedUser(UUID.randomUUID(), Role.CLIENT, Set.of())));
        NotificationPreferenceController controller = new NotificationPreferenceController(
                mock(NotificationPreferenceService.class), currentUser);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new CsrfFilter(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .build();
    }

    @Test
    void rejectsPreferenceMutationWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/cuenta/notificaciones").param("requestUpdates", "false"))
                .andExpect(status().isForbidden());
    }
}