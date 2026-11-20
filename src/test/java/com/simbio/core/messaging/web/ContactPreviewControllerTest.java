package com.simbio.core.messaging.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContactPreviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void spanishPreviewShowsDisabledControlsAndNoSubmissionForm() throws Exception {
        String html = mockMvc.perform(get("/es/contacto"))
                .andExpect(status().isOk())
                .andExpect(header().string("Permissions-Policy", "microphone=(self)"))
                .andExpect(view().name("messaging/contact-preview"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String main = mainContent(html);
        assertThat(main).contains("Contacto",         "El formulario está temporalmente desactivado",
                        "El texto y la versión del consentimiento están pendientes de aprobación legal.",
                        "<fieldset disabled", "id=\"contact-name\"", "id=\"contact-email\"",
                        "id=\"contact-message\"", "role=\"status\"")
                .doesNotContain("<form", "method=\"post\"", "name=\"contact-name\"");
    }

    @Test
    void englishPreviewIsLocalizedAndDoesNotEnableContactCollection() throws Exception {
        String html = mockMvc.perform(get("/en/contacto"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String main = mainContent(html);
        assertThat(main).contains("Contact", "The form is temporarily unavailable",
                        "Consent wording and version are pending legal approval.",
                        "<fieldset disabled", "id=\"contact-phone\"")
                .doesNotContain("<form", "method=\"post\"");
    }

    @Test
    void contactSubmissionRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/es/contacto")
                        .param("personName", "Synthetic Person")
                        .param("email", "person@example.test")
                        .param("reason", "Synthetic reason")
                        .param("message", "Synthetic message")
                        .param("consentAccepted", "true")
                        .param("consentVersion", "1"))
                .andExpect(status().isForbidden());
    }

    private static String mainContent(String html) {
        int start = html.indexOf("<main id=\"main-content\"");
        int end = html.indexOf("</main>", start);
        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        return html.substring(start, end);
    }
}
