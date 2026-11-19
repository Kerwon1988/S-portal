package com.simbio.core.config.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.simbio.core.audit.application.AuditRecorderService;
import com.simbio.core.config.errortracking.ErrorInventorySnapshot;
import com.simbio.core.config.errortracking.ErrorInventoryService;
import com.simbio.core.config.errortracking.ErrorRecord;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.AuditEvent;
import com.simbio.core.shared.spi.CurrentUser;
import com.simbio.core.shared.spi.ErrorReporter.Source;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorInventoryControllerTest {

    private static final String FINGERPRINT = "a".repeat(64);
    private static final UUID ADMIN_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUser currentUser;

    @MockitoBean
    private ErrorInventoryService inventoryService;

    @MockitoBean
    private AuditRecorderService auditRecorder;

    @BeforeEach
    void provideCurrentUser() {
        when(currentUser.currentUser()).thenReturn(Optional.of(
                new AuthenticatedUser(ADMIN_ID, Role.ADMIN, Set.of(Permission.ERROR_INVENTORY_MANAGE))));
    }

    @Test
    void rendersFilteredSanitizedSummariesForAdmin() throws Exception {
        String excludedFingerprint = "b".repeat(64);
        when(inventoryService.snapshot()).thenReturn(inventory(true, List.of(
                entry(FINGERPRINT, "HIGH", "OPEN", "/gestion/{id}", ""),
                entry(excludedFingerprint, "LOW", "RESOLVED", "/customers/123456/ana@example.test",
                        "token=synthetic-secret"))));

        mockMvc.perform(get("/gestion/errores")
                        .with(admin())
                        .param("source", "BACKEND")
                        .param("severity", "HIGH")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(view().name("config/errortracking/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(FINGERPRINT)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("java.lang.IllegalStateException")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("scope=\"col\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "for=\"resolution-note-" + FINGERPRINT + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(excludedFingerprint))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("corr-12345678"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("ana@example.test"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("synthetic-secret"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("com.simbio.core.example.Handler.run"))));
    }

    @Test
    void rendersSemanticTableAndExplicitLabelsForViewerControls() throws Exception {
        when(inventoryService.snapshot()).thenReturn(inventory(true, List.of(
                entry(FINGERPRINT, "HIGH", "OPEN", "/gestion/{id}", ""))));

        mockMvc.perform(get("/gestion/errores").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<h1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<caption")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("scope=\"col\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("for=\"source\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("for=\"severity\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("for=\"status\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<details>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<summary")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "for=\"resolution-note-" + FINGERPRINT + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "for=\"status-" + FINGERPRINT + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("type=\"submit\"")));
    }

    @Test
    void deniesNonAdminEvenWhenTheyHaveTheDedicatedPermission() throws Exception {
        mockMvc.perform(get("/gestion/errores")
                        .with(user("employee").authorities(
                                authority("ROLE_EMPLOYEE"), authority("ERROR_INVENTORY_MANAGE"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/gestion/errores")
                        .with(user("client").authorities(
                                authority("ROLE_CLIENT"), authority("ERROR_INVENTORY_MANAGE"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(inventoryService);
    }

    @Test
    void requiresDedicatedPermissionForAdmin() throws Exception {
        mockMvc.perform(get("/gestion/errores")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(inventoryService);
    }

    @Test
    void requiresCsrfForResolutionAndAuditsSuccessfulUpdate() throws Exception {
        mockMvc.perform(post("/gestion/errores/{fingerprint}/estado", FINGERPRINT)
                        .with(admin())
                        .param("status", "RESOLVED")
                        .param("resolutionNote", "Fixed the synthetic failure"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(inventoryService, auditRecorder);

        mockMvc.perform(post("/gestion/errores/{fingerprint}/estado", FINGERPRINT)
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("status", "RESOLVED")
                        .param("resolutionNote", "Fixed the synthetic failure"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/gestion/errores*"));

        verify(inventoryService).updateStatus(FINGERPRINT, "RESOLVED", "Fixed the synthetic failure");
        verify(auditRecorder).record(any(AuditEvent.class));
    }

    @Test
    void returnsDegradedPageWithoutInventoryDetailsWhenStorageIsUnavailable() throws Exception {
        when(inventoryService.snapshot()).thenReturn(inventory(false, List.of()));

        mockMvc.perform(get("/gestion/errores").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.containsString("El almacenamiento no está disponible"),
                        org.hamcrest.Matchers.containsString("Storage is unavailable"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(FINGERPRINT))));
    }

    @Test
    void redactsPathAndResolutionNoteBeforeRenderingDetails() throws Exception {
        when(inventoryService.snapshot()).thenReturn(inventory(true, List.of(
                entry(FINGERPRINT, "HIGH", "RESOLVED", "/customers/123456/ana@example.test",
                        "Removed token=synthetic-secret"))));

        mockMvc.perform(get("/gestion/errores").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[redacted]")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("ana@example.test"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("synthetic-secret"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("com.simbio.core.example.Handler.run"))));
    }

    @Test
    void rejectsUnknownStatusWithoutCallingInventoryService() throws Exception {
        mockMvc.perform(post("/gestion/errores/{fingerprint}/estado", FINGERPRINT)
                        .with(admin())
                        .with(csrf())
                        .param("status", "DELETED")
                        .param("resolutionNote", "Synthetic note"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(inventoryService, auditRecorder);
    }

    @Test
    void hidesUnknownInventoryIdentifiers() throws Exception {
        doThrow(new NoSuchElementException())
                .when(inventoryService).updateStatus(FINGERPRINT, "RESOLVED", "Synthetic note");

        mockMvc.perform(post("/gestion/errores/{fingerprint}/estado", FINGERPRINT)
                        .with(admin())
                        .with(csrf())
                        .param("status", "RESOLVED")
                        .param("resolutionNote", "Synthetic note"))
                .andExpect(status().isNotFound());

        verify(inventoryService).updateStatus(FINGERPRINT, "RESOLVED", "Synthetic note");
        verifyNoInteractions(auditRecorder);
    }

    private static RequestPostProcessor admin() {
        return user("admin").authorities(
                authority("ROLE_ADMIN"), authority("ERROR_INVENTORY_MANAGE"));
    }

    private static org.springframework.security.core.authority.SimpleGrantedAuthority authority(String name) {
        return new org.springframework.security.core.authority.SimpleGrantedAuthority(name);
    }

    private static ErrorInventorySnapshot inventory(boolean available, List<ErrorRecord> records) {
        return new ErrorInventorySnapshot(available, 1_000, records);
    }

    private static ErrorRecord entry(String fingerprint, String severity, String status, String path, String note) {
        return new ErrorRecord(
                fingerprint,
                Source.BACKEND,
                "MVC_EXCEPTION",
                "IllegalStateException",
                "java.lang.IllegalStateException",
                "java.lang.IllegalStateException\n at com.simbio.core.example.Handler.run",
                path,
                "test-release",
                "en",
                "corr-12345678",
                severity,
                status,
                note,
                "",
                0,
                "2026-11-18T07:00:00Z",
                "2026-11-18T07:05:00Z",
                2);
    }
}
