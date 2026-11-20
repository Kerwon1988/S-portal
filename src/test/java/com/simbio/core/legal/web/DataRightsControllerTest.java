package com.simbio.core.legal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataRightsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousVisitorCannotOpenAccountDataRightsPage() throws Exception {
        mockMvc.perform(get("/cuenta/datos"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedNonClientCannotOpenAccountDataRightsPage() throws Exception {
        mockMvc.perform(get("/cuenta/datos").with(user("synthetic-employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/cuenta/datos").with(user("synthetic-admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedClientSeesInformationalPageWithoutDataMutationControls() throws Exception {
        String html = mockMvc.perform(get("/cuenta/datos").with(user("synthetic-client").roles("CLIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("legal/data-rights"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String main = mainContent(html);
        assertThat(main).containsAnyOf("Tus datos", "Your data")
                .containsAnyOf("Esta página no ha modificado ni eliminado datos.",
                        "This page has not changed or erased any data.")
                .doesNotContain("<form", "method=\"post\"", "name=\"export\"", "name=\"erasure\"");
    }

    private static String mainContent(String html) {
        int start = html.indexOf("<main id=\"main-content\"");
        int end = html.indexOf("</main>", start);
        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        return html.substring(start, end);
    }
}
