package com.simbio.core.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void emitsUniqueNonceCspHstsAndDeniesUnlistedManagementRoutes() throws Exception {
        MvcResult first = mockMvc.perform(get("/public/headers").secure(true))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult second = mockMvc.perform(get("/public/headers").secure(true))
                .andExpect(status().isOk())
                .andReturn();

        String firstCsp = first.getResponse().getHeader("Content-Security-Policy");
        String secondCsp = second.getResponse().getHeader("Content-Security-Policy");
        assertThat(firstCsp).contains("nonce-").doesNotContain("unsafe-inline", "unsafe-eval");
        assertThat(secondCsp).isNotEqualTo(firstCsp);
        assertThat(first.getResponse().getHeader("Strict-Transport-Security")).contains("max-age=");
        assertThat(mockMvc.perform(get("/gestion/secret"))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getStatus()).isEqualTo(403);
    }
}
