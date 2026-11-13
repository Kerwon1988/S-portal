package com.simbio.core.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.simbio.tests.security.SecurityTestEndpoints;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityTestEndpoints.class)
class CsrfTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsMutationWithoutTokenAndAcceptsCookieTokenPair() throws Exception {
        mockMvc.perform(post("/public/mutation"))
                .andExpect(status().isForbidden());

        MvcResult tokenResponse = mockMvc.perform(get("/public/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie csrfCookie = tokenResponse.getResponse().getCookie("SPORTAL_XSRF");
        String token = tokenResponse.getResponse().getContentAsString();

        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.isHttpOnly()).isFalse();
        mockMvc.perform(post("/public/mutation")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", token))
                .andExpect(status().isOk());
    }

    @Test
    void protectsThemePreferenceMutationAndRejectsUnsupportedValues() throws Exception {
        MvcResult tokenResponse = mockMvc.perform(get("/public/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie csrfCookie = tokenResponse.getResponse().getCookie("SPORTAL_XSRF");
        String token = tokenResponse.getResponse().getContentAsString();

        mockMvc.perform(post("/preferences/theme").param("theme", "dark"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/preferences/theme")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", token)
                        .param("theme", "dark"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/preferences/theme")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", token)
                        .param("theme", "system"))
                .andExpect(status().isBadRequest());
    }
}
