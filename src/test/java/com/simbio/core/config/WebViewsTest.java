package com.simbio.core.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import jakarta.servlet.RequestDispatcher;
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
class WebViewsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rendersPublicLayoutsWithLocaleNonceAndFingerprintedAssets() throws Exception {
        mockMvc.perform(get("/es"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Menos gestión, más negocio.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ir al contenido principal")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/assets/app.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"portal-correlation-id\" content=\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("nonce=\"")));

        mockMvc.perform(get("/en"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Less admin, more business.")));
    }

    @Test
    void rendersNotFoundPageAndKeepsCsrfOnMutatingRequests() throws Exception {
        mockMvc.perform(get("/es/not-a-page"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/error")
                        .header("Accept-Language", "es")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Página no encontrada")));

        mockMvc.perform(post("/es/not-a-mutation"))
                .andExpect(status().isForbidden());
    }

    @Test
    void defaultDenyRendersBrandedForbiddenPage() throws Exception {
        mockMvc.perform(get("/gestion"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/error")
                        .header("Accept-Language", "es")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Acceso no permitido")));
    }

    @Test
    void localePreferencePreservesSafePathAndRejectsUnsupportedLanguage() throws Exception {
        CsrfCredentials csrf = csrfCredentials();
        mockMvc.perform(post("/preferences/locale/en")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .param("returnTo", "/es/contacto?source=nav"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/en/contacto?source=nav"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                                result.getResponse().getHeaders("Set-Cookie"))
                        .anyMatch(value -> value.startsWith("sp_lang=en; Path=/")));

        mockMvc.perform(post("/preferences/locale/en")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .param("returnTo", "https://example.invalid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/en"));

        mockMvc.perform(post("/preferences/locale/fr")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isBadRequest());
    }

    private CsrfCredentials csrfCredentials() throws Exception {
        MvcResult tokenResponse = mockMvc.perform(get("/public/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        return new CsrfCredentials(
                tokenResponse.getResponse().getCookie("SPORTAL_XSRF"),
                tokenResponse.getResponse().getContentAsString());
    }

    private record CsrfCredentials(Cookie cookie, String token) {
    }
}
