package com.simbio.core.audit.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.audit.application.AuditPage;
import com.simbio.core.audit.application.AuditView;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditRecorder auditRecorder;

    @Test
    void invalidFilterShowsAccessibleErrorWithoutFallingBackToUnfilteredAuditData() throws Exception {
        String sensitiveActionCode = "SYNTHETIC_AUDIT_EVENT";
        auditRecorder.record(new AuditRecord(
                UUID.randomUUID(), sensitiveActionCode, "TEST", UUID.randomUUID(), "synthetic-correlation"));

        var result = mockMvc.perform(get("/gestion/auditoria")
                        .header("Accept-Language", "en")
                        .with(user("auditor").authorities(new SimpleGrantedAuthority("AUDIT_VIEW")))
                        .param("actorId", "not-a-uuid"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("invalidFilter", true))
                .andReturn();

        AuditPage page = (AuditPage) result.getModelAndView().getModel().get("auditPage");
        assertThat(page.entries()).isEmpty();
        assertThat(result.getResponse().getContentAsString()).contains("One or more filters are invalid.")
                .doesNotContain(sensitiveActionCode);
    }

    @Test
    void rendersPagedEntityHistoryAndAccessibleTableForAuthorizedUser() throws Exception {
        UUID requestedEntityId = UUID.randomUUID();
        String actionCode = "AUDIT_UI_ACCEPTANCE";
        auditRecorder.record(new AuditRecord(
                UUID.randomUUID(), actionCode, "TEST", requestedEntityId, "synthetic-correlation"));
        for (int index = 0; index < 25; index++) {
            auditRecorder.record(new AuditRecord(
                    UUID.randomUUID(), actionCode, "TEST", UUID.randomUUID(), "synthetic-correlation"));
        }

        var result = mockMvc.perform(get("/gestion/auditoria")
                        .header("Accept-Language", "en")
                        .with(user("auditor").authorities(new SimpleGrantedAuthority("AUDIT_VIEW")))
                        .param("actionCode", actionCode)
                        .param("entityType", "TEST")
                        .param("entityId", requestedEntityId.toString())
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andReturn();

        AuditPage page = (AuditPage) result.getModelAndView().getModel().get("auditPage");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.totalElements()).isEqualTo(26);
        assertThat(page.entries()).hasSize(1);
        List<?> entityHistory = (List<?>) result.getModelAndView().getModel().get("entityHistory");
        assertThat(entityHistory).hasSize(1);
        assertThat(((AuditView) entityHistory.get(0)).entityPublicId()).isEqualTo(requestedEntityId);

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("for=\"actorId\"", "id=\"actorId\"", "<caption", "scope=\"col\"", "<nav")
                .contains("entityId=" + requestedEntityId);
    }

    @Test
    void rejectsPartialEntityHistoryFilterWithoutQueryingAllEvents() throws Exception {
        mockMvc.perform(get("/gestion/auditoria")
                        .with(user("auditor").authorities(new SimpleGrantedAuthority("AUDIT_VIEW")))
                        .param("entityType", "TEST"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("invalidFilter", true));
    }

    @Test
    void rendersUserAuditPanelOnlyThroughTheAuditOwnerRoute() throws Exception {
        UUID userId = UUID.randomUUID();
        auditRecorder.record(new AuditRecord(
                UUID.randomUUID(), "USER_STATUS_CHANGED", "USER", userId, "synthetic-correlation"));

        var result = mockMvc.perform(get("/gestion/auditoria/usuarios/{userId}/panel", userId)
                        .header("Accept-Language", "en")
                        .with(user("auditor").authorities(new SimpleGrantedAuthority("AUDIT_VIEW"))))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getModelAndView().getViewName()).isEqualTo("audit/user-panel :: panel");
        assertThat(result.getResponse().getContentAsString())
                .contains("User audit history", "USER_STATUS_CHANGED")
                .doesNotContain("synthetic-correlation");
    }
}
