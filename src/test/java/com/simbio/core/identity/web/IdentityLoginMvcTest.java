package com.simbio.core.identity.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.simbio.core.identity.application.AdminBootstrapService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentityLoginMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminBootstrapService bootstrapService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void rendersLocalizedLoginAndAcceptsOnlyCsrfProtectedAuthentication() throws Exception {
        mockMvc.perform(get("/acceso").header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(view().name("identity/login"));
        bootstrapService.bootstrap("admin@example.test", "synthetic-password-4", "en");
        org.assertj.core.api.Assertions.assertThat(
                bootstrapService.bootstrap("another-admin@example.test", "synthetic-password-5", "es"))
                .isFalse();

        mockMvc.perform(post("/acceso")
                        .with(csrf())
                        .param("username", "admin@example.test")
                        .param("password", "incorrect-synthetic-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/acceso?error"));
        mockMvc.perform(post("/acceso")
                        .with(csrf())
                        .param("username", "unknown@example.test")
                        .param("password", "incorrect-synthetic-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/acceso?error"));

        mockMvc.perform(post("/acceso")
                        .param("username", "admin@example.test")
                        .param("password", "synthetic-password-4"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/acceso")
                        .with(csrf())
                        .param("username", "admin@example.test")
                        .param("password", "synthetic-password-4")
                        .param("continue", "/gestion/auditoria"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestion/auditoria"));

        mockMvc.perform(post("/acceso")
                        .with(csrf())
                        .param("username", "admin@example.test")
                        .param("password", "synthetic-password-4")
                        .param("continue", "//external.example"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestion/auditoria"));

        org.assertj.core.api.Assertions.assertThat(
                passwordEncoder.encode("synthetic-password-4"))
                .startsWith("{argon2id}")
                .doesNotContain("synthetic-password-4");
    }
}
